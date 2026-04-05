package com.dashcam.app.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

/**
 * Service de connexion dashcam (placeholder pour extensions futures).
 * Peut servir à maintenir une connexion persistante en arrière-plan,
 * surveiller la qualité du flux, ou gérer la reconnexion automatique.
 */
public class DashcamConnectionService extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Implémentation future : reconnexion auto, monitoring qualité signal
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
