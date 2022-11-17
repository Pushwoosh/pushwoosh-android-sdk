package com.pushwoosh.testingapp;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

/**
 * Created by etkachenko on 2/21/17.
 */

public class SectionsPagerAdapter extends FragmentPagerAdapter {
	private static final Integer PAGES_NUMBER = 5;

	public SectionsPagerAdapter(FragmentManager fm) {
		super(fm);
	}

	@Override
	public Fragment getItem(int position) {
		return PlaceholderFragment.newInstance(position + 1);
	}

	@Override
	public int getCount() {
		return PAGES_NUMBER;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		switch (position) {
			case 0:
				return "Main";
			case 1:
				return "Settings";
			case 2:
				return "Stats";
			case 3:
				return "Logs";
			case 4:
				return "Inbox";
		}
		return null;
	}
}
