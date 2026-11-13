package com.oneuitweak.app

import android.content.Context
import io.github.muntashirakon.adb.AdbConnection
import io.github.muntashirakon.adb.AdbKeyPair
import io.github.muntashirakon.adb.AdbPairingClient
import java.io.File
import java.net.Socket

/**
 * Native ADB client. Pairs with Android Wireless debugging and runs shell commands.
 * Built on libadb-android. All library calls live in this file, so API fixes stay here.
 */
object AdbBridge {

    data class Result(val ok: Boolean, val output: String)

    private const val HOST = "127.0.0.1"

    @Volatile
    private var connection: AdbConnection? = null

    val isConnected: Boolean
        get() = connection != null

    private fun keys(context: Context): AdbKeyPair {
        val priv = File(context.filesDir, "adb_key")
        val pub = File(context.filesDir, "adb_key.pub")
        return if (priv.exists() && pub.exists()) {
            AdbKeyPair.read(priv, pub)
        } else {
            AdbKeyPair.generate(priv, pub)
        }
    }

    /** Step 1: pair using the port and 6-digit code from "Pair device with pairing code". */
    fun pair(context: Context, port: Int, code: String): Result = try {
        val client = AdbPairingClient(HOST, port, code, keys(context))
        try {
            if (client.start()) Result(true, "Paired") else Result(false, "Pairing failed. Check the port and code.")
        } finally {
            client.close()
        }
    } catch (t: Throwable) {
        Result(false, t.message ?: "Pairing failed")
    }

    /** Step 2: connect using the port shown on the main Wireless debugging screen. */
    fun connect(context: Context, port: Int): Result = try {
        val conn = AdbConnection.create(Socket(HOST, port), keys(context))
        if (conn.connect()) {
            connection = conn
            Result(true, "Connected")
        } else {
            Result(false, "Connection refused. Check the connect port.")
        }
    } catch (t: Throwable) {
        Result(false, t.message ?: "Connection failed")
    }

    fun disconnect() {
        runCatching { connection?.close() }
        connection = null
    }

    fun exec(vararg commands: String): Result {
        val conn = connection ?: return Result(false, "Not connected to ADB")
        return try {
            val stream = conn.open("shell:" + commands.joinToString(" && "))
            val out = stream.openInputStream().bufferedReader().use { it.readText() }.trim()
            val failed = out.startsWith("Error", ignoreCase = true) ||
                out.contains("Permission denied") ||
                out.contains("Exception")
            Result(!failed, out)
        } catch (t: Throwable) {
            disconnect()
            Result(false, "ADB connection lost: ${t.message ?: "unknown"}")
        }
    }

    fun query(namespace: String, key: String): String? {
        val r = exec("settings get $namespace $key")
        return if (r.ok && r.output.isNotBlank() && r.output != "null") r.output else null
    }
}
