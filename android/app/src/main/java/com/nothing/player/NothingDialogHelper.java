package com.nothing.player;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.res.ResourcesCompat;
import java.util.List;
import java.util.Locale;

/**
 * Universal Dialog Helper for Nothing OS NDOT Dot-Matrix Theme.
 * Solves button invisibility/blankness issues by enforcing high-contrast,
 * pure white text and themed button surfaces.
 */
public class NothingDialogHelper {

    public interface OnItemSelectedListener {
        void onItemSelected(int index, String item);
    }

    public interface OnInputSubmittedListener {
        void onInputSubmitted(String text);
    }

    public static class MenuItemOption {
        public String title;
        public String subtitle;
        public Runnable action;

        public MenuItemOption(String title, String subtitle, Runnable action) {
            this.title = title;
            this.subtitle = subtitle;
            this.action = action;
        }
    }

    /**
     * Shows a Nothing OS styled confirmation / delete dialog.
     * Buttons have guaranteed visibility: Cancel (dark surface) and Positive (red accent).
     */
    public static Dialog showConfirmDialog(
            Context context,
            String title,
            String message,
            String positiveText,
            Runnable onPositive,
            String negativeText,
            Runnable onNegative
    ) {
        if (context == null) return null;
        if (context instanceof Activity && ((Activity) context).isFinishing()) return null;

        try {
            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_nothing_confirm);

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88f);
                dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            TextView tvTitle = dialog.findViewById(R.id.tv_confirm_dialog_title);
            TextView tvMsg = dialog.findViewById(R.id.tv_confirm_dialog_message);
            AppCompatButton btnCancel = dialog.findViewById(R.id.btn_confirm_dialog_cancel);
            AppCompatButton btnPositive = dialog.findViewById(R.id.btn_confirm_dialog_positive);

            if (tvTitle != null && title != null) {
                tvTitle.setText(title.toUpperCase(Locale.US));
            }
            if (tvMsg != null && message != null) {
                tvMsg.setText(message);
            }

            if (btnCancel != null) {
                if (negativeText != null) {
                    btnCancel.setText(negativeText.toUpperCase(Locale.US));
                }
                btnCancel.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (onNegative != null) onNegative.run();
                });
            }

            if (btnPositive != null) {
                if (positiveText != null) {
                    btnPositive.setText(positiveText.toUpperCase(Locale.US));
                }
                btnPositive.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (onPositive != null) onPositive.run();
                });
            }

            dialog.show();
            return dialog;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Shows a single-choice selection dialog (for View Mode, Sort Option, etc.).
     */
    public static Dialog showSelectionDialog(
            Context context,
            String title,
            List<String> items,
            int selectedIndex,
            OnItemSelectedListener listener
    ) {
        if (context == null) return null;
        if (context instanceof Activity && ((Activity) context).isFinishing()) return null;

        try {
            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_nothing_selection);

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88f);
                dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            TextView tvTitle = dialog.findViewById(R.id.tv_selection_dialog_title);
            if (tvTitle != null && title != null) {
                tvTitle.setText(title.toUpperCase(Locale.US));
            }

            LinearLayout container = dialog.findViewById(R.id.layout_track_items_container);
            container.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(context);

            for (int i = 0; i < items.size(); i++) {
                final int idx = i;
                final String text = items.get(i);
                View itemView = inflater.inflate(R.layout.item_nothing_track_selection, container, false);
                View indicator = itemView.findViewById(R.id.view_track_indicator);
                TextView tvTrack = itemView.findViewById(R.id.tv_track_name);

                tvTrack.setText(text.toUpperCase(Locale.US));
                if (idx == selectedIndex) {
                    indicator.setBackgroundResource(R.drawable.bg_circle_red_active);
                    tvTrack.setTextColor(Color.parseColor("#D71921"));
                } else {
                    indicator.setBackgroundResource(R.drawable.bg_circle_dark);
                    tvTrack.setTextColor(Color.parseColor("#FFFFFF"));
                }

                itemView.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (listener != null) listener.onItemSelected(idx, text);
                });

                container.addView(itemView);
            }

            Button btnCancel = dialog.findViewById(R.id.btn_selection_dialog_cancel);
            if (btnCancel != null) {
                btnCancel.setOnClickListener(v -> dialog.dismiss());
            }

            dialog.show();
            return dialog;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Shows a Nothing OS styled 3-dot overflow options menu.
     */
    public static Dialog showMenuDialog(
            Context context,
            String title,
            List<MenuItemOption> options
    ) {
        if (context == null) return null;
        if (context instanceof Activity && ((Activity) context).isFinishing()) return null;

        try {
            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_nothing_selection);

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88f);
                dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            TextView tvTitle = dialog.findViewById(R.id.tv_selection_dialog_title);
            if (tvTitle != null && title != null) {
                tvTitle.setText(title.toUpperCase(Locale.US));
            }

            LinearLayout container = dialog.findViewById(R.id.layout_track_items_container);
            container.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(context);

            for (MenuItemOption option : options) {
                View itemView = inflater.inflate(R.layout.item_nothing_menu_option, container, false);
                TextView tvMenuTitle = itemView.findViewById(R.id.tv_menu_title);
                TextView tvMenuSubtitle = itemView.findViewById(R.id.tv_menu_subtitle);

                tvMenuTitle.setText(option.title.toUpperCase(Locale.US));
                if (option.subtitle != null && !option.subtitle.isEmpty()) {
                    tvMenuSubtitle.setText(option.subtitle);
                    tvMenuSubtitle.setVisibility(View.VISIBLE);
                } else {
                    tvMenuSubtitle.setVisibility(View.GONE);
                }

                itemView.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (option.action != null) {
                        option.action.run();
                    }
                });

                container.addView(itemView);
            }

            Button btnCancel = dialog.findViewById(R.id.btn_selection_dialog_cancel);
            if (btnCancel != null) {
                btnCancel.setText("CLOSE");
                btnCancel.setOnClickListener(v -> dialog.dismiss());
            }

            dialog.show();
            return dialog;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Shows a Nothing OS themed text-input dialog without light-theme/invisible-text issues.
     */
    public static Dialog showInputDialog(
            Context context,
            String title,
            String message,
            String hint,
            String positiveText,
            int inputType,
            OnInputSubmittedListener listener
    ) {
        if (context == null) return null;
        if (context instanceof Activity && ((Activity) context).isFinishing()) return null;

        try {
            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

            float density = context.getResources().getDisplayMetrics().density;
            int pad = (int) (20 * density);

            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundResource(R.drawable.bg_dialog_nothing);
            root.setPadding(pad, pad, pad, pad);

            if (title != null) {
                TextView tvTitle = new TextView(context);
                tvTitle.setText("● " + title.toUpperCase(Locale.US));
                tvTitle.setTextColor(Color.WHITE);
                tvTitle.setTextSize(12.5f);
                tvTitle.setTypeface(ResourcesCompat.getFont(context, R.font.ndot), android.graphics.Typeface.BOLD);
                root.addView(tvTitle);
            }

            if (message != null && !message.isEmpty()) {
                TextView tvMsg = new TextView(context);
                tvMsg.setText(message);
                tvMsg.setTextColor(Color.parseColor("#B3FFFFFF"));
                tvMsg.setTextSize(9.5f);
                tvMsg.setTypeface(ResourcesCompat.getFont(context, R.font.ndot));
                tvMsg.setPadding(0, (int) (8 * density), 0, 0);
                root.addView(tvMsg);
            }

            EditText input = new EditText(context);
            input.setHint(hint != null ? hint : "");
            input.setTextColor(Color.WHITE);
            input.setHintTextColor(Color.parseColor("#66FFFFFF"));
            input.setTextSize(11f);
            input.setSingleLine(true);
            input.setInputType(inputType);
            input.setBackgroundResource(R.drawable.bg_chip_unselected);
            input.setPadding((int) (14 * density), (int) (10 * density), (int) (14 * density), (int) (10 * density));
            try {
                input.setTypeface(ResourcesCompat.getFont(context, R.font.ndot));
            } catch (Exception ignored) {}
            LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            inputLp.topMargin = (int) (14 * density);
            inputLp.bottomMargin = (int) (16 * density);
            root.addView(input, inputLp);

            LinearLayout btnRow = new LinearLayout(context);
            btnRow.setOrientation(LinearLayout.HORIZONTAL);
            btnRow.setGravity(android.view.Gravity.END);

            AppCompatButton btnCancel = new AppCompatButton(context);
            btnCancel.setText("CANCEL");
            btnCancel.setTextColor(Color.WHITE);
            btnCancel.setTextSize(9f);
            btnCancel.setTypeface(ResourcesCompat.getFont(context, R.font.ndot), android.graphics.Typeface.BOLD);
            btnCancel.setBackgroundResource(R.drawable.bg_chip_dark_inactive);
            btnCancel.setPadding((int) (14 * density), 0, (int) (14 * density), 0);
            btnCancel.setOnClickListener(v -> dialog.dismiss());
            LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, (int) (38 * density));
            cancelLp.setMarginEnd((int) (8 * density));
            btnRow.addView(btnCancel, cancelLp);

            AppCompatButton btnOk = new AppCompatButton(context);
            btnOk.setText(positiveText != null ? positiveText.toUpperCase(Locale.US) : "CONFIRM");
            btnOk.setTextColor(Color.WHITE);
            btnOk.setTextSize(9f);
            btnOk.setTypeface(ResourcesCompat.getFont(context, R.font.ndot), android.graphics.Typeface.BOLD);
            btnOk.setBackgroundResource(R.drawable.bg_chip_red_active);
            btnOk.setPadding((int) (16 * density), 0, (int) (16 * density), 0);
            btnOk.setOnClickListener(v -> {
                String val = input.getText() != null ? input.getText().toString().trim() : "";
                dialog.dismiss();
                if (listener != null) listener.onInputSubmitted(val);
            });
            LinearLayout.LayoutParams okLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, (int) (38 * density));
            btnRow.addView(btnOk, okLp);

            root.addView(btnRow);
            dialog.setContentView(root);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                int width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.88f);
                dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            dialog.show();
            return dialog;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
