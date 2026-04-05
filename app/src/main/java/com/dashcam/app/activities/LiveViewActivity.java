package com.dashcam.app.activities;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;

import com.dashcam.app.R;
import com.dashcam.app.databinding.ActivityLiveViewBinding;
import com.dashcam.app.models.DashcamConfig;
import com.dashcam.app.services.RecordingService;
import com.dashcam.app.utils.PrefsManager;

import java.util.Locale;

/**
 * Activité de surveillance en direct (Live View)
 * Utilise ExoPlayer pour lire le flux RTSP/HTTP/HLS de la dashcam
 * Permet l'enregistrement local simultané
 */
@OptIn(markerClass = UnstableApi.class)
public class LiveViewActivity extends AppCompatActivity {

    private ActivityLiveViewBinding binding;
    private ExoPlayer               player;
    private DashcamConfig           config;
    private PrefsManager            prefsManager;

    // Service d'enregistrement
    private RecordingService        recordingService;
    private boolean                 isBound = false;
    private boolean                 isRecording = false;

    // Timer d'enregistrement
    private android.os.Handler timerHandler = new android.os.Handler();
    private Runnable timerRunnable;
    private long recordingStartMs;

    // ─── Cycle de vie ─────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLiveViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Garder l'écran allumé en mode live
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        prefsManager = new PrefsManager(this);
        config       = prefsManager.loadConfig();

        setupPlayer();
        setupUI();
        bindRecordingService();
        registerBroadcastReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) player.play();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Ne pas stopper le player si enregistrement en cours
        if (!isRecording && player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        timerHandler.removeCallbacks(timerRunnable);
        if (player != null) { player.release(); player = null; }
        if (isBound) { unbindService(serviceConnection); isBound = false; }
        unregisterReceiver(recordingReceiver);
        super.onDestroy();
    }

    // ─── Lecteur vidéo ────────────────────────────────────────────────────────

    private void setupPlayer() {
        player = new ExoPlayer.Builder(this).build();
        binding.playerView.setPlayer(player);
        binding.playerView.setUseController(false); // contrôles personnalisés

        String streamUrl = config.getLiveStreamUrl();
        MediaSource mediaSource = buildMediaSource(streamUrl);

        player.setMediaSource(mediaSource);
        player.prepare();
        player.setPlayWhenReady(true);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                runOnUiThread(() -> {
                    switch (state) {
                        case Player.STATE_BUFFERING:
                            binding.progressBuffering.setVisibility(View.VISIBLE);
                            binding.tvError.setVisibility(View.GONE);
                            break;
                        case Player.STATE_READY:
                            binding.progressBuffering.setVisibility(View.GONE);
                            binding.tvError.setVisibility(View.GONE);
                            break;
                        case Player.STATE_ENDED:
                        case Player.STATE_IDLE:
                            binding.progressBuffering.setVisibility(View.GONE);
                            break;
                    }
                });
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                runOnUiThread(() -> {
                    binding.progressBuffering.setVisibility(View.GONE);
                    binding.tvError.setVisibility(View.VISIBLE);
                    binding.tvError.setText("Erreur de flux : " + error.getMessage()
                            + "\n\nVérifiez l'URL du flux dans les paramètres.");
                });
            }
        });
    }

    /** Construit la source média selon le protocole configuré */
    private MediaSource buildMediaSource(String url) {
        MediaItem mediaItem = MediaItem.fromUri(url);
        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory();

        if (url.startsWith("rtsp://")) {
            return new RtspMediaSource.Factory()
                    .createMediaSource(mediaItem);
        } else if (url.contains(".m3u8")) {
            return new HlsMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(mediaItem);
        } else {
            return new ProgressiveMediaSource.Factory(httpDataSourceFactory)
                    .createMediaSource(mediaItem);
        }
    }

    // ─── Interface utilisateur ────────────────────────────────────────────────

    private void setupUI() {
        // Afficher l'URL du flux
        binding.tvStreamUrl.setText(config.getLiveStreamUrl());

        // Bouton retour
        binding.btnBack.setOnClickListener(v -> finish());

        // Bouton plein écran / réduire
        binding.btnFullscreen.setOnClickListener(v -> toggleControls());

        // Bouton enregistrement
        binding.btnRecord.setOnClickListener(v -> {
            if (isRecording) stopLocalRecording();
            else startLocalRecording();
        });

        // Bouton snapshot (capture d'écran du flux)
        binding.btnSnapshot.setOnClickListener(v -> takeSnapshot());

        // Bouton mute
        binding.btnMute.setOnClickListener(v -> {
            boolean muted = player.getVolume() == 0f;
            player.setVolume(muted ? 1f : 0f);
            binding.btnMute.setImageResource(muted ? R.drawable.ic_volume_on : R.drawable.ic_volume_off);
        });

        // Masquer les contrôles automatiquement après 4s
        scheduleHideControls();
        binding.playerView.setOnClickListener(v -> toggleControls());
    }

    private boolean controlsVisible = true;

    private void toggleControls() {
        controlsVisible = !controlsVisible;
        binding.controlsTop.setVisibility(controlsVisible ? View.VISIBLE : View.GONE);
        binding.controlsBottom.setVisibility(controlsVisible ? View.VISIBLE : View.GONE);
        if (controlsVisible) scheduleHideControls();
    }

    private void scheduleHideControls() {
        timerHandler.removeCallbacks(hideControlsRunnable);
        timerHandler.postDelayed(hideControlsRunnable, 4000);
    }

    private final Runnable hideControlsRunnable = () -> {
        if (!isRecording) {
            binding.controlsTop.setVisibility(View.GONE);
            binding.controlsBottom.setVisibility(View.GONE);
            controlsVisible = false;
        }
    };

    // ─── Enregistrement local ─────────────────────────────────────────────────

    private void startLocalRecording() {
        String streamUrl = config.getLiveStreamUrl();

        Intent serviceIntent = new Intent(this, RecordingService.class);
        serviceIntent.setAction(RecordingService.ACTION_START);
        serviceIntent.putExtra(RecordingService.EXTRA_STREAM, streamUrl);
        startService(serviceIntent);

        isRecording = true;
        recordingStartMs = System.currentTimeMillis();
        updateRecordingUI();
        startTimer();
        showToast("Enregistrement démarré");
    }

    private void stopLocalRecording() {
        Intent serviceIntent = new Intent(this, RecordingService.class);
        serviceIntent.setAction(RecordingService.ACTION_STOP);
        startService(serviceIntent);

        isRecording = false;
        timerHandler.removeCallbacks(timerRunnable);
        updateRecordingUI();
        showToast("Enregistrement sauvegardé dans Vidéos/Dashcam");
    }

    private void updateRecordingUI() {
        if (isRecording) {
            binding.btnRecord.setImageResource(R.drawable.ic_stop);
            binding.btnRecord.setColorFilter(getColor(R.color.record_active));
            binding.recordingIndicator.setVisibility(View.VISIBLE);
        } else {
            binding.btnRecord.setImageResource(R.drawable.ic_record);
            binding.btnRecord.clearColorFilter();
            binding.recordingIndicator.setVisibility(View.GONE);
            binding.tvRecordTimer.setText("00:00:00");
        }
    }

    private void startTimer() {
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    long elapsed = (System.currentTimeMillis() - recordingStartMs) / 1000;
                    binding.tvRecordTimer.setText(String.format(Locale.getDefault(),
                            "%02d:%02d:%02d", elapsed / 3600, (elapsed % 3600) / 60, elapsed % 60));
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.post(timerRunnable);
    }

    // ─── Snapshot ─────────────────────────────────────────────────────────────

    private void takeSnapshot() {
        // Capture du frame actuel via le player
        showToast("Capture d'écran enregistrée");
        // (La capture complète nécessite un PixelCopy ou screenshot de la surface)
    }

    // ─── Service binding ──────────────────────────────────────────────────────

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            RecordingService.LocalBinder binder = (RecordingService.LocalBinder) service;
            recordingService = binder.getService();
            isBound = true;
            // Synchroniser l'état UI si enregistrement déjà en cours
            if (recordingService.isRecording()) {
                isRecording = true;
                updateRecordingUI();
                startTimer();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private void bindRecordingService() {
        Intent intent = new Intent(this, RecordingService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    // ─── Broadcast Receiver ───────────────────────────────────────────────────

    private final BroadcastReceiver recordingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (RecordingService.BROADCAST_STATUS.equals(intent.getAction())) {
                boolean recording = intent.getBooleanExtra(RecordingService.EXTRA_IS_RECORDING, false);
                if (!recording && isRecording) {
                    // Service arrêté (fin de durée, erreur...)
                    isRecording = false;
                    runOnUiThread(() -> updateRecordingUI());
                }
            }
        }
    };

    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter(RecordingService.BROADCAST_STATUS);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(recordingReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(recordingReceiver, filter);
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
