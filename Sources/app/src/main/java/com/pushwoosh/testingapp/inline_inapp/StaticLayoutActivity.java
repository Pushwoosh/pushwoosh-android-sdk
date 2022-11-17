package com.pushwoosh.testingapp.inline_inapp;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.pushwoosh.testingapp.R;

import java.util.ArrayList;
import java.util.List;

public class StaticLayoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.inline_inapp_static_layout);

        initListView();
    }

    protected void initListView() {
        ListView listView = findViewById(R.id.list_view);
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            list.add(Integer.toString(i));
        }



        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, list) {
            View inAppView;

            @Override
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public int getItemViewType(int position) {
                return position == 15 ? 1 : 0;
            }

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (position == 15) {
                    if (inAppView == null) {
                        LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        if (inflater != null)
                            inAppView = inflater.inflate(R.layout.inline_inapp_list_item, parent, false);
                    }
                    return inAppView;
                }
                return super.getView(position, convertView, parent);
            }
        };
        listView.setAdapter(adapter);

    }
}
