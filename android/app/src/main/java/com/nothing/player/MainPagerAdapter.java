package com.nothing.player;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainPagerAdapter extends FragmentStateAdapter {
    private final VideosFragment videosFragment = new VideosFragment();
    private final MusicFragment musicFragment = new MusicFragment();

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            return musicFragment;
        }
        return videosFragment;
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public VideosFragment getVideosFragment() {
        return videosFragment;
    }

    public MusicFragment getMusicFragment() {
        return musicFragment;
    }
}
