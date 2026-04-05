package com.dashcam.app.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * Utilitaire WiFi : vérifie que le téléphone est bien connecté
 * au réseau WiFi de la dashcam avant de tenter la connexion.
 */
public class WifiHelper {

    private final Context context;

    public WifiHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Retourne true si le WiFi est activé et connecté */
    public boolean isWifiConnected() {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wm == null || !wm.isWifiEnabled()) return false;
        WifiInfo info = wm.getConnectionInfo();
        return info != null && info.getNetworkId() != -1;
    }

    /** Retourne le SSID du réseau WiFi actuel (sans guillemets) */
    public String getCurrentSsid() {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wm == null) return "";
        WifiInfo info = wm.getConnectionInfo();
        if (info == null) return "";
        String ssid = info.getSSID();
        if (ssid == null) return "";
        return ssid.replace("\"", "");
    }

    /** Retourne l'adresse IP locale du téléphone sur le réseau actuel */
    public String getLocalIpAddress() {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wm == null) return "";
        int ip = wm.getConnectionInfo().getIpAddress();
        return String.format("%d.%d.%d.%d",
                (ip & 0xff),
                (ip >> 8 & 0xff),
                (ip >> 16 & 0xff),
                (ip >> 24 & 0xff));
    }

    /**
     * Essaie de deviner l'IP passerelle (souvent l'IP de la dashcam)
     * en remplaçant le dernier octet de l'IP locale par .1
     */
    public String guessGatewayIp() {
        String localIp = getLocalIpAddress();
        if (localIp.isEmpty()) return "192.168.1.1";
        String[] parts = localIp.split("\\.");
        if (parts.length != 4) return "192.168.1.1";
        return parts[0] + "." + parts[1] + "." + parts[2] + ".1";
    }

    /** Vérifie si le réseau actuel ressemble à celui d'une dashcam */
    public boolean looksLikeDashcamNetwork() {
        String ssid = getCurrentSsid().toLowerCase();
        return ssid.contains("dashcam")
                || ssid.contains("dash")
                || ssid.contains("cam")
                || ssid.contains("viofo")
                || ssid.contains("blackvue")
                || ssid.contains("thinkware")
                || ssid.contains("70mai")
                || ssid.contains("nextbase")
                || ssid.contains("garmin")
                || ssid.contains("ddpai");
    }

    /** Retourne un message d'aide contextuel selon la situation WiFi */
    public String getConnectionHint() {
        if (!isWifiConnected()) {
            return "⚠ Aucun WiFi détecté. Connectez-vous au réseau WiFi de votre dashcam depuis les paramètres Android.";
        }
        String ssid = getCurrentSsid();
        if (looksLikeDashcamNetwork()) {
            return "✓ Réseau dashcam détecté : " + ssid + "\n  IP suggérée : " + guessGatewayIp();
        }
        return "WiFi : " + ssid + "\n  (Assurez-vous d'être connecté au WiFi de la dashcam)";
    }
}
