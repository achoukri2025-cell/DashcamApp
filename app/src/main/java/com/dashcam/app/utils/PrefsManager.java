package com.dashcam.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.dashcam.app.models.DashcamConfig;
import com.google.gson.Gson;

/**
 * Gestionnaire des préférences de l'application
 */
public class PrefsManager {

    private static final String PREFS_NAME   = "dashcam_prefs";
    private static final String KEY_CONFIG   = "dashcam_config";
    private static final String KEY_FIRST_RUN = "first_run";

    private final SharedPreferences prefs;
    private final Gson gson;

    public PrefsManager(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson  = new Gson();
    }

    /** Sauvegarde la configuration de la caméra */
    public void saveConfig(DashcamConfig config) {
        prefs.edit().putString(KEY_CONFIG, gson.toJson(config)).apply();
    }

    /** Charge la configuration sauvegardée ou retourne une config par défaut */
    public DashcamConfig loadConfig() {
        String json = prefs.getString(KEY_CONFIG, null);
        if (json == null) return new DashcamConfig();
        try {
            return gson.fromJson(json, DashcamConfig.class);
        } catch (Exception e) {
            return new DashcamConfig();
        }
    }

    public boolean isFirstRun() {
        return prefs.getBoolean(KEY_FIRST_RUN, true);
    }

    public void setFirstRunDone() {
        prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply();
    }
}
