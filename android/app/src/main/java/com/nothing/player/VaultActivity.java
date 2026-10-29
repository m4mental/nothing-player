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
import androidx.appcompat.app.AppCompatActivity;
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
    private Button btnChangePin;

    // PIN Lock Screen Views
    private LinearLayout layoutVaultLock;
    private TextView tvPinHeading, tvPinSubheading, tvPinError;
    private View pinDot1, pinDot2, pinDot3, pinDot4;
    private EditText etVaultPinInput;
    private Button btnSubmitVaultPin;

    // Content Views
    private FrameLayout layoutVaultContent;
    private LinearLayout layoutVaultEmpty;
    private RecyclerView rvVaultVideos;
    private VaultVideosAdapter vaultAdapter;
    private List<VaultManager.VaultItem> vaultItemList = new ArrayList<>();

    private boolean isSettingNewPin = false;
    private boolean isUnlocked = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vault);

        initViews();
        setupListeners();
        checkLockState();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back_vault);
        tvHeaderTitle = findViewById(R.id.tv_vault_header_title);
        tvHeaderSubtitle = findViewById(R.id.tv_vault_header_subtitle);
        btnChangePin = findViewById(R.id.btn_vault_change_pin);

        layoutVaultLock = findViewById(R.id.layout_vault_lock);
        tvPinHeading = findViewById(R.id.tv_pin_heading);
        tvPinSubheading = findViewById(R.id.tv_pin_subheading);
        tvPinError = findViewById(R.id.tv_pin_error);

        pinDot1 = findViewById(R.id.pin_dot_1);
        pinDot2 = findViewById(R.id.pin_dot_2);
        pinDot3 = findViewById(R.id.pin_dot_3);
        pinDot4 = findViewById(R.id.pin_dot_4);

        etVaultPinInput = findViewById(R.id.et_vault_pin_input);
        btnSubmitVaultPin = findViewById(R.id.btn_submit_vault_pin);

        layoutVaultContent = findViewById(R.id.layout_vault_content);
        layoutVaultEmpty = findViewById(R.id.layout_vault_empty);
        rvVaultVideos = findViewById(R.id.rv_vault_videos);

        rvVaultVideos.setLayoutManager(new LinearLayoutManager(this));
        vaultAdapter = new VaultVideosAdapter();
        rvVaultVideos.setAdapter(vaultAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        etVaultPinInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int len = s.length();
                updatePinDots(len);
                if (tvPinError.getVisibility() == View.VISIBLE) {
                    tvPinError.setVisibility(View.GONE);
                }
                // Auto verify on 4 digits if already configured
                if (len == 4 && !isSettingNewPin && VaultManager.isPinSet(VaultActivity.this)) {
                    verifyAndUnlock(s.toString());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnSubmitVaultPin.setOnClickListener(v -> {
            String pin = etVaultPinInput.getText().toString().trim();
            if (isSettingNewPin || !VaultManager.isPinSet(this)) {
                if (pin.length() < 4) {
                    showError("PIN MUST BE 4 DIGITS");
                } else {
                    VaultManager.setPin(this, pin);
                    Toast.makeText(this, "Vault PIN Configured Successfully", Toast.LENGTH_SHORT).show();
                    isSettingNewPin = false;
                    unlockVault();
                }
            } else {
                verifyAndUnlock(pin);
            }
        });

        btnChangePin.setOnClickListener(v -> {
            NothingDialogHelper.showConfirmDialog(
                    this,
                    "RESET VAULT PIN",
                    "Do you want to change your Private Vault PIN?",
                    "CHANGE PIN",
                    () -> {
                        isUnlocked = false;
                        isSettingNewPin = true;
                        etVaultPinInput.setText("");
                        checkLockState();
                    },
                    "CANCEL",
                    null
            );
        });
    }

    private void checkLockState() {
        if (!VaultManager.isPinSet(this) || isSettingNewPin) {
            // Setup PIN Mode
            tvPinHeading.setText("SET 4-DIGIT PIN");
            tvPinSubheading.setText("Choose a security PIN to protect your private videos");
            btnSubmitVaultPin.setText("CONFIRM & SET PIN");
            btnChangePin.setVisibility(View.GONE);
            layoutVaultLock.setVisibility(View.VISIBLE);
            layoutVaultContent.setVisibility(View.GONE);
        } else if (!isUnlocked) {
            // Unlock PIN Mode
            tvPinHeading.setText("UNLOCK PRIVATE VAULT");
            tvPinSubheading.setText("Enter your 4-digit PIN to access private videos");
            btnSubmitVaultPin.setText("UNLOCK VAULT");
            btnChangePin.setVisibility(View.GONE);
            layoutVaultLock.setVisibility(View.VISIBLE);
            layoutVaultContent.setVisibility(View.GONE);
        } else {
            unlockVault();
        }
    }

    private void updatePinDots(int length) {
        pinDot1.setBackgroundResource(length >= 1 ? R.drawable.led_active_pin : R.drawable.led_inactive);
        pinDot2.setBackgroundResource(length >= 2 ? R.drawable.led_active_pin : R.drawable.led_inactive);
        pinDot3.setBackgroundResource(length >= 3 ? R.drawable.led_active_pin : R.drawable.led_inactive);
        pinDot4.setBackgroundResource(length >= 4 ? R.drawable.led_active_pin : R.drawable.led_inactive);
    }

    private void verifyAndUnlock(String pin) {
        if (VaultManager.verifyPin(this, pin)) {
            unlockVault();
        } else {
            vibrateError();
            showError("INCORRECT PIN. TRY AGAIN");
            etVaultPinInput.setText("");
        }
    }

    private void unlockVault() {
        isUnlocked = true;
        layoutVaultLock.setVisibility(View.GONE);
        layoutVaultContent.setVisibility(View.VISIBLE);
        btnChangePin.setVisibility(View.VISIBLE);
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

            holder.tvDuration.setText(formatDuration(item.duration));

            // Load Video Thumbnail
            if (item.vaultPath != null) {
                Glide.with(VaultActivity.this)
                        .load(new File(item.vaultPath))
                        .centerCrop()
                        .placeholder(R.color.nothing_black)
                        .into(holder.ivThumb);
            }

            // Play in Player
            holder.btnPlay.setOnClickListener(v -> {
                Intent intent = new Intent(VaultActivity.this, ExoVideoPlayerActivity.class);
                intent.putExtra("path", item.vaultPath);
                intent.putExtra("video_path", item.vaultPath);
                intent.putExtra("title", item.title);
                intent.putExtra("video_title", item.title);
                startActivity(intent);
            });

            // Restore Video to Storage
            holder.btnRestore.setOnClickListener(v -> {
                NothingDialogHelper.showConfirmDialog(
                        VaultActivity.this,
                        "RESTORE VIDEO",
                        "Restore \"" + item.title + "\" to its original folder?",
                        "RESTORE",
                        () -> {
                            if (VaultManager.restoreItem(VaultActivity.this, item)) {
                                Toast.makeText(VaultActivity.this, "Restored to original location", Toast.LENGTH_SHORT).show();
                                loadVaultVideos();
                            } else {
                                Toast.makeText(VaultActivity.this, "Failed to restore video", Toast.LENGTH_SHORT).show();
                            }
                        },
                        "CANCEL",
                        null
                );
            });

            // Permanently Delete Video
            holder.btnDelete.setOnClickListener(v -> {
                NothingDialogHelper.showConfirmDialog(
                        VaultActivity.this,
                        "PERMANENT DELETE",
                        "Permanently delete \"" + item.title + "\"?\n\nThis video cannot be recovered.",
                        "DELETE FOREVER",
                        () -> {
                            if (VaultManager.deletePermanently(VaultActivity.this, item)) {
                                Toast.makeText(VaultActivity.this, "Video deleted permanently", Toast.LENGTH_SHORT).show();
                                loadVaultVideos();
                            }
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

        class VaultViewHolder extends RecyclerView.ViewHolder {
            ImageView ivThumb;
            TextView tvDuration, tvTitle, tvInfo;
            Button btnPlay, btnRestore, btnDelete;

            VaultViewHolder(@NonNull View v) {
                super(v);
                ivThumb = v.findViewById(R.id.iv_vault_thumb);
                tvDuration = v.findViewById(R.id.tv_vault_duration);
                tvTitle = v.findViewById(R.id.tv_vault_title);
                tvInfo = v.findViewById(R.id.tv_vault_info);
                btnPlay = v.findViewById(R.id.btn_vault_play);
                btnRestore = v.findViewById(R.id.btn_vault_restore);
                btnDelete = v.findViewById(R.id.btn_vault_delete);
            }
        }
    }

    private static String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private static String formatDate(long millis) {
        if (millis <= 0) return "Recently";
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
        return sdf.format(new Date(millis));
    }

    private static String formatDuration(long millis) {
        if (millis <= 0) return "00:00";
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = millis / (1000 * 60 * 60);
        if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }
}

