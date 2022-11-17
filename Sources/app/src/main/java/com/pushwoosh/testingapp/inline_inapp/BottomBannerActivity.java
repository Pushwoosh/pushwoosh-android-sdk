package com.pushwoosh.testingapp.inline_inapp;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.pushwoosh.testingapp.R;

import java.util.ArrayList;
import java.util.List;

public class BottomBannerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inline_inapp_bottom_banner_layout);
        initListView();
    }

    protected void initListView() {
        ListView listView = findViewById(R.id.list_view);
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(Integer.toString(i));
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, list);
        listView.setAdapter(adapter);
    }
}
