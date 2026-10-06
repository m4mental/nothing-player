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
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;
import java.util.List;

/**
 * Universal Dialog Helper for Nothing OS NDOT Dot-Matrix Theme.
 * Solves button invisibility/blankness issues by enforcing high-contrast,
 * pure white text and themed button surfaces.
 */
public class NothingDialogHelper {

    public interface OnItemSelectedListener {
        void onItemSelected(int index, String item);
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
                tvTitle.setText(title.toUpperCase());
            }
            if (tvMsg != null && message != null) {
                tvMsg.setText(message);
            }

            if (btnCancel != null) {
                if (negativeText != null) {
                    btnCancel.setText(negativeText.toUpperCase());
                }
                btnCancel.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (onNegative != null) onNegative.run();
                });
            }

            if (btnPositive != null) {
                if (positiveText != null) {
                    btnPositive.setText(positiveText.toUpperCase());
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
                tvTitle.setText(title.toUpperCase());
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

                tvTrack.setText(text.toUpperCase());
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
                tvTitle.setText(title.toUpperCase());
            }

            LinearLayout container = dialog.findViewById(R.id.layout_track_items_container);
            container.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(context);

            for (MenuItemOption option : options) {
                View itemView = inflater.inflate(R.layout.item_nothing_menu_option, container, false);
                TextView tvMenuTitle = itemView.findViewById(R.id.tv_menu_title);
                TextView tvMenuSubtitle = itemView.findViewById(R.id.tv_menu_subtitle);

                tvMenuTitle.setText(option.title.toUpperCase());
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
}
