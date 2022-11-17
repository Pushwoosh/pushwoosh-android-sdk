package com.pushwoosh.testingapp;

import androidx.annotation.NonNull;
import androidx.test.filters.LargeTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.tags.TagsBundle;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;


@LargeTest
@RunWith(AndroidJUnit4.class)
public class TagTest extends BaseTest {

    public static final int TIME_TEST_DATA = 36000000;

    @Before
    public void setUp() {

    }

    @Test(timeout = TIME_OUT)
    public void sendTag() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        TagsBundle tagsBundle = getTagsBundle();

        Pushwoosh.getInstance().sendTags(tagsBundle, result -> {
            Assert.assertTrue(result.isSuccess());
            countDownLatch.countDown();

        });
        countDownLatch.await();
    }

    private TagsBundle getTagsBundle() {
        List<String> stringList = getListString();
        Date date = new Date();
        date.setTime(TIME_TEST_DATA);
        return new TagsBundle.Builder()
                .putString("st_test", "test")
                .putInt("int_test", 12)
                .putBoolean("bool_test", true)
                .putLong("long_test", 100000000000L)
                .putList("list_test", stringList)
                .putDate("date_test", date)
                .build();
    }

    @NonNull
    private List<String> getListString() {
        List<String> stringList = new ArrayList<>();
        stringList.add("1");
        stringList.add("xcf");
        stringList.add("rut");
        return stringList;
    }

    @Test(timeout = TIME_OUT)
    public void getTagDefault() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        Pushwoosh.getInstance().getTags(result -> {
            Assert.assertTrue(result.isSuccess());
            assertTagsDefault(result.getData());
            countDownLatch.countDown();

        });
        countDownLatch.await();
    }

    private void assertTagsDefault(TagsBundle tags) {
        Assert.assertEquals("1.0.0.0", tags.getString("Application Version"));
        Assert.assertEquals(Locale.getDefault().getLanguage(), tags.getString("Language"));
        Assert.assertFalse(tags.getString("Country").isEmpty());
        Assert.assertFalse(tags.getString("City").isEmpty());
        Assert.assertNotEquals(0, tags.getLong("First Install", 0));
    }

    // need config tags on server
    @Test(timeout = TIME_OUT)
    public void getTag() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        TagsBundle tagsBundle = getTagsBundle();

        Pushwoosh.getInstance().sendTags(tagsBundle, result -> {
            Assert.assertTrue(result.isSuccess());
            getTagCheck(countDownLatch);
        });
        countDownLatch.await();
    }

    private void getTagCheck(CountDownLatch countDownLatch){
        Pushwoosh.getInstance().getTags(result -> {
            Assert.assertTrue(result.isSuccess());
            assertTags(result.getData());
            countDownLatch.countDown();

        });
    }

    private void assertTags(TagsBundle tags) {
        Assert.assertEquals("test", tags.getString("st_test"));
        Assert.assertEquals(12, tags.getInt("int_test",0));
        Assert.assertEquals(100000000000L, tags.getLong("long_test",0));
        Assert.assertEquals(getListString(), tags.getList("list_test"));
        Assert.assertNotEquals(0, tags.getLong("date_test", 0));
        Assert.assertEquals(1, tags.getInt("bool_test",0));
    }


}
