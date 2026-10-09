package com.pushwoosh.featureflags;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Immutable snapshot of one feature flag as evaluated by Pushwoosh for this device.
 * <p>
 * Property getters return the default when the property is absent or its JSON type differs
 * from the getter: a string is never converted to a number and vice versa.
 */
public final class FeatureFlag {
    private final String key;
    private final boolean enabled;
    private final String ruleId;
    private final JSONObject properties;

    FeatureFlag(@NonNull String key, boolean enabled, @NonNull String ruleId, @NonNull JSONObject properties) {
        this.key = key;
        this.enabled = enabled;
        this.ruleId = ruleId;
        this.properties = JsonValues.copy(properties);
    }

    /**
     * Returns the flag key as configured in Pushwoosh.
     */
    @NonNull public String getKey() {
        return key;
    }

    /**
     * Returns whether the flag is on for this device and user, as evaluated by Pushwoosh.
     * <p>
     * {@code false} also when the flag is killed; {@link #getRuleId()} tells which rule produced the value.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Identifies the rule that produced {@link #isEnabled()} and the property overrides.
     *
     * @return id of the matched rule, {@code "default"} when no rule matched, {@code ""} when the flag is killed
     */
    @NonNull public String getRuleId() {
        return ruleId;
    }

    /**
     * Reads a string property.
     *
     * @param property the property name
     * @param defaultValue the value to return when the property is absent or is not a string
     * @return the property value, or {@code defaultValue}
     */
    @Nullable public String getString(@NonNull String property, @Nullable String defaultValue) {
        Object value = properties.opt(property);
        return value instanceof String ? (String) value : defaultValue;
    }

    /**
     * Reads a numeric property; integers and decimals are both returned as {@code double}.
     *
     * @param property the property name
     * @param defaultValue the value to return when the property is absent or is not a number
     * @return the property value, or {@code defaultValue}
     */
    public double getNumber(@NonNull String property, double defaultValue) {
        Object value = properties.opt(property);
        return value instanceof Number ? ((Number) value).doubleValue() : defaultValue;
    }

    /**
     * Reads a boolean property.
     *
     * @param property the property name
     * @param defaultValue the value to return when the property is absent or is not a boolean
     * @return the property value, or {@code defaultValue}
     */
    public boolean getBool(@NonNull String property, boolean defaultValue) {
        Object value = properties.opt(property);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    /**
     * Reads an object property.
     *
     * @param property the property name
     * @param defaultValue the value to return when the property is absent or is not an object
     * @return a copy of the object property; changing it does not change the flag
     */
    @Nullable public JSONObject getJsonObject(@NonNull String property, @Nullable JSONObject defaultValue) {
        Object value = properties.opt(property);
        return value instanceof JSONObject ? JsonValues.copy((JSONObject) value) : defaultValue;
    }

    /**
     * Reads an array property.
     *
     * @param property the property name
     * @param defaultValue the value to return when the property is absent or is not an array
     * @return a copy of the array property; changing it does not change the flag
     */
    @Nullable public JSONArray getJsonArray(@NonNull String property, @Nullable JSONArray defaultValue) {
        Object value = properties.opt(property);
        return value instanceof JSONArray ? JsonValues.copy((JSONArray) value) : defaultValue;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FeatureFlag)) {
            return false;
        }
        FeatureFlag that = (FeatureFlag) other;
        return enabled == that.enabled
                && key.equals(that.key)
                && ruleId.equals(that.ruleId)
                && JsonValues.equal(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, enabled, ruleId);
    }

    @NonNull @Override
    public String toString() {
        return "FeatureFlag{key=" + key + ", enabled=" + enabled + ", ruleId=" + ruleId + "}";
    }
}
