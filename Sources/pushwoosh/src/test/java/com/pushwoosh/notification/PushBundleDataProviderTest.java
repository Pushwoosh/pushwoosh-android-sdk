/*
 *
 * Copyright (c) 2018. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.notification;

import android.os.Bundle;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by aevstefeev on 13/03/2018.
 */
@RunWith(RobolectricTestRunner.class)
public class PushBundleDataProviderTest {
    @Test
    public void getActions() throws Exception {
        Bundle bundle = new Bundle();
        Collection<Action> actions = PushBundleDataProvider.getActions(bundle);
        Assert.assertTrue(actions.isEmpty());

        bundle.putString("pw_actions", "[{title:\"action1\", type:\"ACTIVITY\"},{title:\"action2\",type:\"ACTIVITY\"},{title:\"action3\",type:\"ACTIVITY\"}]");
        actions = PushBundleDataProvider.getActions(bundle);
        Assert.assertEquals(3, actions.size());
    }

    @Test
    public void isSilent() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("silent", "true");
        boolean result = PushBundleDataProvider.isSilent(bundle);
        Assert.assertTrue(result);

        Bundle bundle2 = new Bundle();
        bundle2.putString("pw_silent", "false");
        boolean result2 = PushBundleDataProvider.isSilent(bundle2);
        Assert.assertFalse(result2);
    }

    @Test
    public void getPriority() throws Exception {
        Bundle bundle = new Bundle();
        int result = PushBundleDataProvider.getPriority(bundle);
        Assert.assertEquals(0, result);

        bundle.putString("pri", "2");
        int result2 = PushBundleDataProvider.getPriority(bundle);
        Assert.assertEquals(2, result2);

        bundle = new Bundle();
        bundle.putString("pri","5");
        int result3 = PushBundleDataProvider.getPriority(bundle);
        Assert.assertEquals(0, result3);

        bundle = new Bundle();
        bundle.putString("pri", "ewrw");
        int result4 = PushBundleDataProvider.getPriority(bundle);
        Assert.assertEquals(0, result4);
    }




}