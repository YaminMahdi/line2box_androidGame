package com.diu.yk_games.line2box.util

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.diu.yk_games.line2box.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

/**
 * name : Yamin Mahdi
 * date : 31/08/24 9:50 AM
 * email: yamin_khan@aisa.com
 */

/**
 * Must create an instance of this class in the activity root as val.
 * @param activity: provide ComponentActivity instance.
 * @param onEnd: callback invokes when checking is done
 */
class InAppUpdate(private val activity: ComponentActivity, private val onEnd : ()-> Unit = {}) {
    companion object{
        private var appUpdateManager: AppUpdateManager? = null
        private var isImmediateUpdateStarted = false
    }
    private val localVersionCode = BuildConfig.VERSION_CODE
    private var playVersionCode = 0

    private val updateFlowResultLauncher =
        activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {result->
            isImmediateUpdateStarted = false
            if (result.resultCode == RESULT_OK) {
                // Handle successful app update
                onEnd.invoke()
            }
            if (result.resultCode == RESULT_CANCELED) {
                if(playVersionCode - localVersionCode > 2)
                    showImmediateUpdate()
                else
                    onEnd.invoke()
            }
        }

    /**
     * Checks that the update is available or not.
     * @param noUpdate: callback function that will be invoked when there is no update available.
     */
    fun checkForUpdate(noUpdate: () -> Unit = {}) {
        AppUpdateManagerFactory
            .create(activity)
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                    appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ||
                    appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED
                ) {
                    playVersionCode = appUpdateInfo.availableVersionCode()
                    if (playVersionCode > localVersionCode)
                        showImmediateUpdate()
                    Log.d("checkUpdate", "checkForUpdate: update available")
                } else {
                    Log.d("checkUpdate", "checkForUpdate: no update available")
                    noUpdate.invoke()
                }
            }
            .addOnFailureListener {
                Log.d("checkUpdate", "checkForUpdate: ${it.message}")
                noUpdate.invoke()
            }
    }

    /**
     * Show the update dialog to the user.
     * If the user cancels the update dialog then it will be shown again if `localVersionCode` is lower by 2 from `playVersionCode`.
     */
    private fun showImmediateUpdate() {
        if (isImmediateUpdateStarted) return
        isImmediateUpdateStarted = true
        appUpdateManager = AppUpdateManagerFactory.create(activity)
        appUpdateManager?.let { manager ->
            manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
                manager.startUpdateFlowForResult(
                    appUpdateInfo, updateFlowResultLauncher,
                    AppUpdateOptions
                        .newBuilder(AppUpdateType.IMMEDIATE)
                        .build()
                )
            }.addOnFailureListener {
                isImmediateUpdateStarted = false
                showPlayStorePage()
            }.addOnCanceledListener {
                isImmediateUpdateStarted = false
                if (playVersionCode - localVersionCode > 2)
                    showImmediateUpdate()
            }
        }

    }

    /**
     * Opens the app's Play Store page to prompt the user for an update.
     * If the Play Store app is not found, it falls back to opening the Play Store website in a browser.
     */
    private fun showPlayStorePage() {
        try {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setData("https://play.google.com/store/apps/details?id=${activity.applicationContext.packageName}".toUri())
                    .setPackage("com.android.vending")
            )
        } catch (_: ActivityNotFoundException) {
            activity.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setData("market://details?id=${activity.applicationContext.packageName}".toUri())
            )
        }
    }


    /**
     * Call this function in Activity's `onResume()` function.
     * This function handles the case when an immediate update was shown but the app was
     * closed before the update was completed. In this case, onResume should check if an update
     * is still pending and if so, resume the update.
     */
    fun onResume() {
        appUpdateManager
            ?: run { AppUpdateManagerFactory.create(activity).also { appUpdateManager = it } }
                .appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    // However, you should execute this check at all entry points into the app.
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        // If an in-app update is already running, resume the update.
                        showImmediateUpdate()
                    }
                }
    }
}

//implementation("com.google.android.play:app-update:2.1.0")
//implementation("com.google.android.play:app-update-ktx:2.1.0")