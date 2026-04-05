package com.dashcam.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.dashcam.app.R;
import com.dashcam.app.activities.LiveViewActivity;
import com.dashcam.app.models.DashcamConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service de premier plan pour l'enregistrement du flux vidéo
 * Enregistre le flux HTTP/RTSP dans un fichier MP4 local
 */
public class RecordingService extends Service {

    private static final String TAG             = "RecordingService";
    private static final String CHANNEL_ID      = "dashcam_recording";
    private static final int    NOTIFICATION_ID = 101;

    public static final String ACTION_START  = "action_start_recording";
    public static final String ACTION_STOP   = "action_stop_recording";
    public static final String EXTRA_CONFIG  = "extra_dashcam_config";
    public static final String EXTRA_STREAM  = "extra_stream_url";

    // Broadcast pour mettre à jour l'UI
    public static final String BROADCAST_STATUS = "com.dashcam.app.RECORDING_STATUS";
    public static final String EXTRA_IS_RECORDING = "is_recording";
    public static final String EXTRA_DURATION     = "duration_seconds";
    public static final String EXTRA_FILE_SIZE    = "file_size_bytes";
    public static final String EXTRA_FILE_PATH    = "file_path";

    private final IBinder binder = new LocalBinder();
    private ExecutorService executor;
    private volatile boolean isRecording = false;
    private String currentFilePath;
    private long recordingStartTime;
    private long currentFileSize;

    public class LocalBinder extends Binder {
        public RecordingService getService() { return RecordingService.this; }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return binder; }

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            String streamUrl = intent.getStringExtra(EXTRA_STREAM);
            startRecording(streamUrl);
        } else if (ACTION_STOP.equals(action)) {
            stopRecording();
        }
        return START_NOT_STICKY;
    }

    // ─── Démarrage de l'enregistrement ───────────────────────────────────────

    private void startRecording(String streamUrl) {
        if (isRecording) return;
        isRecording = true;
        recordingStartTime = SystemClock.elapsedRealtime();
        currentFilePath = buildOutputFilePath();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, buildNotification("Enregistrement en cours..."),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIFICATION_ID, buildNotification("Enregistrement en cours..."));
        }

        executor.execute(() -> {
            try {
                recordStream(streamUrl);
            } catch (Exception e) {
                Log.e(TAG, "Erreur d'enregistrement", e);
            } finally {
                stopSelf();
            }
        });

        broadcastStatus();
    }

    /**
     * Enregistre le flux HTTP (MJPEG ou direct) dans un fichier
     * Pour RTSP, ExoPlayer dans LiveViewActivity gère la capture via MediaMuxer
     */
    private void recordStream(String streamUrl) {
        HttpURLConnection connection = null;
        FileOutputStream fos = null;

        try {
            URL url = new URL(streamUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(0); // pas de timeout en lecture
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.e(TAG, "Réponse HTTP: " + connection.getResponseCode());
                return;
            }

            File outputFile = new File(currentFilePath);
            fos = new FileOutputStream(outputFile);

            InputStream in  = connection.getInputStream();
            byte[]      buf = new byte[65536];
            int read;

            while (isRecording && (read = in.read(buf)) != -1) {
                fos.write(buf, 0, read);
                currentFileSize += read;

                // Mise à jour notification toutes les 5 secondes
                long elapsed = (SystemClock.elapsedRealtime() - recordingStartTime) / 1000;
                if (elapsed % 5 == 0) {
                    updateNotification(formatDuration(elapsed), formatSize(currentFileSize));
                    broadcastStatus();
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Erreur IO: " + e.getMessage());
        } finally {
            if (fos != null) try { fos.close(); } catch (IOException ignored) {}
            if (connection != null) connection.disconnect();
            isRecording = false;
            broadcastStatus();
        }
    }

    // ─── Arrêt ───────────────────────────────────────────────────────────────

    public void stopRecording() {
        isRecording = false;
    }

    @Override
    public void onDestroy() {
        isRecording = false;
        if (executor != null) executor.shutdownNow();
        super.onDestroy();
    }

    // ─── Notification ─────────────────────────────────────────────────────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Enregistrement Dashcam",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Enregistrement en cours depuis votre dashcam");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification(String contentText) {
        Intent stopIntent = new Intent(this, RecordingService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent openIntent = new Intent(this, LiveViewActivity.class);
        PendingIntent openPending = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_record)
                .setContentTitle("Dashcam — Enregistrement actif")
                .setContentText(contentText)
                .setOngoing(true)
                .setContentIntent(openPending)
                .addAction(R.drawable.ic_stop, "Arrêter", stopPending)
                .build();
    }

    private void updateNotification(String duration, String size) {
        String text = duration + "  |  " + size;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIFICATION_ID, buildNotification(text));
    }

    // ─── Broadcast ───────────────────────────────────────────────────────────

    private void broadcastStatus() {
        Intent intent = new Intent(BROADCAST_STATUS);
        intent.putExtra(EXTRA_IS_RECORDING, isRecording);
        intent.putExtra(EXTRA_DURATION, (SystemClock.elapsedRealtime() - recordingStartTime) / 1000);
        intent.putExtra(EXTRA_FILE_SIZE, currentFileSize);
        intent.putExtra(EXTRA_FILE_PATH, currentFilePath);
        sendBroadcast(intent);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String buildOutputFilePath() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "Dashcam");
        if (!dir.exists()) dir.mkdirs();

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        return new File(dir, "DASHCAM_" + timestamp + ".mp4").getAbsolutePath();
    }

    private String formatDuration(long seconds) {
        return String.format(Locale.getDefault(), "%02d:%02d:%02d",
                seconds / 3600, (seconds % 3600) / 60, seconds % 60);
    }

    private String formatSize(long bytes) {
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024));
    }

    // ─── Getters publics ─────────────────────────────────────────────────────

    public boolean isRecording() { return isRecording; }
    public String getCurrentFilePath() { return currentFilePath; }
    public long getRecordingDuration() {
        if (!isRecording) return 0;
        return (SystemClock.elapsedRealtime() - recordingStartTime) / 1000;
    }
}
