package com.dashcam.app.activities;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.dashcam.app.R;
import com.dashcam.app.databinding.ActivitySettingsBinding;
import com.dashcam.app.models.DashcamConfig;
import com.dashcam.app.utils.PrefsManager;

/**
 * Paramètres avancés de la connexion dashcam
 */
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private PrefsManager            prefsManager;
    private DashcamConfig           config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefsManager = new PrefsManager(this);
        config       = prefsManager.loadConfig();

        setupUI();
    }

    private void setupUI() {
        binding.btnBack.setOnClickListener(v -> finish());

        // Pré-remplir avec la config actuelle
        binding.editIp.setText(config.getCameraIp());
        binding.editRtspPort.setText(String.valueOf(config.getCameraPort()));
        binding.editStreamPath.setText(config.getStreamPath());
        binding.editUsername.setText(config.getUsername());
        binding.editPassword.setText(config.getPassword());
        binding.editSdPath.setText(config.getSdCardPath());
        binding.editCustomStreamUrl.setText(config.getLiveStreamUrl());

        // Protocole
        String[] protocols = {"RTSP", "HTTP MJPEG", "HLS"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, protocols);
        binding.spinnerProtocol.setAdapter(adapter);
        int selIdx = config.getProtocol().equals(DashcamConfig.PROTOCOL_RTSP) ? 0 :
                     config.getProtocol().equals(DashcamConfig.PROTOCOL_HTTP) ? 1 : 2;
        binding.spinnerProtocol.setSelection(selIdx);

        // Qualité enregistrement
        String[] qualities = {"Haute (original)", "Moyenne", "Basse"};
        ArrayAdapter<String> qAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, qualities);
        binding.spinnerQuality.setAdapter(qAdapter);

        // URLs générées (lecture seule)
        updateGeneratedUrls();

        binding.editIp.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void onChanged(String s) { updateGeneratedUrls(); }
        });

        // Sauvegarde
        binding.btnSave.setOnClickListener(v -> saveConfig());

        // Dashcams connues : pré-remplir les valeurs typiques
        binding.btnPresetGeneric.setOnClickListener(v -> applyPreset("192.168.1.1", 554, "/live/channel0", "/DCIM/"));
        binding.btnPresetViofo.setOnClickListener(v  -> applyPreset("192.168.1.254", 554, "/live", "/?action=listfile"));
        binding.btnPresetBlackvue.setOnClickListener(v -> applyPreset("10.99.77.1", 7070, "/blackvue_live.m3u8", "/blackvue/record/"));
    }

    private void applyPreset(String ip, int port, String stream, String sdPath) {
        binding.editIp.setText(ip);
        binding.editRtspPort.setText(String.valueOf(port));
        binding.editStreamPath.setText(stream);
        binding.editSdPath.setText(sdPath);
        updateGeneratedUrls();
        Toast.makeText(this, "Preset appliqué", Toast.LENGTH_SHORT).show();
    }

    private void updateGeneratedUrls() {
        String ip   = binding.editIp.getText().toString().trim();
        String port = binding.editRtspPort.getText().toString().trim();
        String path = binding.editStreamPath.getText().toString().trim();
        if (ip.isEmpty() || port.isEmpty()) return;

        String rtspUrl = "rtsp://" + ip + ":" + port + path;
        String httpUrl = "http://" + ip + path;
        binding.tvUrlPreview.setText("RTSP : " + rtspUrl + "\nHTTP : " + httpUrl);
    }

    private void saveConfig() {
        config.setCameraIp(binding.editIp.getText().toString().trim());
        try { config.setCameraPort(Integer.parseInt(binding.editRtspPort.getText().toString())); }
        catch (NumberFormatException e) { config.setCameraPort(554); }
        config.setStreamPath(binding.editStreamPath.getText().toString().trim());
        config.setUsername(binding.editUsername.getText().toString().trim());
        config.setPassword(binding.editPassword.getText().toString().trim());
        config.setSdCardPath(binding.editSdPath.getText().toString().trim());

        String customUrl = binding.editCustomStreamUrl.getText().toString().trim();
        config.setLiveStreamUrl(customUrl);

        int selIdx = binding.spinnerProtocol.getSelectedItemPosition();
        config.setProtocol(selIdx == 0 ? DashcamConfig.PROTOCOL_RTSP :
                           selIdx == 1 ? DashcamConfig.PROTOCOL_HTTP :
                                         DashcamConfig.PROTOCOL_HLS);

        prefsManager.saveConfig(config);
        Toast.makeText(this, "Paramètres sauvegardés", Toast.LENGTH_SHORT).show();
        finish();
    }

    // Watcher simplifié
    private static abstract class SimpleTextWatcher implements android.text.TextWatcher {
        public abstract void onChanged(String s);
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { onChanged(s.toString()); }
        @Override public void afterTextChanged(android.text.Editable s) {}
    }
}
