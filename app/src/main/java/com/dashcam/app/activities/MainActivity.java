package com.dashcam.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dashcam.app.R;
import com.dashcam.app.databinding.ActivityMainBinding;
import com.dashcam.app.models.DashcamConfig;
import com.dashcam.app.utils.WifiHelper;
import com.dashcam.app.utils.DashcamApiClient;
import com.dashcam.app.utils.PrefsManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Activité principale : connexion à la dashcam
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 100;

    private ActivityMainBinding binding;
    private PrefsManager        prefsManager;
    private DashcamConfig       config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefsManager = new PrefsManager(this);
        config       = prefsManager.loadConfig();

        setupUI();
        requestPermissions();
        detectWifiNetwork();
    }

    // ─── UI Setup ─────────────────────────────────────────────────────────────

    private void setupUI() {
        // Pré-remplir les champs avec la config sauvegardée
        binding.editCameraIp.setText(config.getCameraIp());
        binding.editCameraPort.setText(String.valueOf(config.getCameraPort()));

        // Sélection du protocole
        binding.spinnerProtocol.setSelection(
                config.getProtocol().equals(DashcamConfig.PROTOCOL_RTSP) ? 0 :
                config.getProtocol().equals(DashcamConfig.PROTOCOL_HTTP) ? 1 : 2
        );

        // Bouton connexion
        binding.btnConnect.setOnClickListener(v -> {
            if (validateAndSaveConfig()) {
                testConnection();
            }
        });

        // Bouton vue directe (sans test)
        binding.btnLiveView.setOnClickListener(v -> {
            if (validateAndSaveConfig()) {
                openLiveView();
            }
        });

        // Bouton galerie SD
        binding.btnGallery.setOnClickListener(v -> {
            if (validateAndSaveConfig()) {
                openGallery();
            }
        });

        // Bouton galerie locale
        binding.btnLocalGallery.setOnClickListener(v ->
                startActivity(new Intent(this, LocalGalleryActivity.class))
        );

        // Bouton paramètres
        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class))
        );
    }

    // ─── Validation config ────────────────────────────────────────────────────

    private boolean validateAndSaveConfig() {
        String ip   = binding.editCameraIp.getText().toString().trim();
        String port = binding.editCameraPort.getText().toString().trim();

        if (ip.isEmpty()) {
            binding.editCameraIp.setError("Entrez l'adresse IP de la caméra");
            return false;
        }
        if (port.isEmpty()) {
            binding.editCameraPort.setError("Entrez le port");
            return false;
        }

        config.setCameraIp(ip);
        config.setCameraPort(Integer.parseInt(port));

        // Protocole
        int selIdx = binding.spinnerProtocol.getSelectedItemPosition();
        config.setProtocol(selIdx == 0 ? DashcamConfig.PROTOCOL_RTSP :
                           selIdx == 1 ? DashcamConfig.PROTOCOL_HTTP :
                                         DashcamConfig.PROTOCOL_HLS);

        prefsManager.saveConfig(config);
        return true;
    }

    // ─── Test de connexion ────────────────────────────────────────────────────

    private void testConnection() {
        setLoading(true);
        binding.tvStatus.setText("Connexion en cours...");

        DashcamApiClient api = new DashcamApiClient(config);
        api.testConnection(new DashcamApiClient.ConnectionCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                binding.tvStatus.setText("✓ Connecté à " + config.getCameraIp());
                binding.cardActions.setVisibility(View.VISIBLE);
                showToast("Caméra trouvée !");
            }

            @Override
            public void onFailure(String error) {
                setLoading(false);
                binding.tvStatus.setText("✗ Connexion échouée");
                showToast("Erreur: " + error);
            }
        });
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private void openLiveView() {
        Intent intent = new Intent(this, LiveViewActivity.class);
        startActivity(intent);
    }

    private void openGallery() {
        Intent intent = new Intent(this, GalleryActivity.class);
        startActivity(intent);
    }

    // ─── Détection WiFi ──────────────────────────────────────────────────────

    private void detectWifiNetwork() {
        WifiHelper wifiHelper = new WifiHelper(this);
        String hint = wifiHelper.getConnectionHint();
        binding.tvWifiName.setText(hint);
        binding.tvWifiName.setVisibility(View.VISIBLE);

        // Auto-remplir l'IP suggérée si le champ est vide ou par défaut
        String currentIp = binding.editCameraIp.getText().toString().trim();
        if ((currentIp.isEmpty() || currentIp.equals("192.168.1.1"))
                && wifiHelper.isWifiConnected()) {
            String suggestedIp = wifiHelper.guessGatewayIp();
            binding.editCameraIp.setText(suggestedIp);
        }
    }

    // ─── Permissions ─────────────────────────────────────────────────────────

    private void requestPermissions() {
        List<String> needed = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    needed.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    showToast("Certaines permissions sont requises pour l'enregistrement");
                    break;
                }
            }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnConnect.setEnabled(!loading);
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        config = prefsManager.loadConfig();
        detectWifiNetwork();
    }
}
