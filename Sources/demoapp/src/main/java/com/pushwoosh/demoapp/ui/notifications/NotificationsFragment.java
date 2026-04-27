package com.pushwoosh.demoapp.ui.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.pushwoosh.Pushwoosh;
import com.pushwoosh.demoapp.BuildConfig;
import com.pushwoosh.demoapp.R;
import com.pushwoosh.demoapp.databinding.FragmentNotificationsBinding;
import com.pushwoosh.inapp.view.config.ModalRichmediaConfig;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaDismissAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaPresentAnimationType;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaSwipeGesture;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaViewPosition;
import com.pushwoosh.inapp.view.config.enums.ModalRichMediaWindowWidth;
import com.pushwoosh.location.PushwooshLocation;
import com.pushwoosh.richmedia.RichMediaManager;
import com.pushwoosh.richmedia.RichMediaType;

import java.util.Collections;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        NotificationsViewModel notificationsViewModel = new ViewModelProvider(this).get(NotificationsViewModel.class);

        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.aboutSdkVersion.setText(com.pushwoosh.BuildConfig.VERSION_NAME);
        binding.aboutBuildType.setText(BuildConfig.BUILD_TYPE);

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

        // Switches ModalRichmediaConfig.viewPosition at runtime so Phase 2 audit (Task 2.1.e)
        // can exercise all four popup positions (TOP / CENTER / BOTTOM / FULLSCREEN) without
        // rebuilding the demo app. Default is FULLSCREEN to match MainActivity.setupPushwooshSdk().
        RadioGroup modalPositionGroup = binding.modalPositionGroup;
        modalPositionGroup.setOnCheckedChangeListener((group, checkedId) -> {
            ModalRichMediaViewPosition position;
            if (checkedId == R.id.modalPositionTop) {
                position = ModalRichMediaViewPosition.TOP;
            } else if (checkedId == R.id.modalPositionCenter) {
                position = ModalRichMediaViewPosition.CENTER;
            } else if (checkedId == R.id.modalPositionBottom) {
                position = ModalRichMediaViewPosition.BOTTOM;
            } else {
                position = ModalRichMediaViewPosition.FULLSCREEN;
            }
            RichMediaManager.setDefaultRichMediaConfig(new ModalRichmediaConfig()
                    .setViewPosition(position)
                    .setPresentAnimationType(ModalRichMediaPresentAnimationType.SLIDE_UP)
                    .setDismissAnimationType(ModalRichMediaDismissAnimationType.SLIDE_DOWN)
                    .setSwipeGestures(Collections.singleton(ModalRichMediaSwipeGesture.NONE))
                    .setWindowWidth(ModalRichMediaWindowWidth.FULL_SCREEN)
                    .setStatusBarCovered(true)
                    .setAnimationDuration(300));
        });

        MaterialSwitch locationTracking = binding.switchLocationTracking;
        locationTracking.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    PushwooshLocation.startLocationTracking(result -> {
                        if (binding == null) return;
                        if (result.isSuccess()) {
                            Snackbar.make(binding.getRoot(), "Location tracking started", Snackbar.LENGTH_SHORT)
                                    .show();
                        } else {
                            locationTracking.setChecked(false);
                            Snackbar.make(
                                            binding.getRoot(),
                                            "Location error: "
                                                    + result.getException().getMessage(),
                                            Snackbar.LENGTH_SHORT)
                                    .show();
                        }
                    });
                } else {
                    PushwooshLocation.stopLocationTracking();
                }
            }
        });

        binding.buttonBackgroundLocation.setOnClickListener(v -> {
            PushwooshLocation.requestBackgroundLocationPermission();
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
