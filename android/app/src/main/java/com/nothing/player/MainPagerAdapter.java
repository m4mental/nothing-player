package com.nothing.player;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainPagerAdapter extends FragmentStateAdapter {
    private final VideosFragment videosFragment = new VideosFragment();
    private final MusicFragment musicFragment = new MusicFragment();
    private final SettingsFragment settingsFragment = new SettingsFragment();

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 1:
                return musicFragment;
            case 2:
                return settingsFragment;
            case 0:
            default:
                return videosFragment;
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }

    public VideosFragment getVideosFragment() {
        return videosFragment;
    }

    public MusicFragment getMusicFragment() {
        return musicFragment;
    }
}
