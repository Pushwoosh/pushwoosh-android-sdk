package com.pushwoosh.testingapp.layout;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.pushwoosh.Pushwoosh;
import com.pushwoosh.notification.SoundType;
import com.pushwoosh.notification.VibrateType;
import com.pushwoosh.testingapp.AppData;
import com.pushwoosh.testingapp.AppPreferencesStrings;
import com.pushwoosh.testingapp.PlaceholderFragment;
import com.pushwoosh.testingapp.R;
import com.pushwoosh.testingapp.helpers.AppPreferences;
import com.pushwoosh.testingapp.helpers.ShowMessageHelper;
import com.pushwoosh.testingapp.proxy.PushwooshProxyController;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsFragment extends PlaceholderFragment {
    private View view;
    private AppData appData;
    private Boolean fragmentVisible;
    private ArrayAdapter<CharSequence> adapter;

    @BindView(R.id.registerForPushNotificationsSwitch)
    SwitchCompat registerForPushNotificationsSwitch;

    @BindView(R.id.appIdEditText)
    EditText appIdEditText;

    @BindView(R.id.BasePushMessageReceiverSwitch)
    SwitchCompat basePushMessageReceiverSwitch;

    @BindView(R.id.setCustomNotificationFactorySwitch)
    SwitchCompat setCustomNotificationFactorySwitch;

    @BindView(R.id.setPostEventAttributesSwitch)
    SwitchCompat setPostEventAttributesSwitch;

    @BindView(R.id.setLightScreenOnNotificationSwitch)
    SwitchCompat setLightScreenOnNotificationSwitch;

    @BindView(R.id.setMultiNotificationMode)
    SwitchCompat setMultiNotificationMode;

    @BindView(R.id.setEnableLEDSwitch)
    SwitchCompat setEnableLEDSwitch;

    @BindView(R.id.startTrackingGeoPushesSwitch)
    SwitchCompat startTrackingGeoPushesSwitch;

    @BindView(R.id.setSoundNotificationTypeSpinner)
    Spinner setSoundNotificationTypeSpinner;

    @BindView(R.id.setVibrateNotificationTypeSpinner)
    Spinner setVibrateNotificationTypeSpinner;

    @BindView(R.id.setColorLEDEditor)
    Spinner setColorLEDEditor;


    private boolean manual = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        appData = AppData.getInstance();
        view = inflater.inflate(R.layout.fragment_settings, container, false);
        AppPreferences.setDefaults();
        ButterKnife.bind(this, view);

        adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.setSoundNotificationTypeArray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        setSpinners();
        setSwitches();

        appIdEditText.setOnEditorActionListener((v, actionId, event) -> {
            Pushwoosh.getInstance().setAppId(v.getText().toString());
            Pushwoosh.getInstance().registerForPushNotifications(result -> {
                if (result.getException() != null) {
                    result.getException().printStackTrace();
                }
            });
            return true;
        });

        return view;
    }


    private void setSpinners() {
        setSoundNotificationTypeSpinner.setAdapter(adapter);
        setSoundNotificationTypeSpinner.setSelection(0, false);
        setSoundNotificationTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View v, int i, long l) {
                PushwooshProxyController.getPushwooshProxy().setSoundNotificationType(SoundType.fromInt(i));
                String str = "setSoundNotificationType: " + SoundType.fromInt(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        setVibrateNotificationTypeSpinner.setAdapter(adapter);
        setVibrateNotificationTypeSpinner.setSelection(0, false);
        setVibrateNotificationTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View v, int i, long l) {
                PushwooshProxyController.getPushwooshProxy().setVibrateNotificationType(VibrateType.fromInt(i));
                String str = "setVibrateNotificationType: " + VibrateType.fromInt(i);
                ShowMessageHelper.log(str);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        adapter = ArrayAdapter.createFromResource(getContext(), R.array.colorsArray,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        setColorLEDEditor.setAdapter(adapter);
        setColorLEDEditor.setSelection(0, false);
        setColorLEDEditor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View v, int i, long l) {
                String colorString = adapterView.getSelectedItem().toString();
                int color = Color.parseColor(colorString);
                PushwooshProxyController.getPushwooshProxy().setColorLED(color);
                String str = "setColorLED: " + colorString + ",(int: " + color + ")";
                ShowMessageHelper.log(str);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    private void setSwitches() {

        registerForPushNotificationsSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            String str;
            if (b) {
                PushwooshProxyController.getPushwooshProxy().registerForPushNotifications();
                str = "registerForPushNotifications";
            } else {
                PushwooshProxyController.getPushwooshProxy().unregisterForPushNotifications();
                str = "unregisterForPushNotifications";
            }
            ShowMessageHelper.log(str);
        });

        registerForPushNotificationsSwitch.setContentDescription("registerSwitch");

        basePushMessageReceiverSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            String str = "handlePushInForeground" + b;
            AppData.getInstance().setHandleInForeground(b);
            ShowMessageHelper.log(str);
        });

        basePushMessageReceiverSwitch.setContentDescription("handleForegroundSwitch");

        setCustomNotificationFactorySwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            AppData.getInstance().setCustomNotifications(b);
            String str = "setCustomNotificationFactorySwitch: " + b;
            ShowMessageHelper.log(str);
        });

        setPostEventAttributesSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            appData.setPostEventAttributes(b);
            String str = "setPostEventAttributesSwitch: " + b;
            ShowMessageHelper.log(str);
        });

        setMultiNotificationMode.setOnCheckedChangeListener((compoundButton, b) -> {
            String str = "";
            if (!setMultiNotificationMode.isChecked()) {
                PushwooshProxyController.getPushwooshProxy().setSimpleNotificationMode();
                str = "setSimpleNotificationMode: " + setMultiNotificationMode.isChecked();
            }
            if (setMultiNotificationMode.isChecked()) {
                PushwooshProxyController.getPushwooshProxy().setMultiNotificationMode();
                str = "setMultiNotificationMode: " + setMultiNotificationMode.isChecked();
            }
            ShowMessageHelper.log(str);
        });

        setLightScreenOnNotificationSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            PushwooshProxyController.getPushwooshProxy().setLightScreenOnNotification(b);
            String str = "setLightScreenOnNotification: " + b;
            ShowMessageHelper.log(str);
        });

        setEnableLEDSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            PushwooshProxyController.getPushwooshProxy().setEnableLED(b);
            String str = "setEnableLED: " + b;
            ShowMessageHelper.log(str);
        });

        startTrackingGeoPushesSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b) {
                if (manual) {
                    PushwooshProxyController.getPushwooshProxy().startTrackingGeoPushes(result -> {
                        if (!result.isSuccess()) {
                            manual = false;
                            startTrackingGeoPushesSwitch.setChecked(false);
                            manual = true;
                        }
                    });
                    String str = "startTrackingGeoPushes";
                    ShowMessageHelper.log(str);
                }
            } else {
                if (manual) {
                    PushwooshProxyController.getPushwooshProxy().stopTrackingGeoPushes();
                    String str = "stopTrackingGeoPushes";
                    ShowMessageHelper.log(str);
                }
            }
            AppPreferences.saveBool(AppPreferencesStrings.START_TRACKING_GEO_PUSHES_SWITCH, b);
        });

        startTrackingGeoPushesSwitch.setContentDescription("geopushesSwitch");
    }

    private void saveSettings() {
        AppPreferences.saveBool(AppPreferencesStrings.PREFERENCES_EXIST, true);
        AppPreferences.saveBool(AppPreferencesStrings.REGISTER_FOR_PUSH_NOTIFICATIONS_SWITCH, registerForPushNotificationsSwitch.isChecked());
        AppPreferences.saveBool(AppPreferencesStrings.BASE_PUSH_MESSAGE_RECEIVER_SWITCH, basePushMessageReceiverSwitch.isChecked());
        AppPreferences.saveBool(AppPreferencesStrings.SET_LIGHT_SCREEN_ON_NOTIFICATION_SWITCH, setLightScreenOnNotificationSwitch.isChecked());
        AppPreferences.saveBool(AppPreferencesStrings.SET_MULTI_NOTIFICATION_MODE, setMultiNotificationMode.isChecked());
        AppPreferences.saveBool(AppPreferencesStrings.SET_ENABLED_LED_SWITCH, setEnableLEDSwitch.isChecked());
        AppPreferences.saveBool(AppPreferencesStrings.START_TRACKING_GEO_PUSHES_SWITCH, startTrackingGeoPushesSwitch.isChecked());
        AppPreferences.saveBool(AppPreferencesStrings.SET_CUSTOM_NOTIFICATION_FACTORY_SWITCH, setCustomNotificationFactorySwitch.isChecked());
        AppPreferences.saveBool(AppPreferencesStrings.SET_POST_EVENT_ATTRIBUTES_SWITCH, setPostEventAttributesSwitch.isChecked());
        AppPreferences.saveInt(AppPreferencesStrings.SET_SOUND_NOTIFICATION_TYPE_SPINNER, setSoundNotificationTypeSpinner.getSelectedItemPosition());
        AppPreferences.saveInt(AppPreferencesStrings.SET_VIBRATE_NOTIFICATION_TYPE_SPINNER, setVibrateNotificationTypeSpinner.getSelectedItemPosition());
        AppPreferences.saveInt(AppPreferencesStrings.SET_COLOR_LED_EDITOR, setColorLEDEditor.getSelectedItemPosition());
    }

    private void loadSettings() {
        AppPreferences.loadInt(AppPreferencesStrings.SET_VIBRATE_NOTIFICATION_TYPE_SPINNER, setVibrateNotificationTypeSpinner);
        AppPreferences.loadInt(AppPreferencesStrings.SET_COLOR_LED_EDITOR, setColorLEDEditor);
        AppPreferences.loadInt(AppPreferencesStrings.SET_SOUND_NOTIFICATION_TYPE_SPINNER, setSoundNotificationTypeSpinner);
        AppPreferences.loadBool(AppPreferencesStrings.REGISTER_FOR_PUSH_NOTIFICATIONS_SWITCH, registerForPushNotificationsSwitch);
        AppPreferences.loadBool(AppPreferencesStrings.BASE_PUSH_MESSAGE_RECEIVER_SWITCH, basePushMessageReceiverSwitch);
        AppPreferences.loadBool(AppPreferencesStrings.SET_LIGHT_SCREEN_ON_NOTIFICATION_SWITCH, setLightScreenOnNotificationSwitch);
        AppPreferences.loadBool(AppPreferencesStrings.SET_MULTI_NOTIFICATION_MODE, setMultiNotificationMode);
        AppPreferences.loadBool(AppPreferencesStrings.SET_ENABLED_LED_SWITCH, setEnableLEDSwitch);
        AppPreferences.loadBool(AppPreferencesStrings.START_TRACKING_GEO_PUSHES_SWITCH, startTrackingGeoPushesSwitch);
        AppPreferences.loadBool(AppPreferencesStrings.SET_CUSTOM_NOTIFICATION_FACTORY_SWITCH, setCustomNotificationFactorySwitch);
        AppPreferences.loadBool(AppPreferencesStrings.SET_POST_EVENT_ATTRIBUTES_SWITCH, setPostEventAttributesSwitch);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            fragmentVisible = true;
        } else {
            fragmentVisible = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        saveSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!appData.getFirstRun()) {
            loadSettings();
            appData.setPostEventAttributes(setPostEventAttributesSwitch.isChecked());
        }

        appIdEditText.setText(Pushwoosh.getInstance().getApplicationCode());
    }
}
