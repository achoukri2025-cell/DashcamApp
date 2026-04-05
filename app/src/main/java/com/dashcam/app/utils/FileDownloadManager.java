package com.dashcam.app.utils;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire de téléchargements depuis la carte SD de la dashcam.
 * Utilise le DownloadManager Android pour une intégration système complète
 * (notification, reprise, scan MediaStore).
 */
public class FileDownloadManager {

    public interface DownloadCallback {
        void onProgress(long downloadId, int percent);
        void onComplete(long downloadId, String filePath);
        void onFailed(long downloadId, String reason);
    }

    private final Context         context;
    private final DownloadManager downloadManager;
    private final Handler         mainHandler;
    private final Map<Long, DownloadCallback> activeDownloads = new HashMap<>();
    private final Map<Long, Runnable>         progressCheckers = new HashMap<>();

    // Receiver pour détecter la fin des téléchargements
    private final BroadcastReceiver completionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == -1 || !activeDownloads.containsKey(id)) return;

            DownloadCallback cb = activeDownloads.get(id);
            stopProgressChecker(id);

            DownloadManager.Query q = new DownloadManager.Query().setFilterById(id);
            Cursor c = downloadManager.query(q);
            if (c != null && c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    String uriStr = c.getString(c.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI));
                    if (cb != null) mainHandler.post(() -> cb.onComplete(id, uriStr));
                } else {
                    int reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON));
                    if (cb != null) mainHandler.post(() -> cb.onFailed(id, "Code erreur: " + reason));
                }
                c.close();
            }
            activeDownloads.remove(id);
        }
    };

    public FileDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        this.downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());

        // Enregistrer le receiver
        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        this.context.registerReceiver(completionReceiver, filter);
    }

    /**
     * Lance le téléchargement d'un fichier depuis la dashcam
     *
     * @param url           URL du fichier sur la dashcam
     * @param fileName      Nom du fichier de destination
     * @param subFolder     Sous-dossier dans Movies/ (ex: "Dashcam")
     * @param callback      Callback progression/fin
     * @return              ID du téléchargement (pour annulation éventuelle)
     */
    public long download(String url, String fileName, String subFolder, DownloadCallback callback) {
        if (downloadManager == null) {
            if (callback != null) callback.onFailed(-1, "DownloadManager non disponible");
            return -1;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(fileName);
        request.setDescription("Dashcam → " + fileName);
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_MOVIES, subFolder + "/" + fileName);
        request.allowScanningByMediaScanner();
        request.setMimeType("video/mp4");

        long downloadId = downloadManager.enqueue(request);
        if (callback != null) {
            activeDownloads.put(downloadId, callback);
            startProgressChecker(downloadId, callback);
        }
        return downloadId;
    }

    /** Annule un téléchargement en cours */
    public void cancel(long downloadId) {
        stopProgressChecker(downloadId);
        activeDownloads.remove(downloadId);
        if (downloadManager != null) downloadManager.remove(downloadId);
    }

    /** Vérifie la progression toutes les secondes */
    private void startProgressChecker(long downloadId, DownloadCallback callback) {
        Runnable checker = new Runnable() {
            @Override
            public void run() {
                if (!activeDownloads.containsKey(downloadId)) return;

                DownloadManager.Query q = new DownloadManager.Query().setFilterById(downloadId);
                Cursor c = downloadManager.query(q);
                if (c != null && c.moveToFirst()) {
                    long total = c.getLong(c.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    long downloaded = c.getLong(c.getColumnIndexOrThrow(
                            DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    c.close();

                    if (total > 0) {
                        int percent = (int) (downloaded * 100 / total);
                        mainHandler.post(() -> callback.onProgress(downloadId, percent));
                    }
                }
                mainHandler.postDelayed(this, 1000);
            }
        };
        progressCheckers.put(downloadId, checker);
        mainHandler.postDelayed(checker, 500);
    }

    private void stopProgressChecker(long downloadId) {
        Runnable checker = progressCheckers.remove(downloadId);
        if (checker != null) mainHandler.removeCallbacks(checker);
    }

    /** À appeler dans onDestroy de l'activité/service qui utilise ce manager */
    public void release() {
        try {
            context.unregisterReceiver(completionReceiver);
        } catch (Exception ignored) {}
        for (Runnable r : progressCheckers.values()) mainHandler.removeCallbacks(r);
        progressCheckers.clear();
        activeDownloads.clear();
    }
}
