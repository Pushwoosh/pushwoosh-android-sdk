package com.pushwoosh.calls

/**
 * Callback interface for receiving VoIP call permission request results.
 *
 * This callback is invoked on the main thread after the user responds to the
 * READ_PHONE_NUMBERS permission request dialog initiated by
 * [PushwooshCallSettings.requestCallPermissions].
 *
 * **Usage Pattern:**
 * ```kotlin
 * class MainActivity : AppCompatActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *
 *         // Request permissions with callback
 *         PushwooshCallSettings.requestCallPermissions(object : CallPermissionsCallback {
 *             override fun onPermissionResult(
 *                 granted: Boolean,
 *                 grantedPermissions: List<String>,
 *                 deniedPermissions: List<String>
 *             ) {
 *                 if (granted) {
 *                     // All permissions granted
 *                     Log.d("App", "VoIP enabled: $grantedPermissions")
 *                     initializeVoIPFeatures()
 *                 } else {
 *                     // Some permissions denied
 *                     Log.w("App", "Denied: $deniedPermissions")
 *                     showPermissionRationale()
 *                 }
 *             }
 *         })
 *     }
 * }
 * ```
 *
 * **Lambda Syntax (Kotlin):**
 * ```kotlin
 * PushwooshCallSettings.requestCallPermissions { granted, grantedPerms, deniedPerms ->
 *     when {
 *         granted -> enableVoIPCalls()
 *         deniedPerms.isNotEmpty() -> showPermissionDenied()
 *     }
 * }
 * ```
 *
 * **Java Usage:**
 * ```java
 * PushwooshCallSettings.requestCallPermissions(new CallPermissionsCallback() {
 *     @Override
 *     public void onPermissionResult(
 *         boolean granted,
 *         List<String> grantedPermissions,
 *         List<String> deniedPermissions
 *     ) {
 *         if (granted) {
 *             Log.d("App", "VoIP ready");
 *             enableVoIPFeatures();
 *         } else {
 *             Log.w("App", "Permission denied: " + deniedPermissions);
 *         }
 *     }
 * });
 * ```
 *
 * @see PushwooshCallSettings.requestCallPermissions
 */
interface CallPermissionsCallback {
    /**
     * Called when the permission request is completed.
     *
     * This method is invoked on the main thread immediately after the user
     * responds to the system permission dialog. It provides detailed information
     * about which permissions were granted and which were denied.
     *
     * **Example:**
     * ```kotlin
     * override fun onPermissionResult(
     *     granted: Boolean,
     *     grantedPermissions: List<String>,
     *     deniedPermissions: List<String>
     * ) {
     *     if (granted) {
     *         // All permissions granted - VoIP ready
     *         Toast.makeText(this, "VoIP calls enabled", Toast.LENGTH_SHORT).show()
     *         showVoIPCallButton()
     *     } else {
     *         // Some permissions denied
     *         val deniedList = deniedPermissions.joinToString(", ")
     *         Log.w("App", "Denied permissions: $deniedList")
     *
     *         // Show explanation or redirect to settings
     *         AlertDialog.Builder(this)
     *             .setMessage("Phone permission is required for VoIP calls")
     *             .setPositiveButton("Settings") { _, _ -> openSettings() }
     *             .show()
     *     }
     * }
     * ```
     *
     * @param granted `true` if all requested permissions were granted by the user,
     *                `false` if any permission was denied.
     * @param grantedPermissions list of permission strings that were granted.
     *                          Typically contains "android.permission.READ_PHONE_NUMBERS"
     *                          when VoIP permissions are granted.
     * @param deniedPermissions list of permission strings that were denied by the user.
     *                         Empty list if all permissions were granted.
     */
    fun onPermissionResult(
        granted: Boolean,
        grantedPermissions: List<String>,
        deniedPermissions: List<String>
    )
}
