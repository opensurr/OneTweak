package com.oneuitweak.app

import java.util.Locale

data class Current(
    val anim: Float = 1f,
    val font: Float = 1f,
    val autoRotate: Boolean = false,
    val timeoutSec: Int = 60
)

object Tweaks {

    private val packageRegex = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")

    private fun f(v: Float): String = String.format(Locale.US, "%.2f", v)
    private fun bit(on: Boolean): Int = if (on) 1 else 0

    fun setAnimationScale(scale: Float) = AdbBridge.exec(
        "settings put global window_animation_scale ${f(scale)}",
        "settings put global transition_animation_scale ${f(scale)}",
        "settings put global animator_duration_scale ${f(scale)}"
    )

    fun setFontScale(scale: Float) = AdbBridge.exec("settings put system font_scale ${f(scale)}")

    fun setDensity(dpi: Int?): AdbBridge.Result =
        if (dpi == null || dpi !in 120..800) {
            AdbBridge.Result(false, "Enter a DPI between 120 and 800")
        } else {
            AdbBridge.exec("wm density $dpi")
        }

    fun resetDensity() = AdbBridge.exec("wm density reset")

    fun setNightMode(mode: String): AdbBridge.Result =
        if (mode !in setOf("yes", "no", "auto")) {
            AdbBridge.Result(false, "Invalid mode")
        } else {
            AdbBridge.exec("cmd uimode night $mode")
        }

    fun setAutoRotate(on: Boolean) = AdbBridge.exec("settings put system accelerometer_rotation ${bit(on)}")

    fun setScreenTimeout(seconds: Int) = AdbBridge.exec("settings put system screen_off_timeout ${seconds * 1000}")

    fun setBrightness(level: Int) = AdbBridge.exec(
        "settings put system screen_brightness_mode 0",
        "settings put system screen_brightness ${level.coerceIn(0, 255)}"
    )

    fun setShowTouches(on: Boolean) = AdbBridge.exec("settings put system show_touches ${bit(on)}")

    fun setPointerLocation(on: Boolean) = AdbBridge.exec("settings put system pointer_location ${bit(on)}")

    fun setStayAwake(on: Boolean) =
        AdbBridge.exec("settings put global stay_on_while_plugged_in ${if (on) 3 else 0}")

    fun setBluetooth(on: Boolean) = AdbBridge.exec("svc bluetooth ${if (on) "enable" else "disable"}")

    fun setMobileData(on: Boolean) = AdbBridge.exec("svc data ${if (on) "enable" else "disable"}")

    fun setAppEnabled(pkg: String, enabled: Boolean): AdbBridge.Result {
        if (!packageRegex.matches(pkg)) return AdbBridge.Result(false, "Invalid package name")
        val cmd = if (enabled) "pm enable --user 0 $pkg" else "pm disable-user --user 0 $pkg"
        return AdbBridge.exec(cmd)
    }

    fun readCurrent(): Current = Current(
        anim = AdbBridge.query("global", "window_animation_scale")?.toFloatOrNull() ?: 1f,
        font = AdbBridge.query("system", "font_scale")?.toFloatOrNull() ?: 1f,
        autoRotate = AdbBridge.query("system", "accelerometer_rotation") == "1",
        timeoutSec = ((AdbBridge.query("system", "screen_off_timeout")?.toLongOrNull() ?: 60000L) / 1000).toInt()
    )

    fun applyProfile(p: Profile): AdbBridge.Result {
        val results = listOf(
            setAnimationScale(p.anim),
            setFontScale(p.font),
            setNightMode(p.nightMode),
            setAutoRotate(p.autoRotate),
            setScreenTimeout(p.timeoutSec)
        )
        return results.firstOrNull { !it.ok } ?: AdbBridge.Result(true, "Profile ${p.name} applied")
    }

    fun resetAll(): AdbBridge.Result {
        val results = listOf(
            setAnimationScale(1f),
            setFontScale(1f),
            resetDensity(),
            setNightMode("auto"),
            setAutoRotate(false),
            setScreenTimeout(60),
            setShowTouches(false),
            setPointerLocation(false),
            setStayAwake(false)
        )
        return results.firstOrNull { !it.ok } ?: AdbBridge.Result(true, "Reset done")
    }
}
