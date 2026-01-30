/*
 *
 * Copyright (c) 2026. Pushwoosh Inc. (http://www.pushwoosh.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * (i) the original and/or modified Software should be used exclusively to work with Pushwoosh services,
 *
 * (ii) the above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.pushwoosh.notification.handlers.message.system;

import android.os.Bundle;
import android.text.TextUtils;

import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushBundleDataProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Dispatcher for system push commands.
 *
 * Supports two formats:
 *
 * 1. Single command (legacy):
 * {"pw_system_push": 1, "pw_command": "setLogLevel", "value": "INFO"}
 *
 * 2. Multiple commands (new):
 * {"pw_system_push": 1, "pw_commands": [
 *     {"command": "set_base_url", "value": "https://..."},
 *     {"command": "setLogLevel", "value": "INFO"}
 * ]}
 */
class SystemCommandDispatcher implements MessageSystemHandler {

    private static final String TAG = "SystemCommandDispatcher";
    private static final String KEY_PW_COMMANDS = "pw_commands";

    private final Map<String, SystemCommandHandler> handlers = new HashMap<>();

    SystemCommandDispatcher() {
        // Register built-in handlers
        registerHandler(new LogLevelCommandHandler());
        registerHandler(new BaseUrlCommandHandler());
    }

    /**
     * Register a command handler.
     */
    void registerHandler(SystemCommandHandler handler) {
        if (handler != null && handler.getCommandName() != null) {
            handlers.put(handler.getCommandName(), handler);
            PWLog.debug(TAG, "Registered command handler: " + handler.getCommandName());
        }
    }

    @Override
    public boolean preHandleMessage(Bundle pushBundle) {
        if (!PushBundleDataProvider.isSystemPush(pushBundle)) {
            return false;
        }

        // Try new format first: pw_commands array
        String commandsJson = pushBundle.getString(KEY_PW_COMMANDS);
        if (!TextUtils.isEmpty(commandsJson)) {
            return processMultipleCommands(commandsJson);
        }

        // Fall back to legacy format: single pw_command
        String command = PushBundleDataProvider.getInternalCommand(pushBundle);
        if (!TextUtils.isEmpty(command)) {
            String value = PushBundleDataProvider.getValue(pushBundle);
            return processSingleCommand(command, value);
        }

        PWLog.warn(TAG, "System push received but no command found");
        return true; // Consume the system push even if no command
    }

    private boolean processMultipleCommands(String commandsJson) {
        try {
            JSONArray commands = new JSONArray(commandsJson);
            boolean anyHandled = false;

            for (int i = 0; i < commands.length(); i++) {
                JSONObject commandObj = commands.optJSONObject(i);
                if (commandObj == null) {
                    continue;
                }

                String command = commandObj.optString("command");
                String value = commandObj.optString("value", null);

                if (!TextUtils.isEmpty(command)) {
                    if (processSingleCommand(command, value)) {
                        anyHandled = true;
                    }
                }
            }

            return anyHandled || commands.length() > 0;
        } catch (JSONException e) {
            PWLog.error(TAG, "Failed to parse pw_commands: " + e.getMessage());
            return false;
        }
    }

    private boolean processSingleCommand(String command, String value) {
        SystemCommandHandler handler = handlers.get(command);

        if (handler == null) {
            PWLog.warn(TAG, "No handler registered for command: " + command);
            return false;
        }

        PWLog.debug(TAG, "Handling command: " + command);
        boolean handled = handler.handleCommand(command, value);

        if (handled) {
            PWLog.info(
                    TAG,
                    "Command handled successfully: " + command + ", value: " + (value != null ? value : "(no value)"));
        } else {
            PWLog.warn(TAG, "Command handler returned false: " + command);
        }

        return handled;
    }

    /**
     * Interface for command handlers.
     */
    interface SystemCommandHandler {
        /**
         * @return Command name that this handler responds to.
         */
        String getCommandName();

        /**
         * Handle the command.
         * @param command Command name
         * @param value Command value (may be null)
         * @return true if handled successfully
         */
        boolean handleCommand(String command, String value);
    }

    /**
     * Handler for setLogLevel command.
     */
    private static class LogLevelCommandHandler implements SystemCommandHandler {
        @Override
        public String getCommandName() {
            return "setLogLevel";
        }

        @Override
        public boolean handleCommand(String command, String value) {
            if (value != null && !value.isEmpty()) {
                com.pushwoosh.repository.RepositoryModule.getRegistrationPreferences()
                        .logLevel()
                        .set(value);
                PWLog.updateLogLevel(value);
                return true;
            }
            return false;
        }
    }

    /**
     * Handler for set_base_url command.
     */
    private static class BaseUrlCommandHandler implements SystemCommandHandler {
        private static final String HANDLER_TAG = "BaseUrlHandler";

        @Override
        public String getCommandName() {
            return "set_base_url";
        }

        @Override
        public boolean handleCommand(String command, String value) {
            if (value == null || value.isEmpty()) {
                PWLog.warn(HANDLER_TAG, "Empty URL value");
                return false;
            }

            if (!isValidUrl(value)) {
                PWLog.error(HANDLER_TAG, "Invalid URL: " + value);
                return false;
            }

            com.pushwoosh.internal.network.RequestManager requestManager =
                    com.pushwoosh.internal.network.NetworkModule.getRequestManager();
            if (requestManager == null) {
                PWLog.error(HANDLER_TAG, "RequestManager is not initialized");
                return false;
            }

            String normalizedUrl = value.endsWith("/") ? value : value + "/";
            requestManager.updateBaseUrl(normalizedUrl);
            PWLog.info(HANDLER_TAG, "Base URL updated to: " + normalizedUrl);
            return true;
        }

        private boolean isValidUrl(String url) {
            if (!url.startsWith("https://") && !url.startsWith("http://")) {
                return false;
            }
            try {
                new java.net.URL(url);
                return true;
            } catch (java.net.MalformedURLException e) {
                return false;
            }
        }
    }
}
