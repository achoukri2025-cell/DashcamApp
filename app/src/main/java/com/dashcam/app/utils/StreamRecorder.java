package com.dashcam.app.utils;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Enregistreur de flux HTTP vers fichier MP4 local.
 *
 * Pour les flux RTSP, l'enregistrement passe par ExoPlayer + MediaMuxer
 * directement dans LiveViewActivity (non implémenté ici — nécessite
 * une MediaProjection ou un accès à la surface de décodage).
 *
 * Ce recorder gère les flux HTTP MJPEG / progressive MP4 / HLS segments.
 */
public class StreamRecorder {

    private static final String TAG     = "StreamRecorder";
    private static final int    BUF_SIZE = 65536; // 64 KB

    public interface RecorderCallback {
        void onStarted(String filePath);
        void onProgress(long elapsedSeconds, long bytes);
        void onStopped(String filePath, long totalBytes);
        void onError(String message);
    }

    private volatile boolean running = false;
    private Thread            recordThread;
    private String            outputPath;
    private final RecorderCallback callback;

    public StreamRecorder(RecorderCallback callback) {
        this.callback = callback;
    }

    /**
     * Démarre l'enregistrement en arrière-plan
     * @param streamUrl URL HTTP du flux (HTTP MJPEG, HLS segment, etc.)
     */
    public void start(String streamUrl) {
        if (running) return;
        running     = true;
        outputPath  = buildOutputPath();

        recordThread = new Thread(() -> doRecord(streamUrl), "StreamRecorderThread");
        recordThread.start();
    }

    /** Arrête proprement l'enregistrement */
    public void stop() {
        running = false;
        if (recordThread != null) {
            recordThread.interrupt();
            recordThread = null;
        }
    }

    public boolean isRunning() { return running; }
    public String  getOutputPath() { return outputPath; }

    // ─────────────────────────────────────────────────────────────────────────

    private void doRecord(String streamUrl) {
        HttpURLConnection conn = null;
        java.io.FileOutputStream fos = null;
        long totalBytes   = 0;
        long startTime    = System.currentTimeMillis();

        try {
            URL url = new URL(streamUrl);
            conn    = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(0);
            conn.setRequestProperty("User-Agent", "DashcamApp/1.0");
            conn.connect();

            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                notifyError("Erreur HTTP " + code + " lors de la connexion au flux");
                return;
            }

            File outFile = new File(outputPath);
            outFile.getParentFile().mkdirs();
            fos = new java.io.FileOutputStream(outFile);

            // Notification démarrage
            final String path = outputPath;
            if (callback != null) callback.onStarted(path);

            InputStream in  = conn.getInputStream();
            byte[]      buf = new byte[BUF_SIZE];
            int         read;
            long        lastProgressTime = startTime;

            while (running && !Thread.currentThread().isInterrupted()
                   && (read = in.read(buf)) != -1) {

                fos.write(buf, 0, read);
                totalBytes += read;

                // Notification progression toutes les secondes
                long now = System.currentTimeMillis();
                if (now - lastProgressTime >= 1000) {
                    lastProgressTime = now;
                    long elapsed = (now - startTime) / 1000;
                    final long el  = elapsed;
                    final long tb  = totalBytes;
                    if (callback != null) callback.onProgress(el, tb);
                }
            }

        } catch (IOException e) {
            if (running) {
                // Erreur inattendue (pas un arrêt volontaire)
                Log.e(TAG, "Erreur IO: " + e.getMessage());
                notifyError("Erreur réseau : " + e.getMessage());
            }
        } finally {
            running = false;
            if (fos != null) try { fos.flush(); fos.close(); } catch (IOException ignored) {}
            if (conn  != null) conn.disconnect();

            final long tb = totalBytes;
            final String fp = outputPath;
            if (callback != null) callback.onStopped(fp, tb);
            Log.d(TAG, "Enregistrement terminé : " + tb + " octets → " + fp);
        }
    }

    private void notifyError(String msg) {
        running = false;
        if (callback != null) callback.onError(msg);
    }

    /** Construit le chemin de sortie dans Movies/Dashcam/ */
    private String buildOutputPath() {
        File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "Dashcam"
        );
        dir.mkdirs();
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return new File(dir, "DASHCAM_" + ts + ".mp4").getAbsolutePath();
    }
}
