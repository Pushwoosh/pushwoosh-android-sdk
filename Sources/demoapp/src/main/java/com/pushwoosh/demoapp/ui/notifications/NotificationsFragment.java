package com.pushwoosh.demoapp.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.pushwoosh.Pushwoosh;
import com.pushwoosh.demoapp.databinding.FragmentNotificationsBinding;
import com.pushwoosh.richmedia.RichMediaManager;
import com.pushwoosh.richmedia.RichMediaType;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel =
                new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        MaterialSwitch registerForRemoteNotification = binding.switch1;
        MaterialSwitch communicationServerEnable = binding.switch2;
        MaterialSwitch modalRichMediaEnabled = binding.switchRichMediaType;

        // Set checked status for registerForRemoteNotification switch, if the device is registered
        String pushToken = Pushwoosh.getInstance().getPushToken();
        registerForRemoteNotification.setChecked(pushToken != null && !pushToken.isEmpty());

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

        // Set checked status for modalRichMediaEnabled switch, if Modal Rich Media is set
        modalRichMediaEnabled.setChecked(RichMediaManager.getRichMediaType() == RichMediaType.MODAL);

        modalRichMediaEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    RichMediaManager.setRichMediaType(RichMediaType.MODAL);
                } else {
                    RichMediaManager.setRichMediaType(RichMediaType.DEFAULT);
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
