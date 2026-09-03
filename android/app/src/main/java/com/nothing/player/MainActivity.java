package com.nothing.player;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQ_CODE = 1001;

    private ViewPager2 viewPager;
    private BottomNavigationView bottomNav;
    private MainPagerAdapter pagerAdapter;
    private View miniPlayerContainer;
    private ImageView miniPlayerArt;
    private TextView miniPlayerTitle, miniPlayerArtist;
    private ImageButton miniPlayerBtnPlay, miniPlayerBtnNext;
    private ProgressBar miniPlayerProgress;
    private View redLed;

    private MusicPlaybackService musicService;
    private boolean isBound = false;
    private List<MediaItem> currentPlaylist = new ArrayList<>();
    private int currentTrackIndex = -1;
    private MediaItem currentPlayingTrack = null;
    private boolean isPlaying = false;

    private long lastBackPressTime = 0;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private NowPlayingBottomSheet activeNowPlayingSheet = null;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            MusicPlaybackService.LocalBinder localBinder = (MusicPlaybackService.LocalBinder) binder;
            musicService = localBinder.getService();
            isBound = true;
            setupServiceListeners();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.view_pager);
        bottomNav = findViewById(R.id.bottom_nav);
        miniPlayerContainer = findViewById(R.id.mini_player_container);
        miniPlayerArt = findViewById(R.id.mini_player_art);
        miniPlayerTitle = findViewById(R.id.mini_player_title);
        miniPlayerArtist = findViewById(R.id.mini_player_artist);
        miniPlayerBtnPlay = findViewById(R.id.mini_player_btn_play);
        miniPlayerBtnNext = findViewById(R.id.mini_player_btn_next);
        miniPlayerProgress = findViewById(R.id.mini_player_progress);
        redLed = findViewById(R.id.red_led);

        pagerAdapter = new MainPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(2);

        // Start Red LED pulse
        AlphaAnimation pulse = new AlphaAnimation(0.3f, 1.0f);
        pulse.setDuration(800);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        redLed.startAnimation(pulse);

        final boolean[] isPageChanging = {false};
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (isPageChanging[0]) return;
                isPageChanging[0] = true;
                if (position == 0) {
                    bottomNav.setSelectedItemId(R.id.nav_videos);
                } else if (position == 1) {
                    bottomNav.setSelectedItemId(R.id.nav_music);
                }
                isPageChanging[0] = false;
            }
        });

        bottomNav.setOnItemSelectedListener(item -> {
            if (isPageChanging[0]) return true;
            isPageChanging[0] = true;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_videos) {
                viewPager.setCurrentItem(0, true);
            } else if (itemId == R.id.nav_music) {
                viewPager.setCurrentItem(1, true);
            }
            isPageChanging[0] = false;
            return true;
        });

        miniPlayerContainer.setOnClickListener(v -> {
            if (currentPlayingTrack != null) {
                activeNowPlayingSheet = NowPlayingBottomSheet.newInstance(currentPlayingTrack);
                activeNowPlayingSheet.show(getSupportFragmentManager(), "NowPlayingBottomSheet");
            }
        });

        miniPlayerBtnPlay.setOnClickListener(v -> togglePlayPause());
        miniPlayerBtnNext.setOnClickListener(v -> playNext());

        findViewById(R.id.btn_settings_top).setOnClickListener(v -> {
            startActivity(new Intent(this, SettingsActivity.class));
        });

        checkAndRequestPermissions();
        bindMusicService();
        startProgressUpdater();
    }

    private void checkAndRequestPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQ_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            if (pagerAdapter.getVideosFragment() != null) pagerAdapter.getVideosFragment().loadVideos();
            if (pagerAdapter.getMusicFragment() != null) pagerAdapter.getMusicFragment().loadTracks();
        }
    }

    private void bindMusicService() {
        try {
            Intent intent = new Intent(this, MusicPlaybackService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupServiceListeners() {
        if (musicService == null) return;
        musicService.setCallback(new MusicPlaybackService.PlaybackCallback() {
            @Override
            public void onStateChanged(boolean playing) {
                isPlaying = playing;
                runOnUiThread(() -> {
                    miniPlayerBtnPlay.setImageResource(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                    if (activeNowPlayingSheet != null && currentPlayingTrack != null) {
                        activeNowPlayingSheet.updateTrack(currentPlayingTrack, playing);
                    }
                });
            }

            @Override
            public void onTrackEnded() {
                runOnUiThread(() -> playNext());
            }
        });
    }

    public void playAudioTrack(MediaItem track, List<MediaItem> playlist, int index) {
        if (playlist != null) {
            this.currentPlaylist = playlist;
            this.currentTrackIndex = index;
        }
        this.currentPlayingTrack = track;
        this.isPlaying = true;

        miniPlayerContainer.setVisibility(View.VISIBLE);
        miniPlayerTitle.setText(track.title);
        miniPlayerArtist.setText(track.artist != null ? track.artist : "Unknown Artist");
        miniPlayerBtnPlay.setImageResource(android.R.drawable.ic_media_pause);

        Glide.with(this)
                .load(track.path != null ? track.path : track.contentUri)
                .placeholder(android.R.drawable.ic_lock_silent_mode_off)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(miniPlayerArt);

        if (pagerAdapter.getMusicFragment() != null) {
            pagerAdapter.getMusicFragment().notifyCurrentTrackChanged(track.id);
        }

        if (isBound && musicService != null) {
            musicService.playTrack(track.title, track.artist, track.contentUri, track.path);
        }
    }

    public void togglePlayPause() {
        if (isBound && musicService != null) {
            if (isPlaying) {
                musicService.pause();
                isPlaying = false;
                miniPlayerBtnPlay.setImageResource(android.R.drawable.ic_media_play);
            } else {
                musicService.resume();
                isPlaying = true;
                miniPlayerBtnPlay.setImageResource(android.R.drawable.ic_media_pause);
            }
            if (activeNowPlayingSheet != null && currentPlayingTrack != null) {
                activeNowPlayingSheet.updateTrack(currentPlayingTrack, isPlaying);
            }
        }
    }

    public void playNext() {
        if (!currentPlaylist.isEmpty() && currentTrackIndex + 1 < currentPlaylist.size()) {
            currentTrackIndex++;
            playAudioTrack(currentPlaylist.get(currentTrackIndex), currentPlaylist, currentTrackIndex);
        }
    }

    public void playPrevious() {
        if (!currentPlaylist.isEmpty() && currentTrackIndex - 1 >= 0) {
            currentTrackIndex--;
            playAudioTrack(currentPlaylist.get(currentTrackIndex), currentPlaylist, currentTrackIndex);
        }
    }

    public void seekTo(long positionMs) {
        if (isBound && musicService != null) {
            musicService.seekTo((int) positionMs);
        }
    }

    private void startProgressUpdater() {
        progressHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isBound && musicService != null && isPlaying && currentPlayingTrack != null) {
                    long pos = musicService.getCurrentPosition();
                    long dur = currentPlayingTrack.duration > 0 ? currentPlayingTrack.duration : musicService.getDuration();
                    if (dur > 0) {
                        int progress = (int) ((pos * 100) / dur);
                        miniPlayerProgress.setProgress(progress);
                        if (activeNowPlayingSheet != null) {
                            activeNowPlayingSheet.updateProgress(pos, dur);
                        }
                    }
                }
                progressHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    @Override
    public void onBackPressed() {
        if (viewPager.getCurrentItem() == 0 && pagerAdapter.getVideosFragment() != null) {
            if (pagerAdapter.getVideosFragment().handleBackPress()) {
                return;
            }
        }

        if (viewPager.getCurrentItem() != 0) {
            viewPager.setCurrentItem(0, true);
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastBackPressTime < 2000) {
            super.onBackPressed();
        } else {
            lastBackPressTime = now;
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        progressHandler.removeCallbacksAndMessages(null);
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
