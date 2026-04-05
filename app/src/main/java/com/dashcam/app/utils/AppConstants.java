package com.dashcam.app.utils;

/**
 * Constantes globales de l'application DashCam View
 */
public final class AppConstants {

    private AppConstants() {}

    // ─── Dossiers de sortie ───────────────────────────────────────────────────
    public static final String OUTPUT_FOLDER      = "Dashcam";
    public static final String RECORDING_PREFIX   = "DASHCAM_";
    public static final String RECORDING_EXT      = ".mp4";

    // ─── Réseau ───────────────────────────────────────────────────────────────
    public static final int    CONNECT_TIMEOUT_MS  = 10_000;
    public static final int    READ_TIMEOUT_MS      = 30_000;
    public static final int    STREAM_TIMEOUT_MS    = 0;       // illimité pour le flux live

    // ─── IPs par défaut selon la marque ───────────────────────────────────────
    public static final String IP_GENERIC    = "192.168.1.1";
    public static final String IP_VIOFO      = "192.168.1.254";
    public static final String IP_BLACKVUE   = "10.99.77.1";
    public static final String IP_70MAI      = "192.168.0.1";
    public static final String IP_NEXTBASE   = "192.168.0.1";
    public static final String IP_THINKWARE  = "192.168.0.1";
    public static final String IP_DDPAI      = "192.168.0.1";

    // ─── Ports ────────────────────────────────────────────────────────────────
    public static final int PORT_RTSP_DEFAULT  = 554;
    public static final int PORT_HTTP_DEFAULT  = 80;
    public static final int PORT_BLACKVUE      = 7070;
    public static final int PORT_DDPAI         = 8080;

    // ─── Chemins de flux ──────────────────────────────────────────────────────
    public static final String STREAM_GENERIC   = "/live/channel0";
    public static final String STREAM_VIOFO     = "/live";
    public static final String STREAM_BLACKVUE  = "/blackvue_live.m3u8";
    public static final String STREAM_70MAI     = "/live/ch00_0";
    public static final String STREAM_THINKWARE = "/stream1";

    // ─── API carte SD ─────────────────────────────────────────────────────────
    public static final String SD_PATH_GENERIC  = "/DCIM/";
    public static final String SD_PATH_VIOFO    = "/?action=listfile&type=video";
    public static final String SD_PATH_BLACKVUE = "/blackvue/record/";
    public static final String SD_PATH_REST     = "/api/files";

    // ─── Notification ─────────────────────────────────────────────────────────
    public static final String NOTIF_CHANNEL_ID   = "dashcam_recording";
    public static final int    NOTIF_RECORDING_ID  = 101;

    // ─── Buffer d'enregistrement ──────────────────────────────────────────────
    public static final int    STREAM_BUFFER_SIZE  = 65_536;  // 64 KB
    public static final long   PROGRESS_INTERVAL_MS = 1_000;  // 1 sec

    // ─── Préférences ──────────────────────────────────────────────────────────
    public static final String PREFS_NAME          = "dashcam_prefs";
    public static final String PREF_KEY_CONFIG     = "dashcam_config";
    public static final String PREF_KEY_FIRST_RUN  = "first_run";
}
