//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.amazon.device.messaging;

import android.content.Context;
import android.content.Intent;

public abstract class ADMMessageHandlerJobBase {
    protected ADMMessageHandlerJobBase() {
        throw new RuntimeException("Stub! You are bundling a stubbed jar in the apk! Please move it to the classpath instead.");
    }

    protected abstract void onMessage(Context var1, Intent var2);

    protected abstract void onRegistrationError(Context var1, String var2);

    protected abstract void onRegistered(Context var1, String var2);

    protected abstract void onUnregistered(Context var1, String var2);
}
