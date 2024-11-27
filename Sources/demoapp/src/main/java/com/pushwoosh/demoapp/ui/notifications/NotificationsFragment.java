package com.pushwoosh.demoapp.ui.notifications;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.demoapp.databinding.FragmentNotificationsBinding;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch registerForRemoteNotification = binding.switch1;
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch communicationServerEnable = binding.switch2;

        registerForRemoteNotification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Pushwoosh.getInstance().registerForPushNotifications();
                } else {
                    Pushwoosh.getInstance().unregisterForPushNotifications();
                }
            }
        });

        communicationServerEnable.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Pushwoosh.getInstance().startServerCommunication();
                } else {
                    Pushwoosh.getInstance().stopServerCommunication();
                }
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
