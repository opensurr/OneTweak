package com.oneuitweak.app

object OneUiCheck {

    data class Info(val supported: Boolean, val label: String)

    fun check(): Info {
        val v = prop("ro.build.version.oneui").toIntOrNull()
            ?: return Info(false, "Not a Samsung One UI device")

        return if (v in 80000..99999) {
            Info(true, "One UI ${v / 10000}")
        } else {
            Info(false, "One UI ${v / 10000} (unsupported)")
        }
    }

    private fun prop(key: String): String = try {
        Runtime.getRuntime().exec(arrayOf("getprop", key))
            .inputStream.bufferedReader().use { it.readText().trim() }
    } catch (_: Exception) {
        ""
    }
}
