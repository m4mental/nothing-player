package com.nothing.player;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VaultActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvHeaderTitle, tvHeaderSubtitle;
    private Button btnConfig;

    // Authentication HUD Views
    private View layoutVaultLock;
    private TextView tvPinHeading, tvPinSubheading, tvPinError;

    // PIN Mode
    private LinearLayout containerPinMode;
    private View pinDot1, pinDot2, pinDot3, pinDot4;
    private EditText etVaultPinInput;
    private Button btnSubmitVaultPin;

    // Pattern Mode
    private LinearLayout containerPatternMode;
    private DotMatrixPatternView patternViewVault;
    private TextView tvPatternHint;
    private Button btnSubmitVaultPattern;

    // Biometric & Mode Switch
    private Button btnVaultBiometricQuick;
    private TextView tvSwitchAuthMode;

    // Vault Content
    private FrameLayout layoutVaultContent;
    private LinearLayout layoutVaultEmpty;
    private RecyclerView rvVaultVideos;
    private VaultVideosAdapter vaultAdapter;
    private List<VaultManager.VaultItem> vaultItemList = new ArrayList<>();

    private String currentLockType = VaultManager.LOCK_TYPE_PIN;
    private boolean isSettingNewLock = false;
    private boolean isUnlocked = false;
    private String recordedTempPattern = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        initViews();
        setupListeners();
        checkLockState(true);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back_vault);
        tvHeaderTitle = findViewById(R.id.tv_vault_header_title);
        tvHeaderSubtitle = findViewById(R.id.tv_vault_header_subtitle);
        btnConfig = findViewById(R.id.btn_vault_config);

        layoutVaultLock = findViewById(R.id.layout_vault_lock);
        tvPinHeading = findViewById(R.id.tv_pin_heading);
        tvPinSubheading = findViewById(R.id.tv_pin_subheading);
        tvPinError = findViewById(R.id.tv_pin_error);

        // PIN
        containerPinMode = findViewById(R.id.container_pin_mode);
        pinDot1 = findViewById(R.id.pin_dot_1);
        pinDot2 = findViewById(R.id.pin_dot_2);
        pinDot3 = findViewById(R.id.pin_dot_3);
        pinDot4 = findViewById(R.id.pin_dot_4);
        etVaultPinInput = findViewById(R.id.et_vault_pin_input);
        btnSubmitVaultPin = findViewById(R.id.btn_submit_vault_pin);

        // Pattern
        containerPatternMode = findViewById(R.id.container_pattern_mode);
        patternViewVault = findViewById(R.id.pattern_view_vault);
        tvPatternHint = findViewById(R.id.tv_pattern_hint);
        btnSubmitVaultPattern = findViewById(R.id.btn_submit_vault_pattern);

        // Biometric & Switch
        btnVaultBiometricQuick = findViewById(R.id.btn_vault_biometric_quick);
        tvSwitchAuthMode = findViewById(R.id.tv_switch_auth_mode);

        // Vault Content
        layoutVaultContent = findViewById(R.id.layout_vault_content);
        layoutVaultEmpty = findViewById(R.id.layout_vault_empty);
        rvVaultVideos = findViewById(R.id.rv_vault_videos);

        rvVaultVideos.setLayoutManager(new LinearLayoutManager(this));
        vaultAdapter = new VaultVideosAdapter();
        rvVaultVideos.setAdapter(vaultAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // PIN TextWatcher
        etVaultPinInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int len = s.length();
                updatePinDots(len);
                if (tvPinError.getVisibility() == View.VISIBLE) {
                    tvPinError.setVisibility(View.GONE);
                }
                if (len == 4 && !isSettingNewLock && VaultManager.isPinSet(VaultActivity.this)) {
                    verifyAndUnlockPin(s.toString());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSubmitVaultPin.setOnClickListener(v -> {
            String pin = etVaultPinInput.getText().toString().trim();
            if (isSettingNewLock || !VaultManager.isPinSet(this)) {
                if (pin.length() < 4) {
                    showError("PIN MUST BE 4 DIGITS");
                } else {
                    VaultManager.setPin(this, pin);
                    VaultManager.setLockType(this, VaultManager.LOCK_TYPE_PIN);
                    Toast.makeText(this, "Vault PIN Configured Successfully", Toast.LENGTH_SHORT).show();
                    isSettingNewLock = false;
                    unlockVault();
                }
            } else {
                verifyAndUnlockPin(pin);
            }
        });

        // Pattern Listener
        patternViewVault.setOnPatternListener(new DotMatrixPatternView.OnPatternListener() {
            @Override
            public void onPatternStart() {
                if (tvPinError.getVisibility() == View.VISIBLE) {
                    tvPinError.setVisibility(View.GONE);
                }
            }

            @Override
            public void onPatternComplete(String pattern) {
                if (isSettingNewLock || !VaultManager.isPatternSet(VaultActivity.this)) {
                    handlePatternSetup(pattern);
                } else {
                    handlePatternUnlock(pattern);
                }
            }
        });

        btnSubmitVaultPattern.setOnClickListener(v -> {
            if (recordedTempPattern != null) {
                VaultManager.setPattern(this, recordedTempPattern);
                VaultManager.setLockType(this, VaultManager.LOCK_TYPE_PATTERN);
                Toast.makeText(this, "Pattern Configured Successfully", Toast.LENGTH_SHORT).show();
                isSettingNewLock = false;
                recordedTempPattern = null;
                btnSubmitVaultPattern.setVisibility(View.GONE);
                unlockVault();
            }
        });

        // Biometric Quick Unlock
        btnVaultBiometricQuick.setOnClickListener(v -> promptBiometrics());

        // Switch between PIN and Pattern
        tvSwitchAuthMode.setOnClickListener(v -> {
            if (VaultManager.LOCK_TYPE_PIN.equals(currentLockType)) {
                currentLockType = VaultManager.LOCK_TYPE_PATTERN;
            } else {
                currentLockType = VaultManager.LOCK_TYPE_PIN;
            }
            checkLockState(false);
        });

        // Security Configuration Button
        btnConfig.setOnClickListener(v -> showSecurityConfigDialog());
    }

    private void checkLockState(boolean allowAutoBiometric) {
        currentLockType = VaultManager.getLockType(this);
        boolean isConfigured = VaultManager.isSecurityConfigured(this);

        if (!isConfigured || isSettingNewLock) {
            setupModeUI();
        } else if (!isUnlocked) {
            unlockModeUI(allowAutoBiometric);
        } else {
            unlockVault();
        }
    }

    private void setupModeUI() {
        btnConfig.setVisibility(View.GONE);
        layoutVaultLock.setVisibility(View.VISIBLE);
        layoutVaultContent.setVisibility(View.GONE);
        btnVaultBiometricQuick.setVisibility(View.GONE);

        if (VaultManager.LOCK_TYPE_PATTERN.equals(currentLockType)) {
            containerPatternMode.setVisibility(View.VISIBLE);
            containerPinMode.setVisibility(View.GONE);
            tvPinHeading.setText("SET 3X3 PATTERN");
            tvPinSubheading.setText("Draw a dot-matrix pattern with at least 4 dots");
            tvPatternHint.setText("DRAW PATTERN TO RECORD");
            tvSwitchAuthMode.setText("SWITCH TO PIN SETUP");
        } else {
            containerPinMode.setVisibility(View.VISIBLE);
            containerPatternMode.setVisibility(View.GONE);
            tvPinHeading.setText("SET 4-DIGIT PIN");
            tvPinSubheading.setText("Choose a security PIN to protect your private videos");
            btnSubmitVaultPin.setText("CONFIRM & SET PIN");
            tvSwitchAuthMode.setText("SWITCH TO PATTERN SETUP");
        }
    }

    private void unlockModeUI(boolean allowAutoBiometric) {
        btnConfig.setVisibility(View.VISIBLE);
        layoutVaultLock.setVisibility(View.VISIBLE);
        layoutVaultContent.setVisibility(View.GONE);

        // Biometric Button visibility
        boolean bioAvailable = BiometricAuthHelper.isBiometricAvailable(this);
        boolean bioEnabled = VaultManager.isBiometricEnabled(this);
        if (bioAvailable && bioEnabled) {
            btnVaultBiometricQuick.setVisibility(View.VISIBLE);
            if (allowAutoBiometric && VaultManager.isAutoBiometricPrompt(this)) {
                promptBiometrics();
            }
        } else {
            btnVaultBiometricQuick.setVisibility(View.GONE);
        }

        if (VaultManager.LOCK_TYPE_PATTERN.equals(currentLockType)) {
            containerPatternMode.setVisibility(View.VISIBLE);
            containerPinMode.setVisibility(View.GONE);
            tvPinHeading.setText("DRAW UNLOCK PATTERN");
            tvPinSubheading.setText("Connect dots to unlock private videos");
            tvPatternHint.setText("CONNECT SAVED PATTERN");
            btnSubmitVaultPattern.setVisibility(View.GONE);
            tvSwitchAuthMode.setText("SWITCH TO PIN UNLOCK");
            patternViewVault.reset();
        } else {
            containerPinMode.setVisibility(View.VISIBLE);
            containerPatternMode.setVisibility(View.GONE);
            tvPinHeading.setText("UNLOCK PRIVATE VAULT");
            tvPinSubheading.setText("Enter your 4-digit PIN to access private videos");
            btnSubmitVaultPin.setText("UNLOCK VAULT");
            tvSwitchAuthMode.setText("SWITCH TO PATTERN UNLOCK");
        }
    }

    private void handlePatternSetup(String pattern) {
        if (pattern.length() < 4) {
            showError("CONNECT AT LEAST 4 DOTS");
            patternViewVault.showError();
            return;
        }

        if (recordedTempPattern == null) {
            recordedTempPattern = pattern;
            tvPatternHint.setText("DRAW SAME PATTERN TO CONFIRM");
            Toast.makeText(this, "Pattern recorded. Draw again to confirm", Toast.LENGTH_SHORT).show();
            patternViewVault.reset();
        } else {
            if (recordedTempPattern.equals(pattern)) {
                VaultManager.setPattern(this, recordedTempPattern);
                VaultManager.setLockType(this, VaultManager.LOCK_TYPE_PATTERN);
                Toast.makeText(this, "Pattern Lock Configured Successfully", Toast.LENGTH_SHORT).show();
                isSettingNewLock = false;
                recordedTempPattern = null;
                unlockVault();
            } else {
                showError("PATTERNS DID NOT MATCH. TRY AGAIN");
                patternViewVault.showError();
                recordedTempPattern = null;
                tvPatternHint.setText("DRAW PATTERN TO RECORD");
            }
        }
    }

    private void handlePatternUnlock(String pattern) {
        if (VaultManager.verifyPattern(this, pattern)) {
            unlockVault();
        } else {
            showError("INCORRECT PATTERN. TRY AGAIN");
            patternViewVault.showError();
        }
    }

    private void updatePinDots(int length) {
        pinDot1.setBackgroundResource(length >= 1 ? R.drawable.led_active_pin : R.drawable.led_inactive);
        pinDot2.setBackgroundResource(length >= 2 ? R.drawable.led_active_pin : R.drawable.led_inactive);
        pinDot3.setBackgroundResource(length >= 3 ? R.drawable.led_active_pin : R.drawable.led_inactive);
        pinDot4.setBackgroundResource(length >= 4 ? R.drawable.led_active_pin : R.drawable.led_inactive);
    }

    private void verifyAndUnlockPin(String pin) {
        if (VaultManager.verifyPin(this, pin)) {
            unlockVault();
        } else {
            vibrateError();
            showError("INCORRECT PIN. TRY AGAIN");
            etVaultPinInput.setText("");
        }
    }

    private void promptBiometrics() {
        if (!BiometricAuthHelper.isBiometricAvailable(this)) {
            Toast.makeText(this, "Biometrics not available on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        String fallbackText = VaultManager.LOCK_TYPE_PATTERN.equals(currentLockType) ? "USE PATTERN" : "USE PIN";
        BiometricAuthHelper.authenticate(
                this,
                "NOTHING VAULT SECURITY",
                "Touch fingerprint sensor or look at camera",
                fallbackText,
                new BiometricAuthHelper.BiometricCallback() {
                    @Override
                    public void onAuthenticationSucceeded() {
                        Toast.makeText(VaultActivity.this, "Biometric Verified", Toast.LENGTH_SHORT).show();
                        unlockVault();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        showError("BIOMETRIC RECOGNITION FAILED");
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, String errString) {
                        // User cancelled or pressed fallback button
                    }
                }
        );
    }

    private void showSecurityConfigDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_vault_security_config, null);

        Button btnSelectPin = dialogView.findViewById(R.id.btn_config_select_pin);
        Button btnSelectPattern = dialogView.findViewById(R.id.btn_config_select_pattern);
        SwitchCompat switchBio = dialogView.findViewById(R.id.switch_config_biometric);
        SwitchCompat switchAutoBio = dialogView.findViewById(R.id.switch_config_auto_bio);
        TextView tvBioStatus = dialogView.findViewById(R.id.tv_config_biometric_status);
        Button btnCancel = dialogView.findViewById(R.id.btn_config_cancel);
        Button btnSave = dialogView.findViewById(R.id.btn_config_save);

        // Read current values
        final String[] chosenLock = { VaultManager.getLockType(this) };
        boolean bioAvailable = BiometricAuthHelper.isBiometricAvailable(this);
        tvBioStatus.setText("STATUS: " + BiometricAuthHelper.getBiometricStatus(this));

        // Update Tab visual
        Runnable updateTabs = () -> {
            if (VaultManager.LOCK_TYPE_PATTERN.equals(chosenLock[0])) {
                btnSelectPattern.setBackgroundResource(R.drawable.bg_button_red);
                btnSelectPattern.setTextColor(Color.WHITE);
                btnSelectPin.setBackgroundResource(R.drawable.bg_chip_unselected);
                btnSelectPin.setTextColor(Color.parseColor("#B3FFFFFF"));
            } else {
                btnSelectPin.setBackgroundResource(R.drawable.bg_button_red);
                btnSelectPin.setTextColor(Color.WHITE);
                btnSelectPattern.setBackgroundResource(R.drawable.bg_chip_unselected);
                btnSelectPattern.setTextColor(Color.parseColor("#B3FFFFFF"));
            }
        };
        updateTabs.run();

        btnSelectPin.setOnClickListener(v -> {
            chosenLock[0] = VaultManager.LOCK_TYPE_PIN;
            updateTabs.run();
        });

        btnSelectPattern.setOnClickListener(v -> {
            chosenLock[0] = VaultManager.LOCK_TYPE_PATTERN;
            updateTabs.run();
        });

        switchBio.setEnabled(bioAvailable);
        switchBio.setChecked(bioAvailable && VaultManager.isBiometricEnabled(this));
        switchAutoBio.setEnabled(switchBio.isChecked());
        switchAutoBio.setChecked(VaultManager.isAutoBiometricPrompt(this));

        switchBio.setOnCheckedChangeListener((btn, isChecked) -> switchAutoBio.setEnabled(isChecked));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            VaultManager.setLockType(this, chosenLock[0]);
            VaultManager.setBiometricEnabled(this, switchBio.isChecked());
            VaultManager.setAutoBiometricPrompt(this, switchAutoBio.isChecked());
            currentLockType = chosenLock[0];

            Toast.makeText(this, "Security Configuration Saved", Toast.LENGTH_SHORT).show();
            dialog.dismiss();

            // If selected lock type has no code recorded yet, prompt recording
            if (VaultManager.LOCK_TYPE_PIN.equals(chosenLock[0]) && !VaultManager.isPinSet(this)) {
                isSettingNewLock = true;
                isUnlocked = false;
                checkLockState(false);
            } else if (VaultManager.LOCK_TYPE_PATTERN.equals(chosenLock[0]) && !VaultManager.isPatternSet(this)) {
                isSettingNewLock = true;
                isUnlocked = false;
                checkLockState(false);
            } else {
                checkLockState(false);
            }
        });

        dialog.show();
    }

    private void unlockVault() {
        isUnlocked = true;
        layoutVaultLock.setVisibility(View.GONE);
        layoutVaultContent.setVisibility(View.VISIBLE);
        btnConfig.setVisibility(View.VISIBLE);
        loadVaultVideos();
    }

    private void showError(String msg) {
        tvPinError.setText(msg);
        tvPinError.setVisibility(View.VISIBLE);
    }

    private void vibrateError() {
        try {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(120);
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadVaultVideos() {
        vaultItemList = VaultManager.getVaultItems(this);
        if (vaultItemList.isEmpty()) {
            layoutVaultEmpty.setVisibility(View.VISIBLE);
            rvVaultVideos.setVisibility(View.GONE);
            tvHeaderSubtitle.setText("0 ENCRYPTED VIDEOS");
        } else {
            layoutVaultEmpty.setVisibility(View.GONE);
            rvVaultVideos.setVisibility(View.VISIBLE);
            tvHeaderSubtitle.setText(vaultItemList.size() + " ENCRYPTED VIDEO" + (vaultItemList.size() > 1 ? "S" : ""));
            vaultAdapter.notifyDataSetChanged();
        }
    }

    private class VaultVideosAdapter extends RecyclerView.Adapter<VaultVideosAdapter.VaultViewHolder> {

        @NonNull
        @Override
        public VaultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vault_video, parent, false);
            return new VaultViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VaultViewHolder holder, int position) {
            VaultManager.VaultItem item = vaultItemList.get(position);

            holder.tvTitle.setText(item.title);
            String sizeStr = formatFileSize(item.size);
            String dateStr = formatDate(item.addedAt);
            holder.tvInfo.setText(sizeStr + " • " + dateStr);

            if (item.duration > 0) {
                holder.tvDuration.setVisibility(View.VISIBLE);
                holder.tvDuration.setText(formatDuration(item.duration));
            } else {
                holder.tvDuration.setVisibility(View.GONE);
            }

            Glide.with(VaultActivity.this)
                    .load(new File(item.vaultPath))
                    .centerCrop()
                    .into(holder.ivThumbnail);

            holder.btnPlay.setOnClickListener(v -> playVideo(item));
            holder.itemView.setOnClickListener(v -> playVideo(item));

            holder.btnRestore.setOnClickListener(v -> {
                NothingDialogHelper.showConfirmDialog(
                        VaultActivity.this,
                        "RESTORE VIDEO",
                        "Restore \"" + item.title + "\" to its original folder?",
                        "RESTORE",
                        () -> {
                            if (VaultManager.restoreItem(VaultActivity.this, item)) {
                                Toast.makeText(VaultActivity.this, "Restored to storage", Toast.LENGTH_SHORT).show();
                                loadVaultVideos();
                            } else {
                                Toast.makeText(VaultActivity.this, "Failed to restore video", Toast.LENGTH_SHORT).show();
                            }
                        },
                        "CANCEL",
                        null
                );
            });

            holder.btnDelete.setOnClickListener(v -> {
                NothingDialogHelper.showConfirmDialog(
                        VaultActivity.this,
                        "PERMANENT DELETE",
                        "Permanently delete \"" + item.title + "\"?\n\nThis cannot be recovered.",
                        "DELETE",
                        () -> {
                            VaultManager.deletePermanently(VaultActivity.this, item);
                            Toast.makeText(VaultActivity.this, "Deleted permanently", Toast.LENGTH_SHORT).show();
                            loadVaultVideos();
                        },
                        "CANCEL",
                        null
                );
            });
        }

        @Override
        public int getItemCount() {
            return vaultItemList.size();
        }

        private void playVideo(VaultManager.VaultItem item) {
            Intent intent = new Intent(VaultActivity.this, ExoVideoPlayerActivity.class);
            intent.putExtra("path", item.vaultPath);
            intent.putExtra("video_path", item.vaultPath);
            intent.putExtra("title", item.title);
            intent.putExtra("video_title", item.title);
            startActivity(intent);
        }

        class VaultViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumbnail;
            TextView tvDuration, tvTitle, tvInfo;
            Button btnPlay, btnRestore, btnDelete;

            VaultViewHolder(@NonNull View itemView) {
                super(itemView);
                ivThumbnail = itemView.findViewById(R.id.iv_vault_thumb);
                tvDuration = itemView.findViewById(R.id.tv_vault_duration);
                tvTitle = itemView.findViewById(R.id.tv_vault_title);
                tvInfo = itemView.findViewById(R.id.tv_vault_info);
                btnPlay = itemView.findViewById(R.id.btn_vault_play);
                btnRestore = itemView.findViewById(R.id.btn_vault_restore);
                btnDelete = itemView.findViewById(R.id.btn_vault_delete);
            }
        }
    }

    private static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024 * 1024) {
            return String.format(Locale.US, "%.1f KB", bytes / 1024f);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f));
        } else {
            return String.format(Locale.US, "%.2f GB", bytes / (1024f * 1024f * 1024f));
        }
    }

    private static String formatDate(long timestamp) {
        if (timestamp <= 0) return "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "";
        }
    }

    private static String formatDuration(long ms) {
        long sec = (ms / 1000) % 60;
        long min = (ms / (1000 * 60)) % 60;
        long hr = ms / (1000 * 60 * 60);
        if (hr > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hr, min, sec);
        }
        return String.format(Locale.US, "%02d:%02d", min, sec);
    }
}
