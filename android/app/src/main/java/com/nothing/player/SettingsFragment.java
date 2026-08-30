package com.nothing.player;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {
    private TextView tvAudioBoostVal;
    private SeekBar seekbarAudioBoost;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        View cardEqualizer = view.findViewById(R.id.card_open_equalizer);
        tvAudioBoostVal = view.findViewById(R.id.tv_audio_boost_val);
        seekbarAudioBoost = view.findViewById(R.id.seekbar_audio_boost);

        cardEqualizer.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), EqualizerActivity.class);
            startActivity(intent);
        });

        seekbarAudioBoost.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int boost = 100 + progress;
                tvAudioBoostVal.setText(boost + "%");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        return view;
    }
}
