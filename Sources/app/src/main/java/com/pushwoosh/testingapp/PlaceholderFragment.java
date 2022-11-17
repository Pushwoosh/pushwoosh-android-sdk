package com.pushwoosh.testingapp;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pushwoosh.inbox.ui.PushwooshInboxUi;
import com.pushwoosh.testingapp.layout.ActionsFragment;
import com.pushwoosh.testingapp.layout.LogsFragment;
import com.pushwoosh.testingapp.layout.SettingsFragment;
import com.pushwoosh.testingapp.layout.StatsFragment;

/**
 * Created by etkachenko on 2/21/17.
 */

public class PlaceholderFragment extends Fragment {
	private static final String ARG_SECTION_NUMBER = "section_number";
	private static final Integer FIRST_PAGE_NUMBER = 1;
	private static final Integer SECOND_PAGE_NUMBER = 2;
	private static final Integer THIRD_PAGE_NUMBER = 3;
	private static final Integer FOURTH_PAGE_NUMBER = 4;
	private static final Integer INBOX_PAGE_NUMBER = 5;

	public static Fragment newInstance(int sectionNumber) {
		if (sectionNumber == FIRST_PAGE_NUMBER) {
			ActionsFragment fragment = new ActionsFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}
		if (sectionNumber == SECOND_PAGE_NUMBER) {
			SettingsFragment fragment = new SettingsFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}
		if (sectionNumber == THIRD_PAGE_NUMBER) {
			StatsFragment fragment = new StatsFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		} else if (sectionNumber == FOURTH_PAGE_NUMBER) {
			LogsFragment fragment = new LogsFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		} else if (sectionNumber == INBOX_PAGE_NUMBER) {
			return PushwooshInboxUi.INSTANCE.createInboxFragment();
		} else {
			return null;
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		if (getArguments().getInt(ARG_SECTION_NUMBER) == FIRST_PAGE_NUMBER) {
			View rootView = inflater.inflate(R.layout.fragment_actions, container, false);
			return rootView;
		}
		if (getArguments().getInt(ARG_SECTION_NUMBER) == SECOND_PAGE_NUMBER) {
			View rootView = inflater.inflate(R.layout.fragment_settings, container, false);
			return rootView;
		}
		if (getArguments().getInt(ARG_SECTION_NUMBER) == FOURTH_PAGE_NUMBER) {
			View rootView = inflater.inflate(R.layout.fragment_logs, container, false);
			return rootView;
		}
		if (getArguments().getInt(ARG_SECTION_NUMBER) == THIRD_PAGE_NUMBER) {
			View rootView = inflater.inflate(R.layout.fragment_stats, container, false);
			return rootView;
		} else {
			return null;
		}
	}
}