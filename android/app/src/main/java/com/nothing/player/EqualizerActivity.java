package com.nothing.player;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class EqualizerActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "nothing_equalizer_prefs";
    private static final String KEY_ENABLED = "eq_enabled";
    private static final String KEY_BASS = "eq_bass";
    private static final String KEY_VIRTUALIZER = "eq_virtualizer";
    private static final String KEY_PREAMP = "eq_preamp";

    private View eqPowerLed, eqControlsContainer;
    private androidx.appcompat.widget.SwitchCompat switchEqPower;
    private Button btnReset;
    private TextView tvBassVal, tvVirtualizerVal, tvPreampVal;
    private SeekBar seekbarBass, seekbarVirtualizer, seekbarPreamp;
    private TextView chipPunch, chipCinema, chipBass, chipEdm, chipRock, chipPop, chipVocal, chipFlat;

    private SeekBar[] bandSeekbars = new SeekBar[5];
    private TextView[] bandValTexts = new TextView[5];
    private boolean isEqEnabled = true;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        isEqEnabled = prefs.getBoolean(KEY_ENABLED, true);

        ImageButton btnBack = findViewById(R.id.btn_back_eq);
        btnReset = findViewById(R.id.btn_reset_eq);
        switchEqPower = findViewById(R.id.switch_eq_power);
        eqPowerLed = findViewById(R.id.eq_power_led);
        eqControlsContainer = findViewById(R.id.eq_controls_container);

        tvBassVal = findViewById(R.id.tv_bass_boost_val);
        tvVirtualizerVal = findViewById(R.id.tv_virtualizer_val);
        tvPreampVal = findViewById(R.id.tv_preamp_val);

        seekbarBass = findViewById(R.id.seekbar_bass_boost);
        seekbarVirtualizer = findViewById(R.id.seekbar_virtualizer);
        seekbarPreamp = findViewById(R.id.seekbar_preamp);

        chipPunch = findViewById(R.id.preset_nothing_punch);
        chipCinema = findViewById(R.id.preset_cinema);
        chipBass = findViewById(R.id.preset_bass_boost);
        chipEdm = findViewById(R.id.preset_edm);
        chipRock = findViewById(R.id.preset_rock);
        chipPop = findViewById(R.id.preset_pop);
        chipVocal = findViewById(R.id.preset_vocal);
        chipFlat = findViewById(R.id.preset_flat);

        bandSeekbars[0] = findViewById(R.id.seekbar_band_0);
        bandSeekbars[1] = findViewById(R.id.seekbar_band_1);
        bandSeekbars[2] = findViewById(R.id.seekbar_band_2);
        bandSeekbars[3] = findViewById(R.id.seekbar_band_3);
        bandSeekbars[4] = findViewById(R.id.seekbar_band_4);

        bandValTexts[0] = findViewById(R.id.tv_band_0_val);
        bandValTexts[1] = findViewById(R.id.tv_band_1_val);
        bandValTexts[2] = findViewById(R.id.tv_band_2_val);
        bandValTexts[3] = findViewById(R.id.tv_band_3_val);
        bandValTexts[4] = findViewById(R.id.tv_band_4_val);

        btnBack.setOnClickListener(v -> finish());

        // Setup Power Switch
        updatePowerUI();
        switchEqPower.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isEqEnabled = isChecked;
            prefs.edit().putBoolean(KEY_ENABLED, isEqEnabled).apply();
            updatePowerUI();
            applyEffects();
            Toast.makeText(this, isEqEnabled ? "Equalizer ON (DSP Active)" : "Equalizer OFF (DSP Bypass)", Toast.LENGTH_SHORT).show();
        });

        // Setup Reset
        btnReset.setOnClickListener(v -> {
            applyPresetLevels(new int[]{15, 15, 15, 15, 15});
            seekbarBass.setProgress(60);
            seekbarVirtualizer.setProgress(50);
            seekbarPreamp.setProgress(12);
            selectPreset(chipPunch);
        });

        // Setup 5 Frequency Bands
        for (int i = 0; i < 5; i++) {
            final int bandIndex = i;
            int savedLevel = prefs.getInt("eq_band_" + bandIndex, 15);
            bandSeekbars[bandIndex].setProgress(savedLevel);
            updateBandValText(bandIndex, savedLevel);

            bandSeekbars[bandIndex].setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    updateBandValText(bandIndex, progress);
                    if (fromUser) {
                        prefs.edit().putInt("eq_band_" + bandIndex, progress).apply();
                        applyEffects();
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // Setup Bass Boost
        int savedBass = prefs.getInt(KEY_BASS, 60);
        seekbarBass.setProgress(savedBass);
        tvBassVal.setText(savedBass + "%");
        seekbarBass.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvBassVal.setText(progress + "%");
                if (fromUser) {
                    prefs.edit().putInt(KEY_BASS, progress).apply();
                    applyEffects();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Setup 3D Virtualizer
        int savedVirt = prefs.getInt(KEY_VIRTUALIZER, 50);
        seekbarVirtualizer.setProgress(savedVirt);
        tvVirtualizerVal.setText(savedVirt + "%");
        seekbarVirtualizer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVirtualizerVal.setText(progress + "%");
                if (fromUser) {
                    prefs.edit().putInt(KEY_VIRTUALIZER, progress).apply();
                    applyEffects();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Setup Preamp Gain
        int savedPreamp = prefs.getInt(KEY_PREAMP, 12);
        seekbarPreamp.setProgress(savedPreamp);
        updatePreampText(savedPreamp);
        seekbarPreamp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePreampText(progress);
                if (fromUser) {
                    prefs.edit().putInt(KEY_PREAMP, progress).apply();
                    applyEffects();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Presets Listeners
        chipPunch.setOnClickListener(v -> {
            selectPreset(chipPunch);
            applyPresetLevels(new int[]{21, 18, 14, 18, 20});
            seekbarBass.setProgress(80);
            seekbarVirtualizer.setProgress(60);
        });

        chipCinema.setOnClickListener(v -> {
            selectPreset(chipCinema);
            applyPresetLevels(new int[]{24, 19, 17, 21, 23});
            seekbarBass.setProgress(90);
            seekbarVirtualizer.setProgress(100);
            seekbarPreamp.setProgress(14);
            Toast.makeText(this, "🎬 CINEMA: Dolby 3D Theater Sound Applied", Toast.LENGTH_SHORT).show();
        });

        chipBass.setOnClickListener(v -> {
            selectPreset(chipBass);
            applyPresetLevels(new int[]{25, 22, 16, 13, 14});
            seekbarBass.setProgress(100);
            seekbarVirtualizer.setProgress(40);
        });

        chipEdm.setOnClickListener(v -> {
            selectPreset(chipEdm);
            applyPresetLevels(new int[]{22, 19, 13, 19, 23});
            seekbarBass.setProgress(85);
            seekbarVirtualizer.setProgress(70);
        });

        chipRock.setOnClickListener(v -> {
            selectPreset(chipRock);
            applyPresetLevels(new int[]{20, 16, 18, 21, 22});
            seekbarBass.setProgress(70);
            seekbarVirtualizer.setProgress(50);
        });

        chipPop.setOnClickListener(v -> {
            selectPreset(chipPop);
            applyPresetLevels(new int[]{13, 17, 21, 18, 14});
            seekbarBass.setProgress(50);
            seekbarVirtualizer.setProgress(45);
        });

        chipVocal.setOnClickListener(v -> {
            selectPreset(chipVocal);
            applyPresetLevels(new int[]{11, 14, 23, 20, 15});
            seekbarBass.setProgress(30);
            seekbarVirtualizer.setProgress(30);
        });

        chipFlat.setOnClickListener(v -> {
            selectPreset(chipFlat);
            applyPresetLevels(new int[]{15, 15, 15, 15, 15});
            seekbarBass.setProgress(0);
            seekbarVirtualizer.setProgress(0);
        });

        initAudioEffects();
    }

    private void updateBandValText(int bandIndex, int progress) {
        int db = progress - 15;
        bandValTexts[bandIndex].setText((db > 0 ? "+" : "") + db + " dB");
    }

    private void updatePreampText(int progress) {
        int db = progress - 12;
        tvPreampVal.setText((db > 0 ? "+" : "") + db + " dB");
    }

    private void applyPresetLevels(int[] levels) {
        for (int i = 0; i < 5; i++) {
            bandSeekbars[i].setProgress(levels[i]);
            prefs.edit().putInt("eq_band_" + i, levels[i]).apply();
        }
        applyEffects();
    }

    private void updatePowerUI() {
        if (switchEqPower != null) {
            switchEqPower.setChecked(isEqEnabled);
            switchEqPower.setText(isEqEnabled ? "ON" : "OFF");
            switchEqPower.setTextColor(getResources().getColor(isEqEnabled ? R.color.nothing_white : R.color.nothing_white_40));
        }

        if (eqPowerLed != null) {
            eqPowerLed.setAlpha(isEqEnabled ? 1.0f : 0.15f);
        }

        if (eqControlsContainer != null) {
            eqControlsContainer.setAlpha(isEqEnabled ? 1.0f : 0.35f);
        }

        for (SeekBar sb : bandSeekbars) if (sb != null) sb.setEnabled(isEqEnabled);
        if (seekbarBass != null) seekbarBass.setEnabled(isEqEnabled);
        if (seekbarVirtualizer != null) seekbarVirtualizer.setEnabled(isEqEnabled);
        if (seekbarPreamp != null) seekbarPreamp.setEnabled(isEqEnabled);
        if (btnReset != null) btnReset.setEnabled(isEqEnabled);
    }

    private void selectPreset(TextView selected) {
        TextView[] chips = {chipPunch, chipCinema, chipBass, chipEdm, chipRock, chipPop, chipVocal, chipFlat};
        for (TextView c : chips) {
            if (c == selected) {
                c.setBackgroundResource(R.drawable.bg_chip_selected);
                c.setTextColor(getResources().getColor(R.color.nothing_black));
            } else {
                c.setBackgroundResource(R.drawable.bg_chip_unselected);
                c.setTextColor(getResources().getColor(R.color.nothing_white_70));
            }
        }
    }

    private void initAudioEffects() {
        if (MusicPlaybackService.instance != null && MusicPlaybackService.instance.getAudioSessionId() > 0) {
            AudioEffectManager.getInstance().attachAudioSession(MusicPlaybackService.instance.getAudioSessionId(), this);
        }
        applyEffects();
    }

    private void applyEffects() {
        AudioEffectManager.getInstance().applySettings(this);
    }
}
