package com.souljoy.soulmasti

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.souljoy.soulmasti.ui.SoulplayApp
import com.souljoy.soulmasti.ui.theme.SoulplayTheme

class MainActivity : ComponentActivity() {

    private val permissionRequestId = 22
    private lateinit var appUpdateManager: AppUpdateManager
    private val updateFlowLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                Toast.makeText(
                    this,
                    "Update canceled. You can update later from Play Store.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showCompleteUpdateDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        super.onCreate(savedInstanceState)
        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(installStateListener)
        // Disable Android system back from this Activity so it does not return to an old stack.
        onBackPressedDispatcher.addCallback(this) {
            // Intentionally no-op — back button does nothing.
        }
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            SoulplayTheme {
                SoulplayApp(
                    hasVoicePermission = { checkVoicePermissions() },
                    requestVoicePermission = { requestVoicePermissions() }
                )
            }
        }
        checkForFlexibleUpdate()
    }

    override fun onResume() {
        super.onResume()
        checkForFlexibleUpdate()
    }

    override fun onDestroy() {
        appUpdateManager.unregisterListener(installStateListener)
        super.onDestroy()
    }

    private fun checkVoicePermissions(): Boolean =
        requiredVoicePermissions().all { perm ->
            ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestVoicePermissions() {
        ActivityCompat.requestPermissions(
            this,
            requiredVoicePermissions(),
            permissionRequestId
        )
    }

    private fun requiredVoicePermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != permissionRequestId) return
        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            Toast.makeText(this, "Permissions granted — tap Join again", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Microphone permission is required for voice", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkForFlexibleUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    showCompleteUpdateDialog()
                    return@addOnSuccessListener
                }
                val shouldShowUpdatePrompt =
                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                if (!shouldShowUpdatePrompt) return@addOnSuccessListener
                val options = AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                runCatching {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        updateFlowLauncher,
                        options
                    )
                }
            }
    }

    private fun showCompleteUpdateDialog() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this)
            .setTitle("Update ready")
            .setMessage("A new version has been downloaded. Restart app to finish update.")
            .setCancelable(false)
            .setPositiveButton("Restart now") { _, _ ->
                appUpdateManager.completeUpdate()
            }
            .setNegativeButton("Later", null)
            .show()
    }
}
