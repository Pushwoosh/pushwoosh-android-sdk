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

package com.pushwoosh.testingapp;

import androidx.test.espresso.web.model.Atom;
import androidx.test.espresso.web.model.ElementReference;
import androidx.test.espresso.web.webdriver.DriverAtoms;
import androidx.test.espresso.web.webdriver.Locator;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.pushwoosh.GDPRManager;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.web.assertion.WebViewAssertions.webMatches;
import static androidx.test.espresso.web.sugar.Web.onWebView;
import static androidx.test.espresso.web.webdriver.DriverAtoms.findElement;
import static androidx.test.espresso.web.webdriver.DriverAtoms.getText;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webClick;
import static androidx.test.espresso.web.webdriver.DriverAtoms.webScrollIntoView;
import static org.hamcrest.CoreMatchers.containsString;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class GdprUITest {

    @Test
    public void showCommunicationUI() throws InterruptedException {
        GDPRManager.getInstance().showGDPRConsentUI();
        TimeUnit.SECONDS.sleep(5);
        checkMainElements();

    }

    @Test
    public void enableCommunication() throws InterruptedException {
        GDPRManager.getInstance().showGDPRConsentUI();
        TimeUnit.SECONDS.sleep(5);
        onWebView()
                .withElement(findElement(Locator.CLASS_NAME, "button"))
                .perform(webClick());
    }

    @Test
    public void disableCommunication() throws InterruptedException {
        GDPRManager.getInstance().showGDPRConsentUI();
        TimeUnit.SECONDS.sleep(2);
        Atom<ElementReference> checkbox = findElement(Locator.ID, "checkbox");
        String script = checkbox.getScript();
        onWebView()
                .withElement(checkbox)
                .perform(webScrollIntoView())
                .check(webMatches(getText(), containsString("")));
        TimeUnit.SECONDS.sleep(5);
        onWebView()
                .withElement(findElement(Locator.CLASS_NAME, "button"))
                .perform(webClick());
    }

    @Test
    public void showGDPRDeletionUI() throws InterruptedException {
        GDPRManager.getInstance().showGDPRDeletionUI();
        TimeUnit.SECONDS.sleep(2);
        checkMainElements();
    }

    private void checkMainElements() {
        Atom<ElementReference> button = findElement(Locator.CLASS_NAME, "button");
        Atom<ElementReference> checkbox = findElement(Locator.ID, "checkbox");
        onWebView()
                .withElement(button);
        onWebView()
                .withElement(checkbox);
    }
}
