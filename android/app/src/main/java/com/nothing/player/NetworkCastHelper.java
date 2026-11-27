package com.nothing.player;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.RendererDiscoverer;
import org.videolan.libvlc.RendererItem;
import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.libvlc.util.MediaBrowser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Provides LibVLC-powered interactive SMB/FTP/UPnP/DLNA network share folder browsing
 * and Wi-Fi / Chromecast Renderer discovery & casting in Nothing OS NDOT styling.
 */
public final class NetworkCastHelper {

    public interface RendererSelectionCallback {
        void onSelectRenderer(RendererItem rendererOrNullForLocal);
    }

    private NetworkCastHelper() {}

    private static Typeface getNdot(Context context) {
        try {
            return ResourcesCompat.getFont(context, R.font.ndot);
        } catch (Throwable ignored) {
            return Typeface.DEFAULT_BOLD;
        }
    }

    private static int dp(Context ctx, int value) {
        return (int) (value * ctx.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static boolean isPlayableVideoName(String name) {
        return MediaRepository.isVideoExtension(name);
    }

    /**
     * Opens an interactive LibVLC MediaBrowser dialog starting either from LAN discovery
     * (when initialUrl is null/empty) or directly inside the given smb:// / ftp:// URL.
     */
    public static void showNetworkBrowserDialog(Activity activity, String initialUrl) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        final Context ctx = activity;
        final Typeface ndot = getNdot(ctx);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_card_nothing);
        root.setPadding(dp(ctx, 18), dp(ctx, 18), dp(ctx, 18), dp(ctx, 18));

        TextView titleTv = new TextView(ctx);
        titleTv.setText(initialUrl == null || initialUrl.trim().isEmpty()
                ? "NETWORK SHARES (SMB / UPNP / FTP)"
                : "BROWSE: " + initialUrl);
        titleTv.setTextColor(Color.parseColor("#D71921"));
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        titleTv.setTypeface(ndot, Typeface.BOLD);
        titleTv.setSingleLine(true);
        root.addView(titleTv);

        TextView statusTv = new TextView(ctx);
        statusTv.setText("DISCOVERING NETWORK SHARES...");
        statusTv.setTextColor(Color.parseColor("#99FFFFFF"));
        statusTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        statusTv.setTypeface(ndot);
        statusTv.setPadding(0, dp(ctx, 6), 0, dp(ctx, 10));
        root.addView(statusTv);

        ProgressBar spinner = new ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
        spinner.setIndeterminate(true);
        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 4));
        spinLp.bottomMargin = dp(ctx, 10);
        root.addView(spinner, spinLp);

        ScrollView scroll = new ScrollView(ctx);
        LinearLayout listContainer = new LinearLayout(ctx);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 290));
        root.addView(scroll, scrollLp);

        LinearLayout footer = new LinearLayout(ctx);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.END);
        footer.setPadding(0, dp(ctx, 12), 0, 0);

        Button btnBack = new Button(ctx);
        btnBack.setText("BACK");
        btnBack.setTextColor(Color.WHITE);
        btnBack.setTypeface(ndot, Typeface.BOLD);
        btnBack.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btnBack.setBackgroundResource(R.drawable.bg_btn_nothing_outline);
        btnBack.setVisibility(View.GONE);
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(ctx, 38));
        backLp.rightMargin = dp(ctx, 8);
        footer.addView(btnBack, backLp);

        Button btnPlayAll = new Button(ctx);
        btnPlayAll.setText("PLAY FOLDER");
        btnPlayAll.setTextColor(Color.WHITE);
        btnPlayAll.setTypeface(ndot, Typeface.BOLD);
        btnPlayAll.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btnPlayAll.setBackgroundResource(R.drawable.bg_circle_red);
        btnPlayAll.setVisibility(View.GONE);
        LinearLayout.LayoutParams playLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(ctx, 38));
        playLp.rightMargin = dp(ctx, 8);
        footer.addView(btnPlayAll, playLp);

        Button btnClose = new Button(ctx);
        btnClose.setText("CLOSE");
        btnClose.setTextColor(Color.WHITE);
        btnClose.setTypeface(ndot, Typeface.BOLD);
        btnClose.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btnClose.setBackgroundResource(R.drawable.bg_btn_nothing_outline);
        footer.addView(btnClose, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(ctx, 38)));

        root.addView(footer);

        AlertDialog dialog = new AlertDialog.Builder(ctx).setView(root).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        final LibVLC[] libVlcHolder = new LibVLC[1];
        final MediaBrowser[] browserHolder = new MediaBrowser[1];
        final Deque<String> historyStack = new ArrayDeque<>();
        final String[] currentUrl = {initialUrl != null ? initialUrl.trim() : ""};
        final ArrayList<String> folderVideoUrls = new ArrayList<>();
        final ArrayList<String> folderVideoTitles = new ArrayList<>();

        Runnable cleanup = () -> {
            try {
                if (browserHolder[0] != null) {
                    browserHolder[0].release();
                    browserHolder[0] = null;
                }
            } catch (Throwable ignored) {}
            try {
                if (libVlcHolder[0] != null) {
                    libVlcHolder[0].release();
                    libVlcHolder[0] = null;
                }
            } catch (Throwable ignored) {}
        };

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(d -> cleanup.run());

        try {
            ArrayList<String> vlcOpts = new ArrayList<>();
            vlcOpts.add("--no-video-title-show");
            vlcOpts.add("--network-caching=2500");
            libVlcHolder[0] = new LibVLC(ctx.getApplicationContext(), vlcOpts);

            final Runnable[] refreshListRef = new Runnable[1];

            MediaBrowser.EventListener listener = new MediaBrowser.EventListener() {
                @Override
                public void onMediaAdded(int index, IMedia media) {
                    if (refreshListRef[0] != null) {
                        activity.runOnUiThread(refreshListRef[0]);
                    }
                }

                @Override
                public void onMediaRemoved(int index, IMedia media) {
                    if (refreshListRef[0] != null) {
                        activity.runOnUiThread(refreshListRef[0]);
                    }
                }

                @Override
                public void onBrowseEnd() {
                    activity.runOnUiThread(() -> {
                        spinner.setVisibility(View.GONE);
                        if (refreshListRef[0] != null) refreshListRef[0].run();
                        if (folderVideoUrls.isEmpty() && listContainer.getChildCount() == 0) {
                            statusTv.setText("NO SHARES OR FILES FOUND ON LAN");
                        }
                    });
                }
            };

            MediaBrowser browser = new MediaBrowser(libVlcHolder[0], listener, new Handler(Looper.getMainLooper()));
            browser.setIgnoreFileTypes("db,ini,nfo,jpg,jpeg,png,txt,log,exe");
            browserHolder[0] = browser;

            refreshListRef[0] = () -> {
                if (activity.isFinishing() || activity.isDestroyed() || browserHolder[0] == null) return;
                listContainer.removeAllViews();
                folderVideoUrls.clear();
                folderVideoTitles.clear();
                int count = browserHolder[0].getMediaCount();
                statusTv.setText(count + " ITEM" + (count == 1 ? "" : "S") + " FOUND");
                btnBack.setVisibility(historyStack.isEmpty() ? View.GONE : View.VISIBLE);

                for (int i = 0; i < count; i++) {
                    IMedia item = browserHolder[0].getMediaAt(i);
                    if (item == null) continue;
                    int type = item.getType();
                    Uri itemUri = item.getUri();
                    String uriStr = itemUri != null ? itemUri.toString() : "";
                    String label = item.getMeta(IMedia.Meta.Title);
                    if (label == null || label.trim().isEmpty()) {
                        label = itemUri != null && itemUri.getLastPathSegment() != null
                                ? itemUri.getLastPathSegment() : uriStr;
                    }
                    final boolean isFolder = (type == IMedia.Type.Directory || type == IMedia.Type.Disc)
                            || (!isPlayableVideoName(label) && !uriStr.contains("?"));
                    final String itemLabel = label;
                    final String itemUrl = uriStr;

                    if (!isFolder && !itemUrl.isEmpty()) {
                        folderVideoUrls.add(itemUrl);
                        folderVideoTitles.add(itemLabel);
                    }

                    TextView row = new TextView(ctx);
                    row.setText((isFolder ? "[DIR]  " : "●  ") + itemLabel.toUpperCase(Locale.US));
                    row.setTextColor(isFolder ? Color.WHITE : Color.parseColor("#D71921"));
                    row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
                    row.setTypeface(ndot, isFolder ? Typeface.BOLD : Typeface.NORMAL);
                    row.setPadding(dp(ctx, 10), dp(ctx, 11), dp(ctx, 10), dp(ctx, 11));
                    row.setSingleLine(true);

                    row.setOnClickListener(v -> {
                        if (itemUrl.isEmpty()) return;
                        if (isFolder && browserHolder[0] != null) {
                            historyStack.push(currentUrl[0]);
                            currentUrl[0] = itemUrl;
                            titleTv.setText("BROWSE: " + itemLabel.toUpperCase(Locale.US));
                            statusTv.setText("LOADING DIRECTORY...");
                            spinner.setVisibility(View.VISIBLE);
                            btnPlayAll.setVisibility(View.GONE);
                            listContainer.removeAllViews();
                            browserHolder[0].browse(Uri.parse(itemUrl),
                                    MediaBrowser.Flag.Interact | MediaBrowser.Flag.ShowHiddenFiles);
                        } else {
                            int clickedIdx = folderVideoUrls.indexOf(itemUrl);
                            launchNetworkPlaylist(activity, folderVideoUrls, folderVideoTitles,
                                    Math.max(0, clickedIdx), itemUrl, itemLabel);
                            dialog.dismiss();
                        }
                    });
                    listContainer.addView(row);
                    item.release();
                }

                btnPlayAll.setVisibility(folderVideoUrls.isEmpty() ? View.GONE : View.VISIBLE);
            };

            btnBack.setOnClickListener(v -> {
                if (historyStack.isEmpty() || browserHolder[0] == null) return;
                String prev = historyStack.pop();
                currentUrl[0] = prev;
                spinner.setVisibility(View.VISIBLE);
                btnPlayAll.setVisibility(View.GONE);
                listContainer.removeAllViews();
                if (prev == null || prev.isEmpty()) {
                    titleTv.setText("NETWORK SHARES (SMB / UPNP / FTP)");
                    statusTv.setText("DISCOVERING NETWORK SHARES...");
                    browserHolder[0].discoverNetworkShares();
                } else {
                    titleTv.setText("BROWSE: " + prev);
                    statusTv.setText("LOADING DIRECTORY...");
                    browserHolder[0].browse(Uri.parse(prev),
                            MediaBrowser.Flag.Interact | MediaBrowser.Flag.ShowHiddenFiles);
                }
            });

            btnPlayAll.setOnClickListener(v -> {
                if (!folderVideoUrls.isEmpty()) {
                    launchNetworkPlaylist(activity, folderVideoUrls, folderVideoTitles, 0,
                            folderVideoUrls.get(0), folderVideoTitles.get(0));
                    dialog.dismiss();
                }
            });

            if (currentUrl[0].isEmpty()) {
                browser.discoverNetworkShares();
            } else {
                browser.browse(Uri.parse(currentUrl[0]),
                        MediaBrowser.Flag.Interact | MediaBrowser.Flag.ShowHiddenFiles);
            }
        } catch (Throwable t) {
            spinner.setVisibility(View.GONE);
            statusTv.setText("NETWORK BROWSER UNAVAILABLE: " + t.getMessage());
        }

        dialog.show();
    }

    private static void launchNetworkPlaylist(Activity activity, ArrayList<String> urls,
                                              ArrayList<String> titles, int index,
                                              String fallbackUrl, String fallbackTitle) {
        Intent intent = new Intent(activity, ExoVideoPlayerActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(fallbackUrl));
        intent.putExtra("video_uri", fallbackUrl);
        intent.putExtra("contentUri", fallbackUrl);
        intent.putExtra("path", fallbackUrl);
        intent.putExtra("video_path", fallbackUrl);
        intent.putExtra("title", fallbackTitle);
        intent.putExtra("video_title", fallbackTitle);
        if (!urls.isEmpty()) {
            intent.putStringArrayListExtra("playlist_paths", urls);
            intent.putStringArrayListExtra("playlist_uris", urls);
            intent.putStringArrayListExtra("playlist_titles", titles);
            intent.putExtra("playlist_index", index);
        }
        activity.startActivity(intent);
    }

    /**
     * Discovers Chromecast / mDNS / DIAL renderers via LibVLC RendererDiscoverer and lets the
     * user pick a Wi-Fi cast target or jump to system Cast Settings.
     */
    public static void showRendererPickerDialog(Activity activity, LibVLC existingLibVlc,
                                                RendererSelectionCallback callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;

        final Context ctx = activity;
        final Typeface ndot = getNdot(ctx);

        LinearLayout root = new LinearLayout(ctx);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_card_nothing);
        root.setPadding(dp(ctx, 18), dp(ctx, 18), dp(ctx, 18), dp(ctx, 18));

        TextView titleTv = new TextView(ctx);
        titleTv.setText("CAST TO WI-FI RENDERER");
        titleTv.setTextColor(Color.parseColor("#D71921"));
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        titleTv.setTypeface(ndot, Typeface.BOLD);
        root.addView(titleTv);

        TextView statusTv = new TextView(ctx);
        statusTv.setText("SCANNING CHROMECAST & WI-FI DISPLAYS...");
        statusTv.setTextColor(Color.parseColor("#99FFFFFF"));
        statusTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        statusTv.setTypeface(ndot);
        statusTv.setPadding(0, dp(ctx, 6), 0, dp(ctx, 10));
        root.addView(statusTv);

        LinearLayout listContainer = new LinearLayout(ctx);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(listContainer);

        Button btnSysCast = new Button(ctx);
        btnSysCast.setText("WIRELESS DISPLAY (MIRACAST)");
        btnSysCast.setTextColor(Color.WHITE);
        btnSysCast.setTypeface(ndot, Typeface.BOLD);
        btnSysCast.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btnSysCast.setBackgroundResource(R.drawable.bg_btn_nothing_outline);
        LinearLayout.LayoutParams sysLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 40));
        sysLp.topMargin = dp(ctx, 12);
        root.addView(btnSysCast, sysLp);

        Button btnLocal = new Button(ctx);
        btnLocal.setText("RESET TO THIS PHONE");
        btnLocal.setTextColor(Color.WHITE);
        btnLocal.setTypeface(ndot, Typeface.BOLD);
        btnLocal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        btnLocal.setBackgroundResource(R.drawable.bg_circle_red);
        LinearLayout.LayoutParams localLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 40));
        localLp.topMargin = dp(ctx, 8);
        root.addView(btnLocal, localLp);

        AlertDialog dialog = new AlertDialog.Builder(ctx).setView(root).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        final boolean ownsLibVlc = (existingLibVlc == null);
        final LibVLC[] vlcHolder = {existingLibVlc};
        final List<RendererDiscoverer> discoverers = new ArrayList<>();
        final List<RendererItem> foundItems = new ArrayList<>();
        final boolean[] selectedRenderer = {false};

        dialog.setOnDismissListener(d -> {
            for (RendererDiscoverer rd : discoverers) {
                try { rd.stop(); } catch (Throwable ignored) {}
                try { rd.release(); } catch (Throwable ignored) {}
            }
            discoverers.clear();
            if (!selectedRenderer[0]) {
                for (RendererItem item : foundItems) {
                    try { item.release(); } catch (Throwable ignored) {}
                }
            }
            if (ownsLibVlc && vlcHolder[0] != null) {
                try { vlcHolder[0].release(); } catch (Throwable ignored) {}
                vlcHolder[0] = null;
            }
        });

        btnLocal.setOnClickListener(v -> {
            if (callback != null) callback.onSelectRenderer(null);
            dialog.dismiss();
        });

        btnSysCast.setOnClickListener(v -> {
            dialog.dismiss();
            try {
                activity.startActivity(new Intent(Settings.ACTION_CAST_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(activity, "Open Quick Settings > Cast", Toast.LENGTH_SHORT).show();
            }
        });

        try {
            if (vlcHolder[0] == null) {
                vlcHolder[0] = new LibVLC(ctx.getApplicationContext(), new ArrayList<>());
            }
            RendererDiscoverer.Description[] descs = RendererDiscoverer.list(vlcHolder[0]);
            if (descs != null) {
                for (RendererDiscoverer.Description desc : descs) {
                    RendererDiscoverer rd = new RendererDiscoverer(vlcHolder[0], desc.name);
                    rd.setEventListener(event -> {
                        if (event.type == RendererDiscoverer.Event.ItemAdded && event.getItem() != null) {
                            RendererItem item = event.getItem();
                            item.retain();
                            activity.runOnUiThread(() -> {
                                if (!dialog.isShowing()) return;
                                foundItems.add(item);
                                statusTv.setText(foundItems.size() + " WI-FI DEVICE" + (foundItems.size() == 1 ? "" : "S") + " READY");
                                TextView row = new TextView(ctx);
                                String display = item.displayName != null ? item.displayName : item.name;
                                row.setText("●  " + display.toUpperCase(Locale.US) + " (" + item.type + ")");
                                row.setTextColor(Color.WHITE);
                                row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
                                row.setTypeface(ndot, Typeface.BOLD);
                                row.setPadding(dp(ctx, 10), dp(ctx, 12), dp(ctx, 10), dp(ctx, 12));
                                row.setOnClickListener(cv -> {
                                    selectedRenderer[0] = true;
                                    if (callback != null) callback.onSelectRenderer(item);
                                    dialog.dismiss();
                                });
                                listContainer.addView(row);
                            });
                        }
                    });
                    if (rd.start()) {
                        discoverers.add(rd);
                    } else {
                        rd.release();
                    }
                }
            }
            if (discoverers.isEmpty()) {
                statusTv.setText("NO CAST SERVICES ACTIVE — USE WIRELESS DISPLAY BELOW");
            }
        } catch (Throwable t) {
            statusTv.setText("CAST DISCOVERY ERROR — USE WIRELESS DISPLAY BELOW");
        }

        dialog.show();
    }
}
