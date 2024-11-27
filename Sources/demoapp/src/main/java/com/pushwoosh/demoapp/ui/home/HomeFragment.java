package com.pushwoosh.demoapp.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.pushwoosh.Pushwoosh;
import com.pushwoosh.demoapp.databinding.FragmentHomeBinding;
import com.pushwoosh.inapp.PushwooshInApp;
import com.pushwoosh.tags.TagsBundle;

import java.util.Objects;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private boolean attributeState;

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // SET TAGS ELEMENTS
        TextInputEditText textInput1 = binding.textInput1;
        TextInputEditText textInput2 = binding.textInput2;
        Button setTags = binding.button;

        // REGISTER USER ELEMENTS
        TextInputEditText registerUserTextField = binding.textInput3;
        Button registerUser = binding.button2;

        // POST EVENT ELEMENTS
        TextInputEditText postEventTextField = binding.textInput4;
        Button postEvent = binding.button3;
        Switch attributes = binding.switch3;

        // SET LANGUAGE ELEMENTS
        TextInputEditText setLanguageTextField = binding.textInput5;
        Button  setLanguage = binding.button4;

        // GET PUSH TOKEN ELEMENTS
        Button getPushToken = binding.button5;

        // GET HWID ELEMENTS
        Button getHwid = binding.button6;

        // GET USER ID ELEMENTS
        Button getUserId = binding.button7;

        // GET APPLICATION CODE ELEMENTS
        Button getApplicationCode = binding.button8;

        // CLEAR NOTIFICATION CENTER ELEMENT
        Button clearNotificationCenter = binding.button9;

        attributes.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    attributeState = true;
                } else {
                    attributeState = false;
                }
            }
        });

        setTags.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value1 = Objects.requireNonNull(textInput1.getText()).toString();
                String value2 = Objects.requireNonNull(textInput2.getText()).toString();
                int intValue2 = Integer.parseInt(value2);
                TagsBundle tag = new TagsBundle.Builder().putInt(value1, intValue2)
                        .build();
                Pushwoosh.getInstance().setTags(tag);
            }
        });

        registerUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = Objects.requireNonNull(registerUserTextField.getText()).toString();
                Pushwoosh.getInstance().setUserId(user);
            }
        });

        postEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String eventName = Objects.requireNonNull(postEventTextField.getText()).toString();
                if (attributeState) {
                    String value1 = "attributes";
                    int value2 = 10;
                    TagsBundle attributes = new TagsBundle.Builder().putInt(value1, value2)
                            .build();
                    PushwooshInApp.getInstance().postEvent(eventName, attributes);
                } else {
                    PushwooshInApp.getInstance().postEvent(eventName);
                }
            }
        });

        setLanguage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String language = Objects.requireNonNull(setLanguageTextField.getText()).toString();
                Pushwoosh.getInstance().setLanguage(language);
            }
        });

        getPushToken.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String pushToken = Pushwoosh.getInstance().getPushToken();
                Log.d("", "Push Token = " + pushToken);
            }
        });

        getHwid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String hwid = Pushwoosh.getInstance().getHwid();
                Log.d("", "HWID = " + hwid);
            }
        });

        getUserId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userId = Pushwoosh.getInstance().getUserId();
                Log.d("", "USER ID = " + userId);
            }
        });

        getApplicationCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String applicationCode = Pushwoosh.getInstance().getApplicationCode();
                Log.d("", "APPLICATION CODE = " + applicationCode);
            }
        });

        clearNotificationCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Pushwoosh.getInstance().clearLaunchNotification();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}