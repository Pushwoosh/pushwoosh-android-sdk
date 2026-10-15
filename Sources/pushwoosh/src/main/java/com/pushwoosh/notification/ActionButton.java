package com.pushwoosh.notification;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

/**
 * A notification action button defined by the server in the {@code pw_buttons} payload field.
 * <p>
 * Unlike the legacy {@link Action} contract, an action button is cross-platform and its tap is
 * handled by the SDK itself: the notification is dismissed, a {@code pushStat} open event carrying
 * the button id is sent, the action is performed and
 * {@link NotificationServiceExtension#onMessageOpened(PushMessage)} is called with a message whose
 * {@link PushMessage#getTappedButtonId()} returns this button's id.
 * <p>
 * Buttons are read through {@link PushMessage#getButtons()}. A push may carry at most three of them;
 * malformed buttons are skipped without affecting the rest of the push.
 *
 * @see PushMessage#getButtons()
 * @see PushMessage#getTappedButtonId()
 */
public class ActionButton {

    /**
     * What tapping the button does.
     */
    public enum ActionType {
        /** Bring the application to the foreground, exactly like a tap on the notification body. */
        OPEN,
        /** Open the button's {@link ActionButton#getUrl() url}. */
        URL,
        /** Open the button's {@link ActionButton#getUrl() url} as a deep link. */
        DEEPLINK,
        /** Deliver the tap to the application in the background without opening any screen. */
        CUSTOM;

        @Nullable static ActionType parse(@Nullable String value) {
            if (value == null) {
                return null;
            }
            for (ActionType type : values()) {
                if (type.name().equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * Visual emphasis requested by the marketer. Android does not tint notification actions,
     * so this value is parsed and exposed but never rendered.
     */
    public enum Style {
        DEFAULT,
        DESTRUCTIVE;

        @NonNull static Style parse(@Nullable String value) {
            if (value != null) {
                for (Style style : values()) {
                    if (style.name().equalsIgnoreCase(value)) {
                        return style;
                    }
                }
            }
            return DEFAULT;
        }
    }

    // The wire contract lives here and nowhere else: a backend rename costs one line.
    static final String KEY_PW_BUTTONS = "pw_buttons";
    static final String FIELD_ID = "id";
    static final String FIELD_LABEL = "label";
    static final String FIELD_ACTION = "action";
    static final String FIELD_URL = "url";
    static final String FIELD_STYLE = "style";

    static final int MAX_BUTTONS = 3;

    @NonNull private final String id;

    @NonNull private final String label;

    @NonNull private final ActionType action;

    @Nullable private final String url;

    @NonNull private final Style style;

    private ActionButton(
            @NonNull String id,
            @NonNull String label,
            @NonNull ActionType action,
            @Nullable String url,
            @NonNull Style style) {
        this.id = id;
        this.label = label;
        this.action = action;
        this.url = url;
        this.style = style;
    }

    /**
     * Parses one entry of the {@code pw_buttons} array.
     *
     * @param json one array element
     * @return the parsed button, or {@code null} when a mandatory field is missing or unusable
     */
    @Nullable static ActionButton parse(@NonNull JSONObject json) {
        String id = optNullableString(json, FIELD_ID);
        String label = optNullableString(json, FIELD_LABEL);
        if (TextUtils.isEmpty(id) || TextUtils.isEmpty(label)) {
            return null;
        }

        ActionType action = ActionType.parse(optNullableString(json, FIELD_ACTION));
        if (action == null) {
            return null;
        }

        String url = optNullableString(json, FIELD_URL);
        if ((action == ActionType.URL || action == ActionType.DEEPLINK) && TextUtils.isEmpty(url)) {
            return null;
        }

        Style style = Style.parse(optNullableString(json, FIELD_STYLE));
        return new ActionButton(id, label, action, TextUtils.isEmpty(url) ? null : url, style);
    }

    // Android org.json: optString(key, null) on an explicit JSON null returns the string "null".
    @Nullable private static String optNullableString(JSONObject json, String key) {
        return json.isNull(key) ? null : json.optString(key);
    }

    /**
     * @return button identifier, unique within one push; reported to the backend as {@code button_id}
     */
    @NonNull public String getId() {
        return id;
    }

    /**
     * @return button caption, already localized by the backend
     */
    @NonNull public String getLabel() {
        return label;
    }

    /**
     * @return what tapping this button does
     */
    @NonNull public ActionType getAction() {
        return action;
    }

    /**
     * The link the SDK opens for a {@link ActionType#URL} or {@link ActionType#DEEPLINK} button,
     * which is the only case where it is guaranteed to be present. Other action types ignore it,
     * but a {@code url} sent alongside them is kept as the server wrote it rather than dropped.
     *
     * @return the button's link, or {@code null} when the payload carried none
     */
    @Nullable public String getUrl() {
        return url;
    }

    /**
     * @return the requested visual style; Android does not render it
     */
    @NonNull public Style getStyle() {
        return style;
    }
}
