package com.pushwoosh.testingapp;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.pushwoosh.inbox.ui.presentation.view.activity.InboxActivity;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasEntry;

@LargeTest
@RunWith(AndroidJUnit4.class)
@Ignore
public class InboxUiTest extends BaseTest {

    @Rule
    public ActivityTestRule<InboxActivity> activityActivityTestRule = new ActivityTestRule<InboxActivity>(InboxActivity.class);




    @Before
    public void setUp() {

    }



    @Test
    @Ignore
    public void inboxUiTest() throws InterruptedException {
        clearInbox();
        TimeUnit.SECONDS.sleep(4);
        sendPush("message1", null, true);
        TimeUnit.SECONDS.sleep(2);


    }


}
