package com.pushwoosh.testingapp.inline_inapp;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.pushwoosh.testingapp.R;

public class InlineInAppActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inline_inapp_activity);

        setButtonOpenScreen(R.id.dinamic_layout, DinamicLayoutActvity.class);
        setButtonOpenScreen(R.id.static_layout, StaticLayoutActivity.class);
        setButtonOpenScreen(R.id.top_banner, TopBannerActivity.class);
        setButtonOpenScreen(R.id.bottom_banner, BottomBannerActivity.class);
    }

    private void setButtonOpenScreen(@IdRes int resId, Class activityClass) {
        findViewById(resId).setOnClickListener(v -> {
            Intent intent = new Intent(getApplication(), activityClass);
            startActivity(intent);
        });
    }
}
