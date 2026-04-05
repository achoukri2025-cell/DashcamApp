package com.dashcam.app.activities;

import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.dashcam.app.R;
import com.dashcam.app.databinding.ActivityLocalGalleryBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Galerie des vidéos enregistrées LOCALEMENT sur le téléphone
 * (dans Movies/Dashcam/).
 * Permet de lire, partager et supprimer les enregistrements locaux.
 */
public class LocalGalleryActivity extends AppCompatActivity {

    private ActivityLocalGalleryBinding binding;
    private LocalVideoAdapter           adapter;

    static class LocalVideo {
        long   id;
        String title;
        String path;
        long   size;
        long   duration; // ms
        long   dateAdded; // seconds
        Uri    contentUri;

        String getFormattedDuration() {
            long s = duration / 1000;
            return String.format(Locale.getDefault(), "%02d:%02d:%02d",
                    s / 3600, (s % 3600) / 60, s % 60);
        }

        String getFormattedSize() {
            if (size < 1024 * 1024) return String.format("%.0f KB", size / 1024.0);
            return String.format("%.1f MB", size / (1024.0 * 1024));
        }

        String getFormattedDate() {
            return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(new Date(dateAdded * 1000));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLocalGalleryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
        loadLocalVideos();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLocalVideos();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.tvTitle.setText("Mes enregistrements");

        adapter = new LocalVideoAdapter();
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerView.setAdapter(adapter);
        binding.swipeRefresh.setOnRefreshListener(this::loadLocalVideos);
    }

    // ─── Chargement MediaStore ────────────────────────────────────────────────

    private void loadLocalVideos() {
        binding.swipeRefresh.setRefreshing(true);
        List<LocalVideo> videos = new ArrayList<>();

        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATE_ADDED,
        };

        // Filtre sur le dossier Dashcam uniquement
        String selection = MediaStore.Video.Media.DATA + " LIKE ?";
        String[] selectionArgs = { "%/Movies/Dashcam/%" };
        String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, selection, selectionArgs, sortOrder)) {

            if (cursor != null) {
                int idIdx       = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                int nameIdx     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
                int pathIdx     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                int sizeIdx     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
                int durIdx      = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
                int dateIdx     = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED);

                while (cursor.moveToNext()) {
                    LocalVideo v = new LocalVideo();
                    v.id         = cursor.getLong(idIdx);
                    v.title      = cursor.getString(nameIdx);
                    v.path       = cursor.getString(pathIdx);
                    v.size       = cursor.getLong(sizeIdx);
                    v.duration   = cursor.getLong(durIdx);
                    v.dateAdded  = cursor.getLong(dateIdx);
                    v.contentUri = ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, v.id);
                    videos.add(v);
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Erreur lecture galerie: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        binding.swipeRefresh.setRefreshing(false);
        adapter.setVideos(videos);

        if (videos.isEmpty()) {
            binding.tvEmpty.setText("Aucun enregistrement local.\nAppuyez sur ● en vue directe pour commencer.");
            binding.tvEmpty.setVisibility(View.VISIBLE);
        } else {
            binding.tvEmpty.setVisibility(View.GONE);
            binding.tvCount.setText(videos.size() + " enregistrement(s)");
        }
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    private void playVideo(LocalVideo video) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(video.contentUri, "video/mp4");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Aucun lecteur vidéo installé", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareVideo(LocalVideo video) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("video/mp4");
        intent.putExtra(Intent.EXTRA_STREAM, video.contentUri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Partager via..."));
    }

    private void deleteVideo(LocalVideo video, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Supprimer ?")
                .setMessage("Supprimer " + video.title + " ?")
                .setPositiveButton("Supprimer", (d, w) -> {
                    try {
                        // Supprimer via MediaStore
                        getContentResolver().delete(video.contentUri, null, null);
                        // Supprimer le fichier physique
                        if (video.path != null) new File(video.path).delete();
                        adapter.removeAt(position);
                        Toast.makeText(this, "Supprimé", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    // ─── Adapteur ─────────────────────────────────────────────────────────────

    private class LocalVideoAdapter extends RecyclerView.Adapter<LocalVideoAdapter.VH> {

        private List<LocalVideo> videos = new ArrayList<>();

        void setVideos(List<LocalVideo> list) {
            this.videos = list;
            notifyDataSetChanged();
        }

        void removeAt(int pos) {
            videos.remove(pos);
            notifyItemRemoved(pos);
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_local_video, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int pos) {
            LocalVideo v = videos.get(pos);
            h.tvName.setText(v.title);
            h.tvSize.setText(v.getFormattedSize());
            h.tvDate.setText(v.getFormattedDate());
            h.tvDuration.setText(v.getFormattedDuration());

            // Miniature via Glide (supporte les URI MediaStore)
            Glide.with(LocalGalleryActivity.this)
                    .load(v.contentUri)
                    .placeholder(R.drawable.ic_video_placeholder)
                    .centerCrop()
                    .into(h.ivThumb);

            h.itemView.setOnClickListener(vw -> playVideo(v));
            h.btnShare.setOnClickListener(vw -> shareVideo(v));
            h.btnDelete.setOnClickListener(vw -> deleteVideo(v, h.getAdapterPosition()));
        }

        @Override
        public int getItemCount() { return videos.size(); }

        class VH extends RecyclerView.ViewHolder {
            ImageView ivThumb;
            TextView  tvName, tvSize, tvDate, tvDuration;
            View      btnShare, btnDelete;

            VH(@NonNull View v) {
                super(v);
                ivThumb   = v.findViewById(R.id.iv_thumbnail);
                tvName    = v.findViewById(R.id.tv_name);
                tvSize    = v.findViewById(R.id.tv_size);
                tvDate    = v.findViewById(R.id.tv_date);
                tvDuration = v.findViewById(R.id.tv_duration);
                btnShare  = v.findViewById(R.id.btn_share);
                btnDelete = v.findViewById(R.id.btn_delete);
            }
        }
    }
}
