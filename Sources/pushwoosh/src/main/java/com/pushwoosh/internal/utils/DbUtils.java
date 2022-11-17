package com.pushwoosh.internal.utils;

import android.text.TextUtils;

public class DbUtils {
    public static String[] getSelectionArgs(String... args){
        if (args == null || args.length == 0){
            return  null;
        }
        String [] selectionArgs = new String[args.length];

        for (int i = 0; i < args.length; i++){
            selectionArgs[i] = TextUtils.isEmpty(args[i]) ? "'null'" : args[i];
        }

        return selectionArgs;
    }
}