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

package com.pushwoosh.richmedia;

import com.pushwoosh.PushwooshPlatform;
import com.pushwoosh.inapp.view.config.ModalRichmediaConfig;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaWindowWidth;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.repository.NotificationPrefs;
import com.pushwoosh.repository.RepositoryModule;

/**
 * `RichMediaManager` class is a manager responsible for Rich Media presentation.
 */
public class RichMediaManager {
    public static void setDelegate(RichMediaPresentingDelegate delegate) {
        // workaround for PUSH-31718
        // on some Lenovo devices PushwooshPlatform instance is not created when delegate is being set
        PushwooshPlatform pushwooshPlatform = PushwooshPlatform.getInstance();
        if (pushwooshPlatform == null) {
            PWLog.error("RichMediaManager failed to set delegate as PushwooshPlatform is not initialized yet");
            return;
        }
        RichMediaController richMediaController = pushwooshPlatform.getRichMediaController();
        if (richMediaController != null)
            richMediaController.setDelegate(delegate);
    }

    public static void present(RichMedia richMedia) {
        RichMediaController richMediaController = PushwooshPlatform.getInstance().getRichMediaController();
        if (richMediaController != null)
            richMediaController.present(richMedia);
    }

    public static RichMediaStyle getRichMediaStyle() {
        RichMediaController richMediaController = PushwooshPlatform.getInstance().getRichMediaController();
        if (richMediaController != null) {
            return richMediaController.getRichMediaStyle();
        } else {
            return null;
        }
    }

    public static void setDefaultRichMediaConfig(ModalRichmediaConfig config) {
        NotificationPrefs prefs = RepositoryModule.getNotificationPreferences();

        prefs.richMediaDismissAnimation().set(config.getDismissAnimationType().getCode());
        prefs.richMediaPresentAnimation().set(config.getPresentAnimationType().getCode());
        prefs.richMediaSwipeGesture().set(config.getSwipeGesture().getCode());
        prefs.richMediaViewPosition().set(config.getViewPosition().getCode());
        prefs.richMediaWindowWidth().set(config.getWindowWidth().getCode());
        prefs.richMediaStatusBarCovered().set(config.isStatusBarCovered());
        prefs.richMediaAnimationDuration().set(config.getAnimationDuration());
    }

    public static ModalRichmediaConfig getDefaultRichMediaConfig() {
        NotificationPrefs prefs = RepositoryModule.getNotificationPreferences();
        ModalRichmediaConfig config = new ModalRichmediaConfig()
                .setAnimationDuration(prefs.richMediaAnimationDuration().get())
                .setDismissAnimationType(ModalRichMediaDismissAnimationType.getByCode(prefs.richMediaDismissAnimation().get()))
                .setPresentAnimationType(ModalRichMediaPresentAnimationType.getByCode(prefs.richMediaPresentAnimation().get()))
                .setSwipeGesture(ModalRichMediaSwipeGesture.getByCode(prefs.richMediaSwipeGesture().get()))
                .setViewPosition(ModalRichMediaViewPosition.getByCode(prefs.richMediaViewPosition().get()))
                .setWindowWidth(ModalRichMediaWindowWidth.getByCode(prefs.richMediaWindowWidth().get()))
                .setStatusBarCovered(prefs.richMediaStatusBarCovered().get());
        return config;
    }
}
