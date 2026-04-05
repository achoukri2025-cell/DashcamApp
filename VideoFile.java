package com.dashcam.app.models;

/**
 * Représente un fichier vidéo sur la carte SD de la dashcam
 */
public class VideoFile {

    private String name;
    private String url;          // URL de téléchargement
    private long   size;         // taille en octets
    private String date;         // date de création
    private String duration;     // durée de la vidéo
    private String thumbnailUrl; // aperçu
    private boolean isSelected;  // pour la sélection multiple

    public VideoFile() {}

    public VideoFile(String name, String url, long size, String date) {
        this.name = name;
        this.url  = url;
        this.size = size;
        this.date = date;
    }

    /** Taille formatée lisible par l'humain */
    public String getFormattedSize() {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}
