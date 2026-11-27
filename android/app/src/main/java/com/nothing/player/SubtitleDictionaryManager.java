package com.nothing.player;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Instant In-Subtitle Dictionary & Translation Engine for NOTHING-PLAYER.
 * Provides word definitions, phonetics, Hindi translations, and native TTS speech.
 */
public class SubtitleDictionaryManager {
    private static final String TAG = "SubtitleDictionary";
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static TextToSpeech ttsInstance = null;
    private static boolean isTtsInitialized = false;

    // Offline Dictionary Cache for common cinematic/literary words
    private static final Map<String, String[]> OFFLINE_LEXICON = new HashMap<>();

    static {
        OFFLINE_LEXICON.put("anomaly", new String[]{"noun", "/əˈnɒm.ə.li/", "Something that deviates from what is standard, normal, or expected.", "विसंगति / अनियमितता"});
        OFFLINE_LEXICON.put("inevitable", new String[]{"adjective", "/ɪnˈev.ɪ.tə.bəl/", "Certain to happen; unavoidable.", "अपरिहार्य / जिसे टाला न जा सके"});
        OFFLINE_LEXICON.put("serendipity", new String[]{"noun", "/ˌser.ənˈdɪp.ə.ti/", "The occurrence of events by chance in a happy or beneficial way.", "आकस्मिक लाभ / सुखद संयोग"});
        OFFLINE_LEXICON.put("evacuate", new String[]{"verb", "/ɪˈvæk.ju.eɪt/", "Remove someone from a place of danger to a safe place.", "खाली करना / सुरक्षित स्थान पर ले जाना"});
        OFFLINE_LEXICON.put("protocol", new String[]{"noun", "/ˈprəʊ.tə.kɒl/", "The official procedure or system of rules governing affairs.", "नियम / कार्यप्रणाली"});
        OFFLINE_LEXICON.put("breach", new String[]{"noun/verb", "/briːtʃ/", "An act of breaking or failing to observe a law, agreement, or defense.", "उल्लंघन / दरार / भेदना"});
        OFFLINE_LEXICON.put("conspiracy", new String[]{"noun", "/kənˈspɪr.ə.si/", "A secret plan by a group to do something unlawful or harmful.", "षड्यंत्र / साज़िश"});
        OFFLINE_LEXICON.put("sanctuary", new String[]{"noun", "/ˈsæŋk.tʃʊə.ri/", "A place of safety or refuge.", "शरणस्थल / सुरक्षित आश्रय"});
        OFFLINE_LEXICON.put("velocity", new String[]{"noun", "/vəˈlɒs.ə.ti/", "The speed of something in a given direction.", "वेग / गति"});
        OFFLINE_LEXICON.put("surrender", new String[]{"verb", "/səˈren.dər/", "Stop resisting to an enemy or opponent and submit to their authority.", "आत्मसमर्पण करना / हार मानना"});
        OFFLINE_LEXICON.put("betrayal", new String[]{"noun", "/bɪˈtreɪ.əl/", "The action of betraying one's country, group, or a person; treachery.", "विश्वासघात / धोखा"});
        OFFLINE_LEXICON.put("redemption", new String[]{"noun", "/rɪˈdemp.ʃən/", "The action of saving or being saved from sin, error, or evil.", "प्रायश्चित / मुक्ति"});
        OFFLINE_LEXICON.put("classified", new String[]{"adjective", "/ˈklæs.ɪ.faɪd/", "Kept secret from all but a few people in the government.", "गोपनीय / गुप्त"});
        OFFLINE_LEXICON.put("containment", new String[]{"noun", "/kənˈteɪn.mənt/", "The action of keeping something harmful under control or within limits.", "रोकथाम / नियंत्रण"});
        OFFLINE_LEXICON.put("vulnerable", new String[]{"adjective", "/ˈvʌl.nər.ə.bəl/", "Exposed to the possibility of being attacked or harmed.", "असुरक्षित / नाज़ुक"});
    }

    public static class WordDefinition {
        public String word;
        public String phonetic;
        public String partOfSpeech;
        public String definition;
        public String hindiTranslation;
        public String example;
        public String contextSentence;
    }

    public interface DictionaryCallback {
        void onResult(WordDefinition definition);
        void onError(String message);
    }

    public static void initTts(Context context) {
        if (ttsInstance == null) {
            ttsInstance = new TextToSpeech(context.getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS && ttsInstance != null) {
                    ttsInstance.setLanguage(Locale.US);
                    isTtsInitialized = true;
                }
            });
        }
    }

    public static void speakWord(Context context, String word) {
        if (word == null || word.trim().isEmpty()) return;
        if (ttsInstance == null || !isTtsInitialized) {
            ttsInstance = new TextToSpeech(context.getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS && ttsInstance != null) {
                    ttsInstance.setLanguage(Locale.US);
                    isTtsInitialized = true;
                    ttsInstance.speak(word, TextToSpeech.QUEUE_FLUSH, null, "nothing_word_tts");
                }
            });
        } else {
            ttsInstance.speak(word, TextToSpeech.QUEUE_FLUSH, null, "nothing_word_tts");
        }
    }

    public static void lookupWord(Context context, String rawWord, String contextSentence, DictionaryCallback callback) {
        if (rawWord == null || rawWord.trim().isEmpty()) {
            if (callback != null) callback.onError("Empty word");
            return;
        }

        final String cleanWord = rawWord.replaceAll("[^a-zA-Z]", "").toLowerCase().trim();
        if (cleanWord.isEmpty()) {
            if (callback != null) callback.onError("Invalid word characters");
            return;
        }

        EXECUTOR.execute(() -> {
            WordDefinition result = new WordDefinition();
            result.word = cleanWord;
            result.contextSentence = contextSentence;

            // Check Offline Lexicon First
            if (OFFLINE_LEXICON.containsKey(cleanWord)) {
                String[] data = OFFLINE_LEXICON.get(cleanWord);
                result.partOfSpeech = data[0];
                result.phonetic = data[1];
                result.definition = data[2];
                result.hindiTranslation = data[3];
            }

            // Fetch English Definition & Phonetics from Free Public Dictionary API
            try {
                String dictUrl = "https://api.dictionaryapi.dev/api/v2/entries/en/" + URLEncoder.encode(cleanWord, "UTF-8");
                HttpURLConnection conn = (HttpURLConnection) new URL(dictUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (InputStream is = conn.getInputStream();
                         BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = r.readLine()) != null) sb.append(line);
                    }
                    JSONArray arr = new JSONArray(sb.toString());
                    if (arr.length() > 0) {
                        JSONObject entry = arr.getJSONObject(0);
                        if (entry.has("phonetic") && !entry.isNull("phonetic")) {
                            result.phonetic = entry.getString("phonetic");
                        }
                        if (entry.has("meanings")) {
                            JSONArray meanings = entry.getJSONArray("meanings");
                            if (meanings.length() > 0) {
                                JSONObject m = meanings.getJSONObject(0);
                                if (m.has("partOfSpeech")) {
                                    result.partOfSpeech = m.getString("partOfSpeech");
                                }
                                if (m.has("definitions")) {
                                    JSONArray defs = m.getJSONArray("definitions");
                                    if (defs.length() > 0) {
                                        JSONObject d = defs.getJSONObject(0);
                                        if (d.has("definition")) {
                                            result.definition = d.getString("definition");
                                        }
                                        if (d.has("example")) {
                                            result.example = d.getString("example");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.w(TAG, "Online dictionary fetch failed: " + e.getMessage());
            }

            // Fetch Hindi Translation from Public Translation API
            try {
                String transUrl = "https://api.mymemory.translated.net/get?q=" + URLEncoder.encode(cleanWord, "UTF-8") + "&langpair=en|hi";
                HttpURLConnection conn = (HttpURLConnection) new URL(transUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(4500);
                conn.setReadTimeout(4500);

                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    try (InputStream is = conn.getInputStream();
                         BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = r.readLine()) != null) sb.append(line);
                    }
                    JSONObject resp = new JSONObject(sb.toString());
                    if (resp.has("responseData")) {
                        JSONObject rd = resp.getJSONObject("responseData");
                        String translated = rd.optString("translatedText", "");
                        if (!translated.isEmpty() && !translated.equalsIgnoreCase(cleanWord)) {
                            result.hindiTranslation = translated;
                        }
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.w(TAG, "Online translation fetch failed: " + e.getMessage());
            }

            // Fallback if no definition found
            if (result.definition == null || result.definition.isEmpty()) {
                result.definition = "Definition not found in public dictionary.";
            }
            if (result.hindiTranslation == null || result.hindiTranslation.isEmpty()) {
                result.hindiTranslation = "अनुवाद उपलब्ध नहीं";
            }

            MAIN_HANDLER.post(() -> {
                if (callback != null) callback.onResult(result);
            });
        });
    }

    public static void showWordSelectorDialog(Activity activity, String subtitleLine, WordSelectedCallback callback) {
        if (activity == null || activity.isFinishing() || subtitleLine == null || subtitleLine.trim().isEmpty()) {
            return;
        }

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_word_selector, null);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvSubtitleText = view.findViewById(R.id.tv_selector_subtitle_line);
        LinearLayout layoutWordChips = view.findViewById(R.id.layout_selector_words);
        Button btnClose = view.findViewById(R.id.btn_close_word_selector);

        if (tvSubtitleText != null) {
            tvSubtitleText.setText("\"" + subtitleLine.replace("\n", " ") + "\"");
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        String[] tokens = subtitleLine.replaceAll("[^a-zA-Z0-9'\\- ]", " ").split("\\s+");
        LinearLayout currentRow = null;
        int rowWordCount = 0;

        for (String rawToken : tokens) {
            String word = rawToken.replaceAll("[^a-zA-Z]", "").trim();
            if (word.length() < 2) continue;

            if (currentRow == null || rowWordCount >= 4) {
                currentRow = new LinearLayout(activity);
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                currentRow.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                ((LinearLayout.LayoutParams) currentRow.getLayoutParams()).bottomMargin = 8;
                layoutWordChips.addView(currentRow);
                rowWordCount = 0;
            }

            Button chipBtn = new Button(activity);
            chipBtn.setText(word.toUpperCase(Locale.US));
            chipBtn.setTextColor(Color.WHITE);
            chipBtn.setTextSize(9.5f);
            chipBtn.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(activity, R.font.ndot));
            chipBtn.setBackgroundResource(R.drawable.bg_chip_dark_inactive);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (36 * activity.getResources().getDisplayMetrics().density), 1f);
            lp.setMargins(4, 4, 4, 4);
            chipBtn.setLayoutParams(lp);

            chipBtn.setOnClickListener(v -> {
                dialog.dismiss();
                if (callback != null) callback.onWordSelected(word, subtitleLine);
            });

            currentRow.addView(chipBtn);
            rowWordCount++;
        }

        dialog.show();
    }

    public static void showDictionaryDialog(Activity activity, WordDefinition def, Runnable onResumePlayback) {
        if (activity == null || activity.isFinishing() || def == null) return;

        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_subtitle_dictionary, null);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(view)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView tvWord = view.findViewById(R.id.tv_dict_word);
        TextView tvPhonetic = view.findViewById(R.id.tv_dict_phonetic);
        TextView tvHindi = view.findViewById(R.id.tv_dict_hindi);
        TextView tvDef = view.findViewById(R.id.tv_dict_definition);
        TextView tvExample = view.findViewById(R.id.tv_dict_example);
        TextView tvContext = view.findViewById(R.id.tv_dict_context_sentence);

        View btnSpeak = view.findViewById(R.id.btn_dict_speak);
        Button btnResume = view.findViewById(R.id.btn_dict_resume);
        View btnClose = view.findViewById(R.id.btn_dict_close);

        if (tvWord != null) tvWord.setText(def.word.toUpperCase(Locale.US));
        if (tvPhonetic != null) {
            String p = (def.phonetic != null ? def.phonetic : "") + (def.partOfSpeech != null ? " • " + def.partOfSpeech.toUpperCase(Locale.US) : "");
            tvPhonetic.setText(p.trim().isEmpty() ? "WORD" : p);
        }
        if (tvHindi != null) tvHindi.setText(def.hindiTranslation != null ? def.hindiTranslation : "उपलब्ध नहीं");
        if (tvDef != null) tvDef.setText(def.definition != null ? def.definition : "No definition found.");
        if (tvExample != null) {
            if (def.example != null && !def.example.isEmpty()) {
                tvExample.setText("\"" + def.example + "\"");
                tvExample.setVisibility(View.VISIBLE);
            } else {
                tvExample.setVisibility(View.GONE);
            }
        }
        if (tvContext != null && def.contextSentence != null && !def.contextSentence.isEmpty()) {
            tvContext.setText("IN SUBTITLE: \"" + def.contextSentence.replace("\n", " ") + "\"");
            tvContext.setVisibility(View.VISIBLE);
        }

        if (btnSpeak != null) {
            btnSpeak.setOnClickListener(v -> speakWord(activity, def.word));
        }

        if (btnResume != null) {
            btnResume.setOnClickListener(v -> {
                dialog.dismiss();
                if (onResumePlayback != null) onResumePlayback.run();
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    public interface WordSelectedCallback {
        void onWordSelected(String word, String contextSentence);
    }
}

