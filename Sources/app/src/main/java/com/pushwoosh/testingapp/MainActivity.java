package com.pushwoosh.testingapp;

import android.content.Context;
import android.os.Bundle;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;

import com.pushwoosh.inapp.view.config.ModalRichmediaConfig;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.inbox.data.InboxMessage;
import com.pushwoosh.inbox.ui.OnInboxMessageClickListener;
import com.pushwoosh.inbox.ui.PushwooshInboxUi;
import com.pushwoosh.inbox.ui.PushwooshInboxStyle;
import com.pushwoosh.richmedia.RichMediaManager;
import com.pushwoosh.testingapp.helpers.ShowMessageHelper;

import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements OnInboxMessageClickListener {
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private SwipeViewPager mViewPager;

    @Override
    public void onInboxMessageClick(InboxMessage message) {
        Log.d("Inbox", "message clicked");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PushwooshInboxStyle.INSTANCE.setDefaultImageIcon(R.drawable.common_google_signin_btn_icon_dark);
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);
        TabLayout.Tab settingsTab = tabLayout.getTabAt(1);
        settingsTab.setContentDescription("settingsTabBarItem");

        AppData.getInstance();
        ButterKnife.bind(this);
        BusStation.getBus().register(this);

        String appOpen = AppPreferencesStrings.APP_OPEN;

        PushwooshInboxUi.INSTANCE.setOnMessageClickListener(this);
        RichMediaManager.setDefaultRichMediaConfig(new ModalRichmediaConfig()
                .setSwipeGesture(ModalRichMediaSwipeGesture.DOWN)
                .setViewPosition(ModalRichMediaViewPosition.BOTTOM)
                .setDismissAnimationType(ModalRichMediaDismissAnimationType.FADE_OUT)
                .setPresentAnimationType(ModalRichMediaPresentAnimationType.SLIDE_UP)
                .setAnimationDuration(2000));

        ShowMessageHelper.setMessage("Application ready");
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                final InputMethodManager imm = (InputMethodManager) getSystemService(
                        Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mViewPager.getWindowToken(), 0);
                mViewPager.setPagingEnabled(!mSectionsPagerAdapter.getPageTitle(position).equals("Inbox"));
            }

            @Override
            public void onPageScrolled(int position, float offset, int offsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    @Override
    protected void onUserLeaveHint() {
        ShowMessageHelper.cancelToast();
        super.onUserLeaveHint();
    }


    @Override
    public void onBackPressed() {
        if (mViewPager.getCurrentItem() == 0) {
            super.onBackPressed();
        }
        mViewPager.setCurrentItem(0);
    }
}
