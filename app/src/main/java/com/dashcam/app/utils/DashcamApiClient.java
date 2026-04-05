package com.dashcam.app.utils;

import android.os.Handler;
import android.os.Looper;

import com.dashcam.app.models.DashcamConfig;
import com.dashcam.app.models.VideoFile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Client API pour communiquer avec la dashcam via WiFi (HTTP)
 *
 * Supporte plusieurs types de dashcams :
 *  - Dashcams avec API REST standard (retour JSON)
 *  - Dashcams style Viofo/Thinkware (API propriétaire)
 *  - Dashcams avec interface web (scraping HTML basique)
 */
public class DashcamApiClient {

    public interface ConnectionCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface FileListCallback {
        void onSuccess(List<VideoFile> files);
        void onFailure(String error);
    }

    public interface DownloadProgressCallback {
        void onProgress(int percent, long downloadedBytes, long totalBytes);
        void onComplete(String savedPath);
        void onFailure(String error);
    }

    private final DashcamConfig config;
    private final OkHttpClient  client;
    private final Handler       mainHandler;

    public DashcamApiClient(DashcamConfig config) {
        this.config      = config;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    // ─── Test de connexion ────────────────────────────────────────────────────

    /**
     * Teste la connexion à la dashcam en essayant plusieurs endpoints connus
     */
    public void testConnection(ConnectionCallback callback) {
        String baseUrl = config.getApiBaseUrl();

        // Essai de plusieurs endpoints standard
        String[] testEndpoints = {
                baseUrl + "/",
                baseUrl + "/api/status",
                baseUrl + "/cgi-bin/hello",
                baseUrl + "/DCIM/",
                baseUrl + "/status"
        };

        tryNextEndpoint(testEndpoints, 0, callback);
    }

    private void tryNextEndpoint(String[] endpoints, int index, ConnectionCallback callback) {
        if (index >= endpoints.length) {
            mainHandler.post(() -> callback.onFailure("Impossible de joindre la caméra. Vérifiez l'IP et le port."));
            return;
        }

        Request req = new Request.Builder()
                .url(endpoints[index])
                .get()
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // Essayer le prochain endpoint
                tryNextEndpoint(endpoints, index + 1, callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close();
                mainHandler.post(callback::onSuccess);
            }
        });
    }

    // ─── Liste des fichiers SD ────────────────────────────────────────────────

    /**
     * Récupère la liste des vidéos sur la carte SD.
     * Essaie plusieurs formats d'API connus.
     */
    public void listSdCardFiles(FileListCallback callback) {
        String baseUrl = config.getApiBaseUrl();

        // Essai API JSON standard
        tryJsonApi(baseUrl + "/api/files", callback, () ->
            // Fallback : API style Viofo (?action=listfile)
            tryViofoApi(baseUrl + "/?action=listfile&type=video", callback, () ->
                // Fallback : liste DCIM HTML
                tryDcimListing(baseUrl + "/DCIM/", callback)
            )
        );
    }

    /** API JSON standard : retourne [{name, url, size, date}] */
    private void tryJsonApi(String url, FileListCallback callback, Runnable fallback) {
        Request req = new Request.Builder().url(url).get().build();
        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { fallback.run(); }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                response.close();
                try {
                    JSONArray arr = new JSONArray(body);
                    List<VideoFile> files = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        VideoFile vf = new VideoFile(
                                obj.optString("name"),
                                obj.optString("url"),
                                obj.optLong("size", 0),
                                obj.optString("date")
                        );
                        files.add(vf);
                    }
                    mainHandler.post(() -> callback.onSuccess(files));
                } catch (Exception e) {
                    fallback.run();
                }
            }
        });
    }

    /** API style Viofo/BlackVue : retourne une liste XML ou texte */
    private void tryViofoApi(String url, FileListCallback callback, Runnable fallback) {
        Request req = new Request.Builder().url(url).get().build();
        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) { fallback.run(); }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                response.close();
                try {
                    // Format Viofo : "File: /tmp/SD0/filename.mp4\n..."
                    List<VideoFile> files = new ArrayList<>();
                    String baseUrl = config.getApiBaseUrl();
                    String[] lines = body.split("\n");
                    for (String line : lines) {
                        if (line.contains(".mp4") || line.contains(".avi") || line.contains(".mov")) {
                            String path = line.replaceAll(".*File:\\s*", "").trim();
                            String name = path.substring(path.lastIndexOf('/') + 1);
                            VideoFile vf = new VideoFile(name, baseUrl + path, 0, "");
                            files.add(vf);
                        }
                    }
                    if (!files.isEmpty()) {
                        mainHandler.post(() -> callback.onSuccess(files));
                    } else {
                        fallback.run();
                    }
                } catch (Exception e) {
                    fallback.run();
                }
            }
        });
    }

    /** Listing DCIM basique (serveur HTTP simple) */
    private void tryDcimListing(String url, FileListCallback callback) {
        Request req = new Request.Builder().url(url).get().build();
        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure("Impossible de lister les fichiers de la carte SD.\n" +
                        "Vérifiez le chemin d'accès dans les paramètres."));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                response.close();
                // Parse HTML basique : cherche les liens .mp4/.avi
                List<VideoFile> files = new ArrayList<>();
                String[] parts = body.split("href=\"");
                for (int i = 1; i < parts.length; i++) {
                    String href = parts[i].split("\"")[0];
                    if (href.toLowerCase().endsWith(".mp4")
                            || href.toLowerCase().endsWith(".avi")
                            || href.toLowerCase().endsWith(".mov")) {
                        String name = href.substring(href.lastIndexOf('/') + 1);
                        String fileUrl = href.startsWith("http") ? href : url + href;
                        files.add(new VideoFile(name, fileUrl, 0, ""));
                    }
                }
                mainHandler.post(() -> callback.onSuccess(files));
            }
        });
    }

    // ─── Téléchargement ───────────────────────────────────────────────────────

    /**
     * Télécharge un fichier depuis la caméra vers le stockage local
     */
    public void downloadFile(String url, String destinationPath, DownloadProgressCallback callback) {
        Request req = new Request.Builder().url(url).get().build();
        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) {
                    mainHandler.post(() -> callback.onFailure("Erreur HTTP: " + response.code()));
                    return;
                }

                long totalBytes = response.body().contentLength();
                byte[] buffer   = new byte[8192];
                long  downloaded = 0;

                try (java.io.InputStream in = response.body().byteStream();
                     java.io.FileOutputStream out = new java.io.FileOutputStream(destinationPath)) {

                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        downloaded += read;
                        final long dl  = downloaded;
                        final int  pct = totalBytes > 0 ? (int) (dl * 100 / totalBytes) : -1;
                        mainHandler.post(() -> callback.onProgress(pct, dl, totalBytes));
                    }
                    final String savedPath = destinationPath;
                    mainHandler.post(() -> callback.onComplete(savedPath));

                } catch (IOException e) {
                    mainHandler.post(() -> callback.onFailure("Erreur d'écriture: " + e.getMessage()));
                }
            }
        });
    }

    // ─── Commandes caméra ─────────────────────────────────────────────────────

    /** Envoie une commande à la caméra (prise de photo, start/stop enregistrement SD) */
    public void sendCommand(String command, ConnectionCallback callback) {
        String url = config.getApiBaseUrl() + "/cgi-bin/cmd?action=" + command;
        Request req = new Request.Builder().url(url).get().build();
        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close();
                mainHandler.post(callback::onSuccess);
            }
        });
    }
}
