package com.oneuitweak.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private val connected = mutableStateOf(false)
    private val oneUi = mutableStateOf(OneUiCheck.Info(false, "Checking..."))
    private val current = mutableStateOf(Current())
    private val profiles = mutableStateOf<List<Profile>>(emptyList())
    private val busy = mutableStateOf(false)
    private val message = mutableStateOf("Welcome to OneUI Tweak. Finish setup to unlock tweaks.")
    private val notifGranted = mutableStateOf(false)
    private val a11yEnabled = mutableStateOf(false)
    private val model = mutableStateOf(Build.MODEL)

    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted.value = granted
        message.value = if (granted) "Notifications allowed" else "Notifications blocked"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        profiles.value = ProfileStore.load(this)
        model.value = getSharedPreferences("device", MODE_PRIVATE).getString("model", null) ?: Build.MODEL

        lifecycleScope.launch(Dispatchers.IO) {
            val info = OneUiCheck.check()
            withContext(Dispatchers.Main) { oneUi.value = info }
        }

        setContent {
            OneUiTweakTheme {
                AppRoot(
                    state = UiState(
                        connected = connected.value,
                        oneUi = oneUi.value,
                        current = current.value,
                        profiles = profiles.value,
                        busy = busy.value,
                        message = message.value,
                        notifGranted = notifGranted.value,
                        a11yEnabled = a11yEnabled.value,
                        model = model.value
                    ),
                    actions = Actions(
                        tweak = this::runTweak,
                        requestNotifications = this::requestNotifications,
                        openAccessibility = this::openAccessibility,
                        pairAndConnect = this::pairAndConnect,
                        disconnect = this::disconnect,
                        refresh = this::refresh,
                        setLauncher = { variant ->
                            setLauncherIcon(this@MainActivity, variant)
                            message.value = "Icon changed. The launcher may take a moment."
                        },
                        saveProfile = this::saveProfile,
                        deleteProfile = this::deleteProfile,
                        setModel = this::setModel
                    )
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        connected.value = AdbBridge.isConnected
        notifGranted.value = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        a11yEnabled.value = isAccessibilityEnabled()
        if (AdbBridge.isConnected) {
            lifecycleScope.launch(Dispatchers.IO) {
                val c = Tweaks.readCurrent()
                withContext(Dispatchers.Main) { current.value = c }
            }
        }
    }

    private fun runTweak(label: String, block: () -> AdbBridge.Result) {
        if (busy.value) return
        busy.value = true
        lifecycleScope.launch {
            try {
                val r = withContext(Dispatchers.IO) { block() }
                message.value = if (r.ok) "$label: done" else "$label failed: ${r.output.ifBlank { "unknown error" }}"
            } finally {
                busy.value = false
                refresh()
            }
        }
    }

    private fun pairAndConnect(pairPort: Int, code: String, connectPort: Int) {
        if (busy.value) return
        busy.value = true
        message.value = "Pairing..."
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val paired = AdbBridge.pair(this@MainActivity, pairPort, code)
                    if (!paired.ok) paired else AdbBridge.connect(this@MainActivity, connectPort)
                }
                message.value = if (result.ok) "Connected to ADB. Tweaks are ready." else result.output
                if (result.ok) notifyConnected()
            } finally {
                busy.value = false
                refresh()
            }
        }
    }

    private fun disconnect() {
        AdbBridge.disconnect()
        message.value = "Disconnected from ADB"
        refresh()
    }

    private fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            notifGranted.value = true
        }
    }

    private fun openAccessibility() {
        message.value = "Find OneUI Tweak in the list and turn it on."
        runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains("$packageName/")
    }

    @SuppressLint("MissingPermission")
    private fun notifyConnected() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return

        val channelId = "adb_status"
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(channelId, "ADB status", NotificationManager.IMPORTANCE_LOW))

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("OneUI Tweak connected")
            .setContentText("ADB is ready. You can change tweaks now.")
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(1, notification)
    }

    private fun saveProfile(p: Profile) {
        val updated = profiles.value.filterNot { it.name == p.name } + p
        profiles.value = updated
        ProfileStore.save(this, updated)
        message.value = "Saved profile ${p.name}"
    }

    private fun deleteProfile(name: String) {
        val updated = profiles.value.filterNot { it.name == name }
        profiles.value = updated
        ProfileStore.save(this, updated)
        message.value = "Deleted profile $name"
    }

    private fun setModel(m: String) {
        model.value = m
        getSharedPreferences("device", MODE_PRIVATE).edit().putString("model", m).apply()
        message.value = "Device set to $m"
    }
}

/** Switches only this app's own launcher icon. Needs no ADB. */
fun setLauncherIcon(context: Context, variant: String) {
    val pm = context.packageManager
    listOf("LauncherDefault", "LauncherDark").forEach { name ->
        pm.setComponentEnabledSetting(
            ComponentName(context.packageName, "${context.packageName}.$name"),
            if (name == variant) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
