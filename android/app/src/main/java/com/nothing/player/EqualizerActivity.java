package com.nothing.player;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class EqualizerActivity extends AppCompatActivity {
    private TextView tvBassVal, tvPreampVal;
    private SeekBar seekbarBass, seekbarPreamp;
    private TextView chipPunch, chipBass, chipVocal, chipFlat;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_equalizer);

        ImageButton btnBack = findViewById(R.id.btn_back_eq);
        Button btnReset = findViewById(R.id.btn_reset_eq);
        tvBassVal = findViewById(R.id.tv_bass_boost_val);
        tvPreampVal = findViewById(R.id.tv_preamp_val);
        seekbarBass = findViewById(R.id.seekbar_bass_boost);
        seekbarPreamp = findViewById(R.id.seekbar_preamp);

        chipPunch = findViewById(R.id.preset_nothing_punch);
        chipBass = findViewById(R.id.preset_bass_boost);
        chipVocal = findViewById(R.id.preset_vocal);
        chipFlat = findViewById(R.id.preset_flat);

        btnBack.setOnClickListener(v -> finish());

        btnReset.setOnClickListener(v -> {
            seekbarBass.setProgress(6);
            seekbarPreamp.setProgress(12);
            selectPreset(chipFlat);
        });

        seekbarBass.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvBassVal.setText("+" + progress + " dB");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekbarPreamp.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int db = progress - 12;
                tvPreampVal.setText((db > 0 ? "+" : "") + db + " dB");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        chipPunch.setOnClickListener(v -> {
            selectPreset(chipPunch);
            seekbarBass.setProgress(8);
            seekbarPreamp.setProgress(14);
        });

        chipBass.setOnClickListener(v -> {
            selectPreset(chipBass);
            seekbarBass.setProgress(12);
            seekbarPreamp.setProgress(12);
        });

        chipVocal.setOnClickListener(v -> {
            selectPreset(chipVocal);
            seekbarBass.setProgress(2);
            seekbarPreamp.setProgress(15);
        });

        chipFlat.setOnClickListener(v -> {
            selectPreset(chipFlat);
            seekbarBass.setProgress(0);
            seekbarPreamp.setProgress(12);
        });
    }

    private void selectPreset(TextView selected) {
        TextView[] chips = {chipPunch, chipBass, chipVocal, chipFlat};
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
}
