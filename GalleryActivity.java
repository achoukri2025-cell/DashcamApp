package com.dashcam.app.activities;

import android.app.DownloadManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dashcam.app.R;
import com.dashcam.app.databinding.ActivityGalleryBinding;
import com.dashcam.app.models.DashcamConfig;
import com.dashcam.app.models.VideoFile;
import com.dashcam.app.utils.DashcamApiClient;
import com.dashcam.app.utils.PrefsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Galerie des vidéos sur la carte SD de la dashcam
 * Permet de visualiser, télécharger et supprimer des enregistrements
 */
public class GalleryActivity extends AppCompatActivity {

    private ActivityGalleryBinding binding;
    private DashcamApiClient       apiClient;
    private DashcamConfig          config;
    private VideoAdapter           adapter;
    private List<VideoFile>        videoFiles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGalleryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        config    = new PrefsManager(this).loadConfig();
        apiClient = new DashcamApiClient(config);

        setupUI();
        loadVideoList();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        // RecyclerView en grille 2 colonnes
        adapter = new VideoAdapter();
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerView.setAdapter(adapter);

        // Refresh manuel
        binding.swipeRefresh.setOnRefreshListener(this::loadVideoList);

        binding.tvTitle.setText("Carte SD — " + config.getCameraIp());
    }

    // ─── Chargement de la liste ───────────────────────────────────────────────

    private void loadVideoList() {
        binding.swipeRefresh.setRefreshing(true);
        binding.tvEmpty.setVisibility(View.GONE);

        apiClient.listSdCardFiles(new DashcamApiClient.FileListCallback() {
            @Override
            public void onSuccess(List<VideoFile> files) {
                binding.swipeRefresh.setRefreshing(false);
                videoFiles.clear();
                videoFiles.addAll(files);
                adapter.notifyDataSetChanged();

                if (files.isEmpty()) {
                    binding.tvEmpty.setText("Aucune vidéo trouvée sur la carte SD");
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    binding.tvCount.setText(files.size() + " vidéo(s) — " + config.getCameraIp());
                }
            }

            @Override
            public void onFailure(String error) {
                binding.swipeRefresh.setRefreshing(false);
                binding.tvEmpty.setText("Erreur : " + error);
                binding.tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    // ─── Téléchargement ───────────────────────────────────────────────────────

    private void downloadVideo(VideoFile videoFile) {
        showToast("Téléchargement de " + videoFile.getName() + "...");

        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(videoFile.getUrl()));
            request.setTitle(videoFile.getName());
            request.setDescription("Téléchargement depuis la dashcam");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_MOVIES,
                    "Dashcam/" + videoFile.getName()
            );
            request.allowScanningByMediaScanner();

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                showToast("Enregistrement dans Vidéos/Dashcam");
            }
        } catch (Exception e) {
            showToast("Erreur : " + e.getMessage());
        }
    }

    // ─── Lecture directe sur la caméra ───────────────────────────────────────

    private void playVideo(VideoFile videoFile) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(videoFile.getUrl()), "video/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            showToast("Aucun lecteur vidéo disponible");
        }
    }

    // ─── Adapteur RecyclerView ────────────────────────────────────────────────

    private class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_video, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
            VideoFile vf = videoFiles.get(pos);

            h.tvName.setText(vf.getName());
            h.tvSize.setText(vf.getFormattedSize());
            h.tvDate.setText(vf.getDate());

            // Aperçu miniature
            if (vf.getThumbnailUrl() != null) {
                Glide.with(GalleryActivity.this)
                        .load(vf.getThumbnailUrl())
                        .placeholder(R.drawable.ic_video_placeholder)
                        .into(h.ivThumbnail);
            } else {
                h.ivThumbnail.setImageResource(R.drawable.ic_video_placeholder);
            }

            // Lecture
            h.itemView.setOnClickListener(v -> playVideo(vf));

            // Téléchargement
            h.btnDownload.setOnClickListener(v -> downloadVideo(vf));
        }

        @Override
        public int getItemCount() { return videoFiles.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumbnail;
            TextView  tvName, tvSize, tvDate;
            View      btnDownload;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivThumbnail = itemView.findViewById(R.id.iv_thumbnail);
                tvName      = itemView.findViewById(R.id.tv_name);
                tvSize      = itemView.findViewById(R.id.tv_size);
                tvDate      = itemView.findViewById(R.id.tv_date);
                btnDownload = itemView.findViewById(R.id.btn_download);
            }
        }
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
