package com.pushwoosh.demoapp.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.pushwoosh.Pushwoosh;
import com.pushwoosh.demoapp.R;
import com.pushwoosh.demoapp.databinding.FragmentHomeBinding;
import com.pushwoosh.demoapp.liveupdate.LiveUpdateDemo;
import com.pushwoosh.inapp.InAppManager;
import com.pushwoosh.tags.TagsBundle;

import java.util.Objects;

/**
 * Demonstrates core Pushwoosh SDK features for user engagement and data collection.
 * <p>
 * This fragment showcases the following use cases:
 * <ul>
 *   <li>Setting user tags for segmentation and targeting</li>
 *   <li>Registering user ID for cross-device tracking</li>
 *   <li>Posting in-app events to trigger campaigns</li>
 *   <li>Retrieving device identifiers (push token, HWID, user ID)</li>
 *   <li>Managing notification state</li>
 *   <li>Live Activities updates (iOS Live Activities analog for Android)</li>
 * </ul>
 *
 * @see Pushwoosh
 * @see InAppManager
 * @see TagsBundle
 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private boolean attributeState;
    private LiveUpdateDemo liveUpdateDemo;
    private boolean autoProgressEnabled = false;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
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
        MaterialSwitch attributes = binding.switch3;

        // GET PUSH TOKEN ELEMENTS
        Button getPushToken = binding.button4;

        // GET HWID ELEMENTS
        Button getHwid = binding.button5;

        // GET USER ID ELEMENTS
        Button getUserId = binding.button6;

        // GET APPLICATION CODE ELEMENTS
        Button getApplicationCode = binding.button7;

        // CLEAR NOTIFICATION CENTER ELEMENT
        Button clearNotificationCenter = binding.button8;

        attributes.setOnCheckedChangeListener((buttonView, isChecked) -> attributeState = isChecked);

        /*
         * Demonstrates setting user tags for segmentation.
         *
         * Use case: Associate custom key-value data with a user to enable targeted push campaigns.
         */
        setTags.setOnClickListener(v -> {
            String key = Objects.requireNonNull(textInput1.getText()).toString().trim();
            String value = Objects.requireNonNull(textInput2.getText()).toString().trim();

            if (key.isEmpty() || value.isEmpty()) {
                showSnackbar("Key and Value are required");
                return;
            }

            TagsBundle tag = new TagsBundle.Builder()
                    .putString(key, value)
                    .build();
            Pushwoosh.getInstance().setTags(tag, result -> {
                if (result.isSuccess()) {
                    showSnackbar("Tag set: " + key + " = " + value);
                } else {
                    showSnackbar("Error: " + result.getException());
                }
            });
        });

        /*
         * Demonstrates registering a user ID for cross-device tracking.
         *
         * Use case: Link user activity across multiple devices by assigning a unique user identifier.
         * This enables unified user profiles and consistent targeting regardless of which device the user is on.
         */
        registerUser.setOnClickListener(v -> {
            String user = Objects.requireNonNull(registerUserTextField.getText()).toString().trim();

            if (user.isEmpty()) {
                showSnackbar("User ID is required");
                return;
            }

            Pushwoosh.getInstance().setUserId(user, result -> {
                if (result.isSuccess()) {
                    showSnackbar("User registered: " + user);
                } else {
                    showSnackbar("Error: " + result.getException());
                }
            });
        });

        /*
         * Demonstrates posting in-app events to trigger campaigns.
         *
         * Use case: Track user actions (e.g., "purchase_completed", "level_up") to trigger
         * In-App Messages or other automated campaigns based on user behavior.
         * Optionally pass attributes to provide additional context for targeting.
         */
        postEvent.setOnClickListener(v -> {
            String eventName = Objects.requireNonNull(postEventTextField.getText()).toString().trim();

            if (eventName.isEmpty()) {
                showSnackbar("Event name is required");
                return;
            }

            TagsBundle eventAttributes = attributeState
                    ? new TagsBundle.Builder().putInt("price", 99).putString("currency", "USD").build()
                    : null;

            InAppManager.getInstance().postEvent(eventName, eventAttributes, result -> {
                if (result.isSuccess()) {
                    String msg = attributeState
                            ? "Event posted: " + eventName + " (with attributes)"
                            : "Event posted: " + eventName;
                    showSnackbar(msg);
                } else {
                    showSnackbar("Error: " + result.getException());
                }
            });
        });

        /*
         * Demonstrates retrieving the push notification token.
         *
         * Use case: Get the FCM/HMS token for debugging, logging, or sending to your own backend.
         */
        getPushToken.setOnClickListener(v -> {
            String pushToken = Pushwoosh.getInstance().getPushToken();
            showSnackbar("Push Token: " + (pushToken != null ? pushToken : "null"));
        });

        /*
         * Demonstrates retrieving the Hardware ID (HWID).
         *
         * Use case: Get the unique device identifier assigned by Pushwoosh for debugging
         * or matching devices in the Pushwoosh Control Panel.
         */
        getHwid.setOnClickListener(v -> {
            String hwid = Pushwoosh.getInstance().getHwid();
            showSnackbar("HWID: " + hwid);
        });

        /*
         * Demonstrates retrieving the current user ID.
         *
         * Use case: Verify which user ID is currently associated with the device.
         */
        getUserId.setOnClickListener(v -> {
            String userId = Pushwoosh.getInstance().getUserId();
            showSnackbar("User ID: " + (userId != null ? userId : "null"));
        });

        /*
         * Demonstrates retrieving the Pushwoosh application code.
         *
         * Use case: Verify which Pushwoosh app is configured, useful for debugging
         * multi-environment setups (dev/staging/prod).
         */
        getApplicationCode.setOnClickListener(v -> {
            String appCode = Pushwoosh.getInstance().getApplicationCode();
            showSnackbar("App Code: " + (appCode != null ? appCode : "null"));
        });

        /*
         * Demonstrates clearing the launch notification.
         *
         * Use case: Clear the cached push notification that launched the app.
         * Useful when you've already processed the deep link and don't want
         * getLaunchNotification() to return stale data.
         */
        clearNotificationCenter.setOnClickListener(v -> {
            Pushwoosh.getInstance().clearLaunchNotification();
            showSnackbar("Launch notification cleared");
        });

        setupLiveUpdateControls();

        return root;
    }

    /*
     * Demonstrates Live Update notifications (Android 15+ feature).
     *
     * Use case: Display real-time progress updates as prominent status bar chips,
     * similar to iOS Live Activities. Ideal for delivery tracking, ride-sharing,
     * sports scores, or any time-sensitive status updates.
     *
     * This demo simulates a food delivery with progress steps:
     * - Start: begins the Live Update with initial status
     * - Update: advances to next delivery step
     * - Finish: completes with success notification
     * - Cancel: dismisses the Live Update
     *
     * Note: This is a native Android feature, not a Pushwoosh API.
     * Pushwoosh can trigger Live Updates via push notifications.
     */
    private void setupLiveUpdateControls() {
        TextView statusText = binding.liveUpdateStatus;
        Button btnStart = binding.btnLiveUpdateStart;
        Button btnUpdate = binding.btnLiveUpdateUpdate;
        Button btnFinish = binding.btnLiveUpdateFinish;
        Button btnCancel = binding.btnLiveUpdateCancel;
        MaterialSwitch switchAutoProgress = binding.switchAutoProgress;

        // Defer heavy initialization to avoid blocking main thread during layout
        btnStart.post(() -> {
            liveUpdateDemo = LiveUpdateDemo.getInstance(requireContext());
            updateStatusDisplay(statusText);
        });

        switchAutoProgress.setOnCheckedChangeListener((buttonView, isChecked) -> autoProgressEnabled = isChecked);

        btnStart.setOnClickListener(v -> {
            if (liveUpdateDemo == null) return;
            liveUpdateDemo.start(autoProgressEnabled);
            updateStatusDisplay(statusText);
        });

        btnUpdate.setOnClickListener(v -> {
            if (liveUpdateDemo == null) return;
            liveUpdateDemo.update();
            updateStatusDisplay(statusText);
        });

        btnFinish.setOnClickListener(v -> {
            if (liveUpdateDemo == null) return;
            liveUpdateDemo.finish();
            updateStatusDisplay(statusText);
        });

        btnCancel.setOnClickListener(v -> {
            if (liveUpdateDemo == null) return;
            liveUpdateDemo.cancel();
            updateStatusDisplay(statusText);
        });
    }

    private void updateStatusDisplay(TextView statusText) {
        if (liveUpdateDemo != null && liveUpdateDemo.isRunning()) {
            int step = liveUpdateDemo.getCurrentStep() + 1;
            int total = liveUpdateDemo.getTotalSteps();
            statusText.setText(getString(R.string.live_update_status_running, step, total));
        } else {
            statusText.setText(R.string.live_update_status_not_started);
        }
    }

    private void showSnackbar(String message) {
        if (binding != null) {
            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}