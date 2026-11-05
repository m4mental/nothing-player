package com.nothing.player;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.Virtualizer;
import android.os.Build;
import android.util.Log;

public class AudioEffectManager {
    private static final String TAG = "NothingAudioFX";
    public static final String PREFS_NAME = "nothing_equalizer_prefs";
    public static final String KEY_ENABLED = "eq_enabled";
    public static final String KEY_BASS = "eq_bass";
    public static final String KEY_VIRTUALIZER = "eq_virtualizer";
    public static final String KEY_PREAMP = "eq_preamp";

    private static AudioEffectManager instance;

    private Equalizer equalizer;
    private BassBoost bassBoost;
    private Virtualizer virtualizer;
    private LoudnessEnhancer loudnessEnhancer;
    private DynamicsProcessing dynamicsProcessing;
    private boolean isNightModeCompressorEnabled = false;
    private int currentAudioSessionId = 0;

    public static synchronized AudioEffectManager getInstance() {
        if (instance == null) {
            instance = new AudioEffectManager();
        }
        return instance;
    }

    private AudioEffectManager() {}

    public synchronized void attachAudioSession(int sessionId, Context context) {
        if (sessionId <= 0) return;
        if (currentAudioSessionId == sessionId && equalizer != null) {
            applySettings(context);
            return;
        }

        release();
        this.currentAudioSessionId = sessionId;

        try {
            // Notify system audio HAL / Dirac
            if (context != null) {
                Intent intent = new Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION);
                intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId);
                intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
                intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                context.sendBroadcast(intent);
            }

            // Create AudioFX with high priority (1000)
            equalizer = new Equalizer(1000, sessionId);
            bassBoost = new BassBoost(1000, sessionId);
            virtualizer = new Virtualizer(1000, sessionId);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    loudnessEnhancer = new LoudnessEnhancer(sessionId);
                } catch (Exception e) {
                    Log.w(TAG, "LoudnessEnhancer not available", e);
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    DynamicsProcessing.Config.Builder dpBuilder = new DynamicsProcessing.Config.Builder(
                        DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                        2, true, 4, true, 4, true, 4, true
                    );
                    DynamicsProcessing.Limiter limiter = new DynamicsProcessing.Limiter(true, true, 0, 2.0f, 100.0f, 10.0f, -6.0f, 0.0f);
                    dpBuilder.setLimiterAllChannelsTo(limiter);
                    dynamicsProcessing = new DynamicsProcessing(1000, sessionId, dpBuilder.build());
                    dynamicsProcessing.setEnabled(isNightModeCompressorEnabled);
                } catch (Exception e) {
                    Log.w(TAG, "DynamicsProcessing initialization skipped", e);
                }
            }

            applySettings(context);
            Log.d(TAG, "Attached AudioFX to session " + sessionId + " bands: " + (equalizer != null ? equalizer.getNumberOfBands() : 0));
        } catch (Exception e) {
            Log.e(TAG, "Failed to attach AudioFX to session " + sessionId, e);
        }
    }

    public synchronized void applySettings(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean(KEY_ENABLED, true);

        // Apply Equalizer
        if (equalizer != null) {
            try {
                equalizer.setEnabled(isEnabled);
                if (isEnabled) {
                    short numBands = equalizer.getNumberOfBands();
                    short minLevel = equalizer.getBandLevelRange()[0];
                    short maxLevel = equalizer.getBandLevelRange()[1];

                    for (short i = 0; i < numBands; i++) {
                        int progress = prefs.getInt("eq_band_" + Math.min(i, (short) 4), 15);
                        // Convert progress (0-30, center 15) to millibels range (e.g. -1500 to +1500)
                        float normalized = (progress - 15) / 15.0f; // -1.0 to +1.0
                        short targetLevel;
                        if (normalized >= 0) {
                            targetLevel = (short) (normalized * maxLevel);
                        } else {
                            targetLevel = (short) (-normalized * minLevel);
                        }
                        targetLevel = (short) Math.max(minLevel, Math.min(maxLevel, targetLevel));
                        equalizer.setBandLevel(i, targetLevel);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "Error applying equalizer", e);
            }
        }

        // Apply Bass Boost
        if (bassBoost != null) {
            try {
                bassBoost.setEnabled(isEnabled);
                if (isEnabled && bassBoost.getStrengthSupported()) {
                    int bass = prefs.getInt(KEY_BASS, 60);
                    short strength = (short) Math.max(0, Math.min(1000, bass * 10)); // 0-1000
                    bassBoost.setStrength(strength);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error applying bass boost", e);
            }
        }

        // Apply 3D Virtualizer
        if (virtualizer != null) {
            try {
                virtualizer.setEnabled(isEnabled);
                if (isEnabled && virtualizer.getStrengthSupported()) {
                    int virt = prefs.getInt(KEY_VIRTUALIZER, 50);
                    short strength = (short) Math.max(0, Math.min(1000, virt * 10)); // 0-1000
                    virtualizer.setStrength(strength);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error applying virtualizer", e);
            }
        }

        // Apply Preamp Gain (via LoudnessEnhancer)
        if (loudnessEnhancer != null) {
            try {
                loudnessEnhancer.setEnabled(isEnabled);
                if (isEnabled) {
                    int preamp = prefs.getInt(KEY_PREAMP, 12);
                    // Preamp range: 0 (center 12) -> -12dB to +12dB
                    int db = preamp - 12;
                    int targetGainMb = Math.max(0, db * 100); // in millibels (0 to 1200 mB)
                    loudnessEnhancer.setTargetGain(targetGainMb);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error applying preamp gain", e);
            }
        }

        // Night Mode Compressor & Dialogue Normalizer
        this.isNightModeCompressorEnabled = prefs.getBoolean("night_mode_compressor", false);
        if (dynamicsProcessing != null) {
            try {
                dynamicsProcessing.setEnabled(this.isNightModeCompressorEnabled);
            } catch (Exception ignored) {}
        }
        if (this.isNightModeCompressorEnabled && loudnessEnhancer != null) {
            try {
                loudnessEnhancer.setEnabled(true);
                loudnessEnhancer.setTargetGain(600); // +6dB speech boost
            } catch (Exception ignored) {}
        }
    }

    public synchronized void applyVlcEqualizer(org.videolan.libvlc.MediaPlayer vlcPlayer, Context context) {
        if (vlcPlayer == null || context == null) return;
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            boolean isEnabled = prefs.getBoolean(KEY_ENABLED, true);
            if (!isEnabled) {
                vlcPlayer.setEqualizer(null);
                return;
            }
            org.videolan.libvlc.MediaPlayer.Equalizer vlcEq = org.videolan.libvlc.MediaPlayer.Equalizer.create();
            if (vlcEq != null) {
                int preamp = prefs.getInt(KEY_PREAMP, 12);
                vlcEq.setPreAmp((preamp - 12) * 1.5f);
                for (int i = 0; i < 5; i++) {
                    int level = prefs.getInt("eq_band_" + i, 15);
                    float amp = (level - 15) * 1.33f;
                    int bandA = i * 2;
                    int bandB = Math.min(9, i * 2 + 1);
                    vlcEq.setAmp(bandA, amp);
                    vlcEq.setAmp(bandB, amp);
                }
                vlcPlayer.setEqualizer(vlcEq);
            }
        } catch (Exception e) {
            Log.w(TAG, "Error applying VLC equalizer", e);
        }
    }

    public synchronized void applyDialogueBoosterPreset(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putBoolean(KEY_ENABLED, true)
            .putInt("eq_band_0", 11) // Cut deep sub-rumble for speech clarity
            .putInt("eq_band_1", 13)
            .putInt("eq_band_2", 23) // High boost on vocal fundamentals
            .putInt("eq_band_3", 22) // High clarity on consonants and dialogue
            .putInt("eq_band_4", 17)
            .putInt(KEY_BASS, 10)
            .putInt(KEY_VIRTUALIZER, 20)
            .apply();
        applySettings(context);
    }

    public synchronized void setNightModeCompressorEnabled(boolean enabled, Context context) {
        this.isNightModeCompressorEnabled = enabled;
        if (context != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean("night_mode_compressor", enabled).apply();
        }
        if (dynamicsProcessing != null) {
            try {
                dynamicsProcessing.setEnabled(enabled);
            } catch (Exception ignored) {}
        }
        if (loudnessEnhancer != null) {
            try {
                if (enabled) {
                    loudnessEnhancer.setEnabled(true);
                    loudnessEnhancer.setTargetGain(600); // +6dB dialogue boost
                } else {
                    loudnessEnhancer.setTargetGain(0);
                }
            } catch (Exception ignored) {}
        }
    }

    public synchronized boolean isNightModeCompressorEnabled() {
        return isNightModeCompressorEnabled;
    }

    public synchronized void setVolumeBoostGain(int boostPercent) {
        if (loudnessEnhancer != null) {
            try {
                if (boostPercent > 0) {
                    loudnessEnhancer.setEnabled(true);
                    int gainMb = Math.min(1800, boostPercent * 18);
                    loudnessEnhancer.setTargetGain(gainMb);
                } else {
                    loudnessEnhancer.setTargetGain(0);
                }
            } catch (Exception e) {
                Log.w(TAG, "Error setting volume boost gain", e);
            }
        }
    }

    public synchronized void release() {
        release(null);
    }

    public synchronized void release(Context context) {
        if (currentAudioSessionId > 0) {
            try {
                if (context != null) {
                    Intent intent = new Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
                    intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, currentAudioSessionId);
                    intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
                    context.sendBroadcast(intent);
                }
            } catch (Exception ignored) {}
            try {
                if (equalizer != null) { equalizer.release(); equalizer = null; }
                if (bassBoost != null) { bassBoost.release(); bassBoost = null; }
                if (virtualizer != null) { virtualizer.release(); virtualizer = null; }
                if (loudnessEnhancer != null) { loudnessEnhancer.release(); loudnessEnhancer = null; }
                if (dynamicsProcessing != null) { dynamicsProcessing.release(); dynamicsProcessing = null; }
            } catch (Exception ignored) {}
            currentAudioSessionId = 0;
        }
    }
}
