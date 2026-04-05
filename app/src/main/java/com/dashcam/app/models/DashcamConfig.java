package com.dashcam.app.models;

/**
 * Modèle de configuration de la dashcam
 * Supporte les protocoles RTSP, HTTP MJPEG et HLS
 */
public class DashcamConfig {

    public static final String PROTOCOL_RTSP   = "RTSP";
    public static final String PROTOCOL_HTTP   = "HTTP_MJPEG";
    public static final String PROTOCOL_HLS    = "HLS";

    // Paramètres de connexion
    private String cameraIp;
    private int    cameraPort;
    private String protocol;
    private String username;
    private String password;
    private String streamPath;

    // Paramètres d'enregistrement local
    private String recordingQuality; // HIGH, MEDIUM, LOW
    private int    recordingDuration; // minutes, 0 = illimité
    private boolean autoRecord;

    // Chemins caméra
    private String sdCardPath;      // chemin API carte SD
    private String liveStreamUrl;   // URL flux live

    public DashcamConfig() {
        // Valeurs par défaut pour dashcams courantes
        this.cameraIp = "192.168.1.1";
        this.cameraPort = 554;
        this.protocol = PROTOCOL_RTSP;
        this.streamPath = "/live/channel0";
        this.username = "";
        this.password = "";
        this.recordingQuality = "HIGH";
        this.recordingDuration = 0;
        this.autoRecord = false;
        this.sdCardPath = "/files";
    }

    /** Construit l'URL du flux live selon le protocole */
    public String buildStreamUrl() {
        switch (protocol) {
            case PROTOCOL_RTSP:
                if (username.isEmpty()) {
                    return "rtsp://" + cameraIp + ":" + cameraPort + streamPath;
                }
                return "rtsp://" + username + ":" + password + "@" + cameraIp + ":" + cameraPort + streamPath;

            case PROTOCOL_HTTP:
                return "http://" + cameraIp + ":" + cameraPort + streamPath;

            case PROTOCOL_HLS:
                return "http://" + cameraIp + ":" + cameraPort + streamPath + ".m3u8";

            default:
                return "rtsp://" + cameraIp + ":" + cameraPort + streamPath;
        }
    }

    /** URL de base pour l'API HTTP de la caméra */
    public String getApiBaseUrl() {
        return "http://" + cameraIp + ":" + (protocol.equals(PROTOCOL_RTSP) ? 80 : cameraPort);
    }

    /** URL d'accès à la carte SD */
    public String getSdCardUrl() {
        return getApiBaseUrl() + sdCardPath;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public String getCameraIp() { return cameraIp; }
    public void setCameraIp(String cameraIp) { this.cameraIp = cameraIp; }

    public int getCameraPort() { return cameraPort; }
    public void setCameraPort(int cameraPort) { this.cameraPort = cameraPort; }

    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getStreamPath() { return streamPath; }
    public void setStreamPath(String streamPath) { this.streamPath = streamPath; }

    public String getRecordingQuality() { return recordingQuality; }
    public void setRecordingQuality(String recordingQuality) { this.recordingQuality = recordingQuality; }

    public int getRecordingDuration() { return recordingDuration; }
    public void setRecordingDuration(int recordingDuration) { this.recordingDuration = recordingDuration; }

    public boolean isAutoRecord() { return autoRecord; }
    public void setAutoRecord(boolean autoRecord) { this.autoRecord = autoRecord; }

    public String getSdCardPath() { return sdCardPath; }
    public void setSdCardPath(String sdCardPath) { this.sdCardPath = sdCardPath; }

    public String getLiveStreamUrl() {
        return (liveStreamUrl != null && !liveStreamUrl.isEmpty()) ? liveStreamUrl : buildStreamUrl();
    }
    public void setLiveStreamUrl(String liveStreamUrl) { this.liveStreamUrl = liveStreamUrl; }
}
