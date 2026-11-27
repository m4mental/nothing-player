package com.nothing.player;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsFragment extends Fragment {

    private ActivityResultLauncher<String> exportBackupLauncher;
    private ActivityResultLauncher<String[]> importBackupLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        exportBackupLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/json"),
                uri -> {
                    if (uri == null || getContext() == null) return;
                    try {
                        int count = BackupManager.exportToUri(getContext(), uri);
                        Toast.makeText(getContext(), "BACKUP EXPORTED (" + count + " GROUPS)", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "EXPORT FAILED: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
        importBackupLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri == null || getContext() == null) return;
                    try {
                        int keys = BackupManager.importFromUri(getContext(), uri);
                        Toast.makeText(getContext(), "RESTORED " + keys + " SETTINGS & ITEMS", Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "IMPORT FAILED: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        if (getContext() == null) return view;

        final SharedPreferences prefs = getContext().getSharedPreferences("nothing_player_prefs", Context.MODE_PRIVATE);

        // 1. DSP Equalizer
        View cardEqualizer = view.findViewById(R.id.card_open_equalizer);
        if (cardEqualizer != null) {
            cardEqualizer.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), EqualizerActivity.class);
                startActivity(intent);
            });
        }

        // 2. Stereo Balance Fader
        final TextView tvAudioBalanceVal = view.findViewById(R.id.tv_audio_balance_val);
        SeekBar seekbarAudioBalance = view.findViewById(R.id.seekbar_audio_balance);
        int savedBalance = prefs.getInt("audio_balance_lr", 0); // -100 to +100
        updateBalanceLabel(tvAudioBalanceVal, savedBalance);
        if (seekbarAudioBalance != null) {
            seekbarAudioBalance.setProgress(savedBalance + 100);
            seekbarAudioBalance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int balance = progress - 100;
                    updateBalanceLabel(tvAudioBalanceVal, balance);
                    if (fromUser) prefs.edit().putInt("audio_balance_lr", balance).apply();
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // 3. Headset Auto-Pause & Resume
        SwitchCompat switchHeadsetPause = view.findViewById(R.id.switch_headset_auto_pause);
        SwitchCompat switchHeadsetResume = view.findViewById(R.id.switch_headset_auto_resume);
        if (switchHeadsetPause != null) {
            switchHeadsetPause.setChecked(prefs.getBoolean("headset_auto_pause", true));
            switchHeadsetPause.setOnCheckedChangeListener((btn, isChecked) -> {
                prefs.edit().putBoolean("headset_auto_pause", isChecked).apply();
            });
        }
        if (switchHeadsetResume != null) {
            switchHeadsetResume.setChecked(prefs.getBoolean("headset_auto_resume", false));
            switchHeadsetResume.setOnCheckedChangeListener((btn, isChecked) -> {
                prefs.edit().putBoolean("headset_auto_resume", isChecked).apply();
            });
        }

        // 4. HEVC 4K Decoder Routing
        View cardCodecHevc = view.findViewById(R.id.card_codec_hevc);
        final TextView tvCodecHevcVal = view.findViewById(R.id.tv_codec_hevc_val);
        String currentHevc = prefs.getString("pref_hevc_engine", "AUTO (RECOMMENDED)");
        if (tvCodecHevcVal != null) tvCodecHevcVal.setText(currentHevc);
        if (cardCodecHevc != null) {
            cardCodecHevc.setOnClickListener(v -> {
                List<String> options = Arrays.asList("AUTO (RECOMMENDED)", "FORCE HW+ (GPU)", "FORCE VLC (SOFTWARE)");
                int sel = Math.max(0, options.indexOf(prefs.getString("pref_hevc_engine", "AUTO (RECOMMENDED)")));
                NothingDialogHelper.showSelectionDialog(requireActivity(), "HEVC / H.265 DECODER", options, sel, (which, item) -> {
                    prefs.edit().putString("pref_hevc_engine", item).apply();
                    if (tvCodecHevcVal != null) tvCodecHevcVal.setText(item);
                });
            });
        }

        // 5. Dynamic Refresh Rate Matching
        SwitchCompat switchMatchRefresh = view.findViewById(R.id.switch_match_refresh_rate);
        if (switchMatchRefresh != null) {
            switchMatchRefresh.setChecked(prefs.getBoolean("match_display_refresh_rate", false));
            switchMatchRefresh.setOnCheckedChangeListener((btn, isChecked) -> {
                prefs.edit().putBoolean("match_display_refresh_rate", isChecked).apply();
            });
        }

        // 9. Double-Tap Seek Step
        View cardDoubleTapStep = view.findViewById(R.id.card_double_tap_step);
        final TextView tvDoubleTapStepVal = view.findViewById(R.id.tv_double_tap_step_val);
        int currentStepSec = prefs.getInt("double_tap_seek_step_sec", 10);
        if (tvDoubleTapStepVal != null) tvDoubleTapStepVal.setText(currentStepSec + " SECONDS");
        if (cardDoubleTapStep != null) {
            cardDoubleTapStep.setOnClickListener(v -> {
                List<String> steps = Arrays.asList("5 SECONDS", "10 SECONDS", "15 SECONDS", "30 SECONDS", "60 SECONDS");
                int currentSec = prefs.getInt("double_tap_seek_step_sec", 10);
                int sel = steps.indexOf(currentSec + " SECONDS");
                NothingDialogHelper.showSelectionDialog(requireActivity(), "DOUBLE-TAP SEEK STEP", steps, sel, (which, item) -> {
                    int sec = 10;
                    try {
                        sec = Integer.parseInt(item.replace(" SECONDS", "").trim());
                    } catch (Exception ignored) {}
                    prefs.edit().putInt("double_tap_seek_step_sec", sec).apply();
                    if (tvDoubleTapStepVal != null) tvDoubleTapStepVal.setText(item);
                });
            });
        }

        // 10. Long-Press Turbo Multiplier
        View cardLongPressMultiplier = view.findViewById(R.id.card_long_press_multiplier);
        final TextView tvLongPressMultiplierVal = view.findViewById(R.id.tv_long_press_multiplier_val);
        float currentMultiplier = prefs.getFloat("long_press_speed_target", 2.0f);
        if (tvLongPressMultiplierVal != null) tvLongPressMultiplierVal.setText(String.format(java.util.Locale.US, "%.1fX SPEED", currentMultiplier));
        if (cardLongPressMultiplier != null) {
            cardLongPressMultiplier.setOnClickListener(v -> {
                List<String> multipliers = Arrays.asList("1.5X SPEED", "2.0X SPEED", "2.5X SPEED", "3.0X SPEED", "4.0X SPEED");
                float cur = prefs.getFloat("long_press_speed_target", 2.0f);
                int sel = multipliers.indexOf(String.format(java.util.Locale.US, "%.1fX SPEED", cur));
                NothingDialogHelper.showSelectionDialog(requireActivity(), "TURBO BOOST SPEED", multipliers, sel, (which, item) -> {
                    float spd = 2.0f;
                    try {
                        spd = Float.parseFloat(item.replace("X SPEED", "").trim());
                    } catch (Exception ignored) {}
                    prefs.edit().putFloat("long_press_speed_target", spd).apply();
                    if (tvLongPressMultiplierVal != null) tvLongPressMultiplierVal.setText(item);
                });
            });
        }

        // 11. Max Pinch-Zoom Limit
        View cardMaxZoom = view.findViewById(R.id.card_max_zoom);
        final TextView tvMaxZoomVal = view.findViewById(R.id.tv_max_zoom_val);
        float currentZoom = prefs.getFloat("max_zoom_limit", 4.0f);
        if (tvMaxZoomVal != null) tvMaxZoomVal.setText(((int) (currentZoom * 100)) + "% ZOOM");
        if (cardMaxZoom != null) {
            cardMaxZoom.setOnClickListener(v -> {
                List<String> zoomOptions = Arrays.asList("200% ZOOM", "400% ZOOM", "800% ZOOM (MICROSCOPE)");
                float curZ = prefs.getFloat("max_zoom_limit", 4.0f);
                int sel = curZ >= 7.5f ? 2 : curZ >= 3.5f ? 1 : 0;
                NothingDialogHelper.showSelectionDialog(requireActivity(), "MAX PINCH ZOOM LIMIT", zoomOptions, sel, (which, item) -> {
                    float val = which == 0 ? 2.0f : which == 1 ? 4.0f : 8.0f;
                    prefs.edit().putFloat("max_zoom_limit", val).apply();
                    if (tvMaxZoomVal != null) tvMaxZoomVal.setText(item);
                });
            });
        }

        // Pocket & Screen-Off Hardware Keys Track Skip
        androidx.appcompat.widget.SwitchCompat switchPocketKeys = view.findViewById(R.id.switch_pocket_hardware_keys);
        if (switchPocketKeys != null) {
            switchPocketKeys.setChecked(prefs.getBoolean("screen_off_volume_skip", true));
            switchPocketKeys.setOnCheckedChangeListener((btn, isChecked) -> {
                prefs.edit().putBoolean("screen_off_volume_skip", isChecked).apply();
            });
        }

        // Auto Subtitle Guard in Zoom & Ultrawide
        androidx.appcompat.widget.SwitchCompat switchSubGuard = view.findViewById(R.id.switch_settings_subtitle_guard);
        if (switchSubGuard != null) {
            switchSubGuard.setChecked(prefs.getBoolean("pref_subtitle_guard", true));
            switchSubGuard.setOnCheckedChangeListener((btn, isChecked) -> {
                prefs.edit().putBoolean("pref_subtitle_guard", isChecked).apply();
            });
        }

        // 14. Signature Accent Palette Studio
        View cardAccentColor = view.findViewById(R.id.card_accent_color);
        final TextView tvAccentColorVal = view.findViewById(R.id.tv_accent_color_val);
        List<String> palettes = Arrays.asList("NOTHING RED (#D71921)", "NDOT PURE WHITE (#FFFFFF)", "GLYPH STEALTH GREY (#666666)");
        String savedAccentHex = prefs.getString("accent_color_hex", "#D71921");
        int initialAccentIdx = "#FFFFFF".equalsIgnoreCase(savedAccentHex) ? 1 : "#666666".equalsIgnoreCase(savedAccentHex) ? 2 : 0;
        if (tvAccentColorVal != null) tvAccentColorVal.setText(palettes.get(initialAccentIdx));
        if (cardAccentColor != null) {
            cardAccentColor.setOnClickListener(v -> {
                String curHex = prefs.getString("accent_color_hex", "#D71921");
                int sel = "#FFFFFF".equalsIgnoreCase(curHex) ? 1 : "#666666".equalsIgnoreCase(curHex) ? 2 : 0;
                NothingDialogHelper.showSelectionDialog(requireActivity(), "ACCENT PALETTE", palettes, sel, (which, item) -> {
                    String hex = which == 1 ? "#FFFFFF" : which == 2 ? "#666666" : "#D71921";
                    prefs.edit()
                        .putString("accent_color_name", item)
                        .putString("accent_color_hex", hex)
                        .apply();
                    if (tvAccentColorVal != null) tvAccentColorVal.setText(item);
                    Toast.makeText(getContext(), "Accent applied: " + item, Toast.LENGTH_SHORT).show();
                });
            });
        }

        // 15. Browser Card Density (synced with VideosFragment pref_videos_view_mode)
        View cardBrowserDensity = view.findViewById(R.id.card_browser_density);
        final TextView tvBrowserDensityVal = view.findViewById(R.id.tv_browser_density_val);
        List<String> densities = Arrays.asList("2-COLUMN GRID", "3-COLUMN COMPACT", "DETAILED LIST");
        int savedViewMode = prefs.getInt("pref_videos_view_mode", VideoAdapter.VIEW_MODE_GRID_2);
        int initialDensityIdx = (savedViewMode == VideoAdapter.VIEW_MODE_GRID_3) ? 1 : (savedViewMode == VideoAdapter.VIEW_MODE_LIST) ? 2 : 0;
        if (tvBrowserDensityVal != null) tvBrowserDensityVal.setText(densities.get(initialDensityIdx));
        if (cardBrowserDensity != null) {
            cardBrowserDensity.setOnClickListener(v -> {
                int curMode = prefs.getInt("pref_videos_view_mode", VideoAdapter.VIEW_MODE_GRID_2);
                int sel = (curMode == VideoAdapter.VIEW_MODE_GRID_3) ? 1 : (curMode == VideoAdapter.VIEW_MODE_LIST) ? 2 : 0;
                NothingDialogHelper.showSelectionDialog(requireActivity(), "BROWSER DENSITY", densities, sel, (which, item) -> {
                    int mode = (which == 1) ? VideoAdapter.VIEW_MODE_GRID_3 : (which == 2) ? VideoAdapter.VIEW_MODE_LIST : VideoAdapter.VIEW_MODE_GRID_2;
                    prefs.edit()
                        .putString("browser_density_label", item)
                        .putInt("pref_videos_view_mode", mode)
                        .apply();
                    if (tvBrowserDensityVal != null) tvBrowserDensityVal.setText(item);
                });
            });
        }

        // 17. AMOLED Pure-Black Filter
        SwitchCompat switchAmoled = view.findViewById(R.id.switch_amoled_pure_black);
        if (switchAmoled != null) {
            switchAmoled.setChecked(prefs.getBoolean("pref_amoled_pure_black", false));
            switchAmoled.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean("pref_amoled_pure_black", isChecked).apply());
        }

        // 18. Warm Eye Comfort Shield
        SwitchCompat switchEyeComfort = view.findViewById(R.id.switch_eye_comfort);
        if (switchEyeComfort != null) {
            switchEyeComfort.setChecked(prefs.getBoolean("pref_eye_comfort", false));
            switchEyeComfort.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean("pref_eye_comfort", isChecked).apply());
        }

        // 19. RAM Buffer Cache Customizer
        View cardBuffer = view.findViewById(R.id.card_buffer_size_mode);
        final TextView tvBufferVal = view.findViewById(R.id.tv_buffer_size_mode_val);
        String savedBuffer = prefs.getString("pref_buffer_size_mode", "BALANCED (25MB)");
        if (tvBufferVal != null) tvBufferVal.setText(formatBufferShortLabel(savedBuffer));
        if (cardBuffer != null) {
            cardBuffer.setOnClickListener(v -> {
                List<String> options = Arrays.asList("ULTRA-LOW (5MB - 0MS SEEK)", "BALANCED (25MB)", "HEAVY (100MB - ANTI-STUTTER)");
                int sel = Math.max(0, options.indexOf(prefs.getString("pref_buffer_size_mode", "BALANCED (25MB)")));
                NothingDialogHelper.showSelectionDialog(requireActivity(), "RAM BUFFER CACHE SIZE", options, sel, (which, item) -> {
                    prefs.edit().putString("pref_buffer_size_mode", item).apply();
                    if (tvBufferVal != null) tvBufferVal.setText(formatBufferShortLabel(item));
                });
            });
        }

        // 20. ReplayGain Loudness Auto-Leveler
        SwitchCompat switchReplayGain = view.findViewById(R.id.switch_replay_gain_leveler);
        if (switchReplayGain != null) {
            switchReplayGain.setChecked(prefs.getBoolean("pref_replay_gain_leveler", true));
            switchReplayGain.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean("pref_replay_gain_leveler", isChecked).apply());
        }

        // 21. DJ Crossfade Blending
        View cardCrossfade = view.findViewById(R.id.card_crossfade_sec);
        final TextView tvCrossfadeVal = view.findViewById(R.id.tv_crossfade_sec_val);
        String currentCrossfade = prefs.getString("pref_crossfade_sec", "OFF (0S)");
        if (tvCrossfadeVal != null) tvCrossfadeVal.setText(currentCrossfade);
        if (cardCrossfade != null) {
            cardCrossfade.setOnClickListener(v -> {
                List<String> options = Arrays.asList("OFF (0S)", "2 SECONDS", "4 SECONDS", "6 SECONDS");
                int sel = options.indexOf(prefs.getString("pref_crossfade_sec", "OFF (0S)"));
                NothingDialogHelper.showSelectionDialog(requireActivity(), "DJ CROSSFADE DURATION", options, sel, (which, item) -> {
                    prefs.edit().putString("pref_crossfade_sec", item).apply();
                    if (tvCrossfadeVal != null) tvCrossfadeVal.setText(item);
                });
            });
        }

        // 22. Bit-Perfect USB DAC Passthrough
        SwitchCompat switchDac = view.findViewById(R.id.switch_dac_passthrough);
        if (switchDac != null) {
            switchDac.setChecked(prefs.getBoolean("pref_dac_passthrough", false));
            switchDac.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean("pref_dac_passthrough", isChecked).apply());
        }

        // 23. Hearing-Impaired (SDH) Cleaner
        SwitchCompat switchSdh = view.findViewById(R.id.switch_sub_clean_sdh);
        if (switchSdh != null) {
            switchSdh.setChecked(prefs.getBoolean("pref_sub_clean_sdh", false));
            switchSdh.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean("pref_sub_clean_sdh", isChecked).apply());
        }

        // 24. Edge Palm Rejection Deadzone
        View cardPalm = view.findViewById(R.id.card_palm_rejection);
        final TextView tvPalmVal = view.findViewById(R.id.tv_palm_rejection_val);
        String currentPalm = prefs.getString("pref_palm_rejection_label", "OFF (0DP)");
        if (tvPalmVal != null) tvPalmVal.setText(currentPalm);
        if (cardPalm != null) {
            cardPalm.setOnClickListener(v -> {
                List<String> options = Arrays.asList("OFF (0DP)", "16DP (STANDARD)", "24DP (CURVED SCREEN GUARD)");
                int sel = options.indexOf(prefs.getString("pref_palm_rejection_label", "OFF (0DP)"));
                NothingDialogHelper.showSelectionDialog(requireActivity(), "PALM REJECTION DEADZONE", options, sel, (which, item) -> {
                    int dp = which == 1 ? 16 : which == 2 ? 24 : 0;
                    prefs.edit()
                        .putString("pref_palm_rejection_label", item)
                        .putInt("pref_palm_rejection_dp", dp)
                        .apply();
                    if (tvPalmVal != null) tvPalmVal.setText(item);
                });
            });
        }

        // 25. Swipe Travel Sensitivity
        View cardSens = view.findViewById(R.id.card_gesture_sensitivity);
        final TextView tvSensVal = view.findViewById(R.id.tv_gesture_sensitivity_val);
        String currentSens = prefs.getString("pref_gesture_sensitivity", "NORMAL (1.0X)");
        if (tvSensVal != null) tvSensVal.setText(currentSens);
        if (cardSens != null) {
            cardSens.setOnClickListener(v -> {
                List<String> options = Arrays.asList("LOW (0.5X - ACCURATE)", "NORMAL (1.0X)", "HIGH (1.5X - FAST)");
                int sel = options.indexOf(prefs.getString("pref_gesture_sensitivity", "NORMAL (1.0X)"));
                NothingDialogHelper.showSelectionDialog(requireActivity(), "GESTURE SENSITIVITY", options, sel, (which, item) -> {
                    prefs.edit().putString("pref_gesture_sensitivity", item).apply();
                    if (tvSensVal != null) tvSensVal.setText(item);
                });
            });
        }

        // 26. Scrubbing Precision Mode
        View cardScrub = view.findViewById(R.id.card_scrubbing_mode);
        final TextView tvScrubVal = view.findViewById(R.id.tv_scrubbing_mode_val);
        String savedScrub = prefs.getString("pref_scrubbing_mode", "FAST KEYFRAME (0MS - INSTANT)");
        if (tvScrubVal != null) tvScrubVal.setText(formatScrubShortLabel(savedScrub));
        if (cardScrub != null) {
            cardScrub.setOnClickListener(v -> {
                List<String> options = Arrays.asList("FAST KEYFRAME (0MS - INSTANT)", "EXACT FRAME SYNC (ACCURATE)");
                int sel = Math.max(0, options.indexOf(prefs.getString("pref_scrubbing_mode", "FAST KEYFRAME (0MS - INSTANT)")));
                NothingDialogHelper.showSelectionDialog(requireActivity(), "SCRUBBING PRECISION", options, sel, (which, item) -> {
                    prefs.edit().putString("pref_scrubbing_mode", item).apply();
                    if (tvScrubVal != null) tvScrubVal.setText(formatScrubShortLabel(item));
                });
            });
        }

        // 27. Video-Adaptive Auto-Rotation
        SwitchCompat switchAutoRotate = view.findViewById(R.id.switch_auto_rotate_by_aspect);
        if (switchAutoRotate != null) {
            switchAutoRotate.setChecked(prefs.getBoolean("pref_auto_rotate_by_aspect", false));
            switchAutoRotate.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean("pref_auto_rotate_by_aspect", isChecked).apply());
        }

        // 28. Auto-PiP on Home Gesture
        SwitchCompat switchAutoPip = view.findViewById(R.id.switch_auto_pip_home);
        if (switchAutoPip != null) {
            switchAutoPip.setChecked(prefs.getBoolean("pref_auto_pip_home", true));
            switchAutoPip.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean("pref_auto_pip_home", isChecked).apply());
        }

        // 29. Smart Resume Policy
        View cardResume = view.findViewById(R.id.card_resume_policy);
        final TextView tvResumeVal = view.findViewById(R.id.tv_resume_policy_val);
        String currentResume = prefs.getString("pref_resume_policy", "ALWAYS RESUME");
        if (tvResumeVal != null) tvResumeVal.setText(currentResume);
        if (cardResume != null) {
            cardResume.setOnClickListener(v -> {
                List<String> options = Arrays.asList("ALWAYS RESUME", "DISCARD IF < 2 MIN REMAINING", "NEVER (ALWAYS FROM START)");
                int sel = options.indexOf(prefs.getString("pref_resume_policy", "ALWAYS RESUME"));
                NothingDialogHelper.showSelectionDialog(requireActivity(), "SMART RESUME POLICY", options, sel, (which, item) -> {
                    prefs.edit().putString("pref_resume_policy", item).apply();
                    if (tvResumeVal != null) tvResumeVal.setText(item);
                });
            });
        }

        // 30. Ultra-Eco 30FPS Battery Saver
        SwitchCompat switchEco = view.findViewById(R.id.switch_eco_battery_saver);
        if (switchEco != null) {
            switchEco.setChecked(prefs.getBoolean("pref_eco_battery_saver", false));
            switchEco.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean("pref_eco_battery_saver", isChecked).apply());
        }

        // 31. AMOLED Pixel Shifter (Burn-In Guard)
        SwitchCompat switchPixelShift = view.findViewById(R.id.switch_amoled_pixel_shift);
        if (switchPixelShift != null) {
            switchPixelShift.setChecked(prefs.getBoolean("pref_amoled_pixel_shift", true));
            switchPixelShift.setOnCheckedChangeListener((btn, isChecked) -> prefs.edit().putBoolean("pref_amoled_pixel_shift", isChecked).apply());
        }

        // 32. Vault Auto-Lock Timeout
        View cardVaultLock = view.findViewById(R.id.card_vault_autolock);
        final TextView tvVaultLockVal = view.findViewById(R.id.tv_vault_autolock_val);
        List<String> lockLabels = Arrays.asList("IMMEDIATE (0S)", "30 SECONDS", "2 MINUTES", "NEVER (OFF)");
        int[] lockSecs = {0, 30, 120, -1};
        int curSec = VaultManager.getAutoLockSeconds(getContext());
        int initLockIdx = 1;
        for (int i = 0; i < lockSecs.length; i++) {
            if (lockSecs[i] == curSec) { initLockIdx = i; break; }
        }
        if (tvVaultLockVal != null) tvVaultLockVal.setText(lockLabels.get(initLockIdx));
        if (cardVaultLock != null) {
            cardVaultLock.setOnClickListener(v -> {
                int nowSec = VaultManager.getAutoLockSeconds(getContext());
                int sel = 1;
                for (int i = 0; i < lockSecs.length; i++) {
                    if (lockSecs[i] == nowSec) { sel = i; break; }
                }
                NothingDialogHelper.showSelectionDialog(requireActivity(), "VAULT AUTO-LOCK TIMEOUT", lockLabels, sel, (which, item) -> {
                    VaultManager.setAutoLockSeconds(getContext(), lockSecs[which]);
                    if (tvVaultLockVal != null) tvVaultLockVal.setText(item);
                });
            });
        }

        // 33. JSON Backup Export & Import
        Button btnExport = view.findViewById(R.id.btn_export_backup_json);
        if (btnExport != null) {
            btnExport.setOnClickListener(v -> {
                String stamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(new Date());
                exportBackupLauncher.launch("NothingPlayer_Backup_" + stamp + ".json");
            });
        }
        Button btnImport = view.findViewById(R.id.btn_import_backup_json);
        if (btnImport != null) {
            btnImport.setOnClickListener(v -> importBackupLauncher.launch(new String[]{"application/json", "*/*"}));
        }

        return view;
    }

    private void updateBalanceLabel(TextView tv, int balance) {
        if (tv == null) return;
        if (balance == 0) {
            tv.setText("CENTER (0)");
        } else if (balance < 0) {
            tv.setText("LEFT " + Math.abs(balance) + "%");
        } else {
            tv.setText("RIGHT " + balance + "%");
        }
    }

    private String formatBufferShortLabel(String value) {
        if (value == null) return "BALANCED";
        if (value.contains("ULTRA-LOW")) return "ULTRA-LOW";
        if (value.contains("HEAVY")) return "HEAVY (100MB)";
        return "BALANCED";
    }

    private String formatScrubShortLabel(String value) {
        if (value != null && value.contains("EXACT")) return "EXACT FRAME";
        return "FAST KEYFRAME";
    }
}
