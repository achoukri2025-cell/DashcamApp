package com.dashcam.app.utils;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dashcam.app.R;

/**
 * Dialogue de progression pour les téléchargements depuis la carte SD.
 * Affiche une barre de progression et le pourcentage.
 */
public class DownloadProgressDialog {

    private final Dialog     dialog;
    private final ProgressBar progressBar;
    private final TextView   tvPercent;
    private final TextView   tvFileName;
    private final TextView   tvSize;
    private final Handler    mainHandler;

    public DownloadProgressDialog(Context context, String fileName) {
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_download_progress);
        dialog.setCancelable(false);

        progressBar = dialog.findViewById(R.id.progress_bar);
        tvPercent   = dialog.findViewById(R.id.tv_percent);
        tvFileName  = dialog.findViewById(R.id.tv_filename);
        tvSize      = dialog.findViewById(R.id.tv_size);
        mainHandler = new Handler(Looper.getMainLooper());

        if (tvFileName != null) tvFileName.setText(fileName);
        if (progressBar != null) progressBar.setMax(100);
    }

    public void show() {
        mainHandler.post(() -> { if (!dialog.isShowing()) dialog.show(); });
    }

    public void updateProgress(int percent, long downloadedBytes, long totalBytes) {
        mainHandler.post(() -> {
            if (progressBar != null) progressBar.setProgress(Math.max(0, Math.min(100, percent)));
            if (tvPercent   != null) tvPercent.setText(percent >= 0 ? percent + "%" : "...");
            if (tvSize      != null && totalBytes > 0) {
                tvSize.setText(formatSize(downloadedBytes) + " / " + formatSize(totalBytes));
            }
        });
    }

    public void dismiss() {
        mainHandler.post(() -> { if (dialog.isShowing()) dialog.dismiss(); });
    }

    private String formatSize(long bytes) {
        if (bytes < 1024 * 1024) return String.format("%.0f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
