package com.nothing.player;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainPagerAdapter extends FragmentStateAdapter {
    private final FragmentManager fragmentManager;

    public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
        this.fragmentManager = fragmentActivity.getSupportFragmentManager();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 1) {
            return new MusicFragment();
        }
        return new VideosFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }

    public VideosFragment getVideosFragment() {
        Fragment f = fragmentManager.findFragmentByTag("f0");
        return (f instanceof VideosFragment) ? (VideosFragment) f : null;
    }

    public MusicFragment getMusicFragment() {
        Fragment f = fragmentManager.findFragmentByTag("f1");
        return (f instanceof MusicFragment) ? (MusicFragment) f : null;
    }
}
