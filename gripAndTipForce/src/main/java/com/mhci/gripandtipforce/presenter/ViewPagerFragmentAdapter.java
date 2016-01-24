package com.mhci.gripandtipforce.presenter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import com.mhci.gripandtipforce.fragment.ViewPagerFragment;
import com.mhci.gripandtipforce.model.ProjectConfig;

public class ViewPagerFragmentAdapter extends FragmentPagerAdapter{
	private final int mCount = 4; // number of pages
	private ViewPagerFragment[] fragments;

    public ViewPagerFragmentAdapter(FragmentManager fm) {
        super(fm);
        fragments = new ViewPagerFragment[mCount];
    }

    @Override
    public Fragment getItem(int position) {
        Log.d("testViewPager","get item:" + position);
        if(fragments[position] == null) {
            fragments[position] = ViewPagerFragment.newInstance(position);
        }

        fragments[position].mPageIndex = position;

        return fragments[position];
    }

    @Override
    public int getCount() {
        return mCount;
    }

    @Override
    public CharSequence getPageTitle(int position) {
      return ProjectConfig.appTitle;
    }

}