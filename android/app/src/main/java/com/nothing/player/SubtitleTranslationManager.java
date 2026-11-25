package com.nothing.player;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * On-Device Neural Machine Translation Engine for Video Subtitles.
 * Powered by Google ML Kit Translate. Translates entire subtitle tracks (.srt)
 * offline, generates translated .srt files, and seamlessly swaps/replaces
 * active subtitle tracks during playback without interrupting the stream.
 */
public class SubtitleTranslationManager {
    private static final String TAG = "SubtitleTranslator";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public static final Map<String, String> SUPPORTED_LANGUAGES = new LinkedHashMap<>();

    static {
        SUPPORTED_LANGUAGES.put(TranslateLanguage.HINDI, "HINDI (हिंदी)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.SPANISH, "SPANISH (ESPAÑOL)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.FRENCH, "FRENCH (FRANÇAIS)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.GERMAN, "GERMAN (DEUTSCH)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.JAPANESE, "JAPANESE (日本語)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.BENGALI, "BENGALI (বাংলা)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.MARATHI, "MARATHI (मराठी)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.TAMIL, "TAMIL (தமிழ்)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.TELUGU, "TELUGU (తెలుగు)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.URDU, "URDU (اردو)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.GUJARATI, "GUJARATI (ગુજરાતી)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.KANNADA, "KANNADA (ಕನ್ನಡ)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.RUSSIAN, "RUSSIAN (РУССКИЙ)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.KOREAN, "KOREAN (한국어)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.ARABIC, "ARABIC (العربية)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.CHINESE, "CHINESE (中文)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.ITALIAN, "ITALIAN (ITALIANO)");
        SUPPORTED_LANGUAGES.put(TranslateLanguage.PORTUGUESE, "PORTUGUÊS");
    }

    public interface TranslationProgressCallback {
        void onStatus(String status);
        void onProgress(int current, int total, int percent, String sourceText, String translatedText);
        void onCompleted(File srtFile, List<SubtitleParser.SubtitleCue> translatedCues, boolean replaceOriginal);
        void onError(String message);
    }

    public interface SubtitleActionCallback {
        void onSubtitleReady(File srtFile, List<SubtitleParser.SubtitleCue> cues, boolean replaceOriginal);
    }

    /**
     * Checks if the on-device ML model is already downloaded.
     */
    public static void checkModelStatus(String targetLang, OnModelStatusCheckedListener listener) {
        TranslateRemoteModel model = new TranslateRemoteModel.Builder(targetLang).build();
        RemoteModelManager.getInstance().isModelDownloaded(model)
                .addOnSuccessListener(isDownloaded -> {
                    if (listener != null) listener.onStatusChecked(Boolean.TRUE.equals(isDownloaded));
                })
                .addOnFailureListener(e -> {
                    if (listener != null) listener.onStatusChecked(false);
                });
    }

    public interface OnModelStatusCheckedListener {
        void onStatusChecked(boolean isDownloaded);
    }

    /**
     * Translates a list of subtitle cues and writes a translated .srt file.
     */
    public static void translateSubtitleCues(
            Context context,
            String videoPathOrTitle,
            List<SubtitleParser.SubtitleCue> cues,
            String sourceLang,
            String targetLang,
            boolean replaceOriginal,
            TranslationProgressCallback callback
    ) {
        if (cues == null || cues.isEmpty()) {
            if (callback != null) callback.onError("No subtitle dialogues found to translate.");
            return;
        }

        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(sourceLang != null ? sourceLang : TranslateLanguage.ENGLISH)
                .setTargetLanguage(targetLang != null ? targetLang : TranslateLanguage.HINDI)
                .build();
        Translator translator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder().build();

        if (callback != null) callback.onStatus("PREPARING NEURAL MODEL (~30MB)...");

        translator.downloadModelIfNeeded(conditions)
                .addOnSuccessListener(unused -> {
                    EXECUTOR.execute(() -> {
                        try {
                            List<SubtitleParser.SubtitleCue> translatedCues = new ArrayList<>();
                            Map<String, String> cache = new HashMap<>();
                            int total = cues.size();

                            for (int i = 0; i < total; i++) {
                                SubtitleParser.SubtitleCue cue = cues.get(i);
                                String originalText = cue.text != null ? cue.text.trim() : "";

                                if (originalText.isEmpty()) {
                                    translatedCues.add(new SubtitleParser.SubtitleCue(cue.startTimeMs, cue.endTimeMs, ""));
                                    continue;
                                }

                                String translatedText;
                                if (cache.containsKey(originalText)) {
                                    translatedText = cache.get(originalText);
                                } else {
                                    try {
                                        translatedText = Tasks.await(translator.translate(originalText), 15, TimeUnit.SECONDS);
                                        cache.put(originalText, translatedText);
                                    } catch (Exception te) {
                                        translatedText = originalText; // fallback to original on timeout
                                    }
                                }

                                translatedCues.add(new SubtitleParser.SubtitleCue(cue.startTimeMs, cue.endTimeMs, translatedText));

                                final int currentIdx = i + 1;
                                final int percent = (int) ((currentIdx * 100f) / total);
                                final String src = originalText;
                                final String tgt = translatedText;

                                MAIN_HANDLER.post(() -> {
                                    if (callback != null) {
                                        callback.onProgress(currentIdx, total, percent, src, tgt);
                                    }
                                });
                            }

                            // Save translated cues to .srt file
                            File outFile = resolveOutputFile(context, videoPathOrTitle, targetLang);
                            boolean written = SubtitleParser.writeSrtFile(outFile, translatedCues);

                            translator.close();

                            MAIN_HANDLER.post(() -> {
                                if (written && outFile.exists()) {
                                    if (callback != null) {
                                        callback.onCompleted(outFile, translatedCues, replaceOriginal);
                                    }
                                } else {
                                    if (callback != null) {
                                        callback.onError("Failed to write translated .srt file to storage.");
                                    }
                                }
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "Translation execution failed", e);
                            translator.close();
                            MAIN_HANDLER.post(() -> {
                                if (callback != null) {
                                    callback.onError("Translation error: " + e.getMessage());
                                }
                            });
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    translator.close();
                    if (callback != null) {
                        callback.onError("Failed to download neural translation model: " + e.getMessage());
                    }
                });
    }

    private static File resolveOutputFile(Context context, String videoPathOrTitle, String targetLang) {
        String baseName = "video_translated";
        if (videoPathOrTitle != null && !videoPathOrTitle.trim().isEmpty()) {
            File vFile = new File(videoPathOrTitle);
            String name = vFile.getName();
            int dot = name.lastIndexOf('.');
            if (dot > 0) {
                baseName = name.substring(0, dot);
            } else {
                baseName = name;
            }

            // Try saving in the same directory as the video file
            File parentDir = vFile.getParentFile();
            if (parentDir != null && parentDir.canWrite()) {
                File candidate = new File(parentDir, baseName + "." + targetLang + ".srt");
                try {
                    if (candidate.createNewFile() || candidate.canWrite()) {
                        return candidate;
                    }
                } catch (Exception ignored) {}
            }
        }

        // Fallback: App external subtitles directory
        File dir = context.getExternalFilesDir("subtitles");
        if (dir == null || (!dir.exists() && !dir.mkdirs())) {
            dir = new File(context.getCacheDir(), "subtitles");
            if (!dir.exists()) dir.mkdirs();
        }
        return new File(dir, baseName + "." + targetLang + ".srt");
    }

    /**
     * Displays the interactive Nothing OS NDOT Dot-Matrix translation dialog.
     */
    public static void showTranslationDialog(
            Activity activity,
            String videoPathOrTitle,
            List<SubtitleParser.SubtitleCue> existingCues,
            SubtitleActionCallback actionCallback
    ) {
        if (activity == null || activity.isFinishing()) return;

        if (existingCues == null || existingCues.isEmpty()) {
            Toast.makeText(activity, "No active subtitles loaded to translate.", Toast.LENGTH_LONG).show();
            return;
        }

        Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_subtitle_translation);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.90f),
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView tvSourceInfo = dialog.findViewById(R.id.tv_source_subtitle_info);
        TextView tvModelBadge = dialog.findViewById(R.id.tv_model_status_badge);
        LinearLayout layoutChips = dialog.findViewById(R.id.layout_language_chips);
        Button btnModeReplace = dialog.findViewById(R.id.btn_mode_replace);
        Button btnModeDual = dialog.findViewById(R.id.btn_mode_dual);
        LinearLayout layoutProgress = dialog.findViewById(R.id.layout_translation_progress);
        TextView tvProgressStatus = dialog.findViewById(R.id.tv_progress_status);
        ProgressBar pbProgress = dialog.findViewById(R.id.pb_translation);
        TextView tvSourcePreview = dialog.findViewById(R.id.tv_live_source_preview);
        TextView tvTargetPreview = dialog.findViewById(R.id.tv_live_translated_preview);
        Button btnCancel = dialog.findViewById(R.id.btn_trans_cancel);
        Button btnStart = dialog.findViewById(R.id.btn_trans_start);

        final String[] selectedTargetLang = {TranslateLanguage.HINDI};
        final boolean[] replaceOriginal = {true};

        if (tvSourceInfo != null) {
            tvSourceInfo.setText(String.format(Locale.US, "SOURCE: ENGLISH (%d DIALOGUES LOADED)", existingCues.size()));
        }

        // Setup Mode Toggles
        btnModeReplace.setOnClickListener(v -> {
            replaceOriginal[0] = true;
            btnModeReplace.setBackgroundResource(R.drawable.bg_chip_red_active);
            btnModeReplace.setTextColor(Color.WHITE);
            btnModeDual.setBackgroundResource(R.drawable.bg_chip_unselected);
            btnModeDual.setTextColor(Color.parseColor("#88FFFFFF"));
        });

        btnModeDual.setOnClickListener(v -> {
            replaceOriginal[0] = false;
            btnModeDual.setBackgroundResource(R.drawable.bg_chip_red_active);
            btnModeDual.setTextColor(Color.WHITE);
            btnModeReplace.setBackgroundResource(R.drawable.bg_chip_unselected);
            btnModeReplace.setTextColor(Color.parseColor("#88FFFFFF"));
        });

        // Populate Language Chips
        final List<Button> chipButtons = new ArrayList<>();
        Runnable updateModelBadge = () -> {
            checkModelStatus(selectedTargetLang[0], isDownloaded -> {
                activity.runOnUiThread(() -> {
                    if (tvModelBadge != null) {
                        if (isDownloaded) {
                            tvModelBadge.setText("● OFFLINE READY");
                            tvModelBadge.setTextColor(Color.parseColor("#D71921"));
                        } else {
                            tvModelBadge.setText("⬇ REQUIRES DOWNLOAD (~30MB)");
                            tvModelBadge.setTextColor(Color.parseColor("#B0B0B0"));
                        }
                    }
                });
            });
        };

        for (Map.Entry<String, String> entry : SUPPORTED_LANGUAGES.entrySet()) {
            final String langCode = entry.getKey();
            final String langName = entry.getValue();

            Button chip = new Button(activity);
            chip.setText(langName.toUpperCase(Locale.US));
            chip.setTextSize(7.5f);
            chip.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(activity, R.font.ndot));
            chip.setPadding(24, 0, 24, 0);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    (int) (32 * activity.getResources().getDisplayMetrics().density)
            );
            params.setMargins(0, 0, 16, 0);
            chip.setLayoutParams(params);

            if (langCode.equals(selectedTargetLang[0])) {
                chip.setBackgroundResource(R.drawable.bg_chip_red_active);
                chip.setTextColor(Color.WHITE);
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected);
                chip.setTextColor(Color.parseColor("#88FFFFFF"));
            }

            chip.setOnClickListener(v -> {
                selectedTargetLang[0] = langCode;
                for (Button b : chipButtons) {
                    b.setBackgroundResource(R.drawable.bg_chip_unselected);
                    b.setTextColor(Color.parseColor("#88FFFFFF"));
                }
                chip.setBackgroundResource(R.drawable.bg_chip_red_active);
                chip.setTextColor(Color.WHITE);
                updateModelBadge.run();
            });

            chipButtons.add(chip);
            layoutChips.addView(chip);
        }

        updateModelBadge.run();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnStart.setOnClickListener(v -> {
            btnStart.setEnabled(false);
            btnCancel.setEnabled(false);
            btnModeReplace.setEnabled(false);
            btnModeDual.setEnabled(false);
            for (Button b : chipButtons) b.setEnabled(false);

            layoutProgress.setVisibility(View.VISIBLE);
            tvProgressStatus.setText("● PREPARING TRANSLATION MODEL...");

            translateSubtitleCues(
                    activity,
                    videoPathOrTitle,
                    existingCues,
                    TranslateLanguage.ENGLISH,
                    selectedTargetLang[0],
                    replaceOriginal[0],
                    new TranslationProgressCallback() {
                        @Override
                        public void onStatus(String status) {
                            tvProgressStatus.setText("● " + status.toUpperCase(Locale.US));
                        }

                        @Override
                        public void onProgress(int current, int total, int percent, String sourceText, String translatedText) {
                            pbProgress.setProgress(percent);
                            tvProgressStatus.setText(String.format(Locale.US, "● TRANSLATING DIALOGUES... [%d / %d] (%d%%)", current, total, percent));
                            tvSourcePreview.setText("ORIGINAL: " + (sourceText.length() > 50 ? sourceText.substring(0, 50) + "..." : sourceText));
                            tvTargetPreview.setText("TRANSLATED: " + (translatedText.length() > 50 ? translatedText.substring(0, 50) + "..." : translatedText));
                        }

                        @Override
                        public void onCompleted(File srtFile, List<SubtitleParser.SubtitleCue> translatedCues, boolean replace) {
                            dialog.dismiss();
                            if (actionCallback != null) {
                                actionCallback.onSubtitleReady(srtFile, translatedCues, replace);
                            }
                        }

                        @Override
                        public void onError(String message) {
                            layoutProgress.setVisibility(View.GONE);
                            btnStart.setEnabled(true);
                            btnCancel.setEnabled(true);
                            btnModeReplace.setEnabled(true);
                            btnModeDual.setEnabled(true);
                            for (Button b : chipButtons) b.setEnabled(true);
                            Toast.makeText(activity, "Error: " + message, Toast.LENGTH_LONG).show();
                        }
                    }
            );
        });

        dialog.show();
    }
}

