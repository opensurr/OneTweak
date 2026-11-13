package com.oneuitweak.app

import android.content.Context
import android.os.Build
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import io.github.muntashirakon.adb.AdbStream
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.X509Certificate
import java.util.Date
import java.util.Random
import javax.security.auth.x500.X500Principal

object AdbBridge {

    data class Result(val ok: Boolean, val output: String)

    private const val HOST = "127.0.0.1"
    private var manager: AdbManager? = null

    val isConnected: Boolean
        get() = manager?.isConnected == true

    private class AdbManager(context: Context) : AbsAdbConnectionManager() {

        private val privateKey: PrivateKey
        private val certificate: Certificate

        init {
            setApi(Build.VERSION.SDK_INT)
            val generator = KeyPairGenerator.getInstance("RSA")
            generator.initialize(2048, SecureRandom.getInstance("SHA1PRNG"))
            val pair = generator.generateKeyPair()
            privateKey = pair.private
            certificate = generateSelfSignedCert(pair.private, pair.public)
        }

        override fun getPrivateKey(): PrivateKey = privateKey
        override fun getCertificate(): Certificate = certificate
        override fun getDeviceName(): String = "OneUITweak"

        private fun generateSelfSignedCert(
            privateKey: PrivateKey,
            publicKey: java.security.PublicKey
        ): X509Certificate {
            // LibADB requires a real X.509 cert for TLS pairing.
            // For a minimal working build, this throws at runtime.
            // For production, add `sun-security-android` and follow the LibADB README.
            throw UnsupportedOperationException(
                "Certificate generation not implemented. See LibADB README."
            )
        }
    }

    fun pair(context: Context, port: Int, code: String): Result = try {
        val mgr = getOrCreateManager(context)
        mgr.pair(HOST, port, code)
        Result(true, "Paired")
    } catch (t: Throwable) {
        Result(false, t.message ?: "Pairing failed")
    }

    fun connect(context: Context, port: Int): Result = try {
        val mgr = getOrCreateManager(context)
        mgr.connect(HOST, port)
        Result(true, "Connected")
    } catch (t: Throwable) {
        Result(false, t.message ?: "Connection failed")
    }

    fun disconnect() {
        runCatching { manager?.close() }
        manager = null
    }

    fun exec(vararg commands: String): Result {
        val mgr = manager ?: return Result(false, "Not connected to ADB")
        return try {
            val stream: AdbStream = mgr.openStream("shell:" + commands.joinToString(" && "))
            val out = stream.openInputStream().bufferedReader().use { it.readText() }.trim()
            val failed = out.startsWith("Error", ignoreCase = true) ||
                out.contains("Permission denied")
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

    private fun getOrCreateManager(context: Context): AdbManager {
        if (manager == null) manager = AdbManager(context.applicationContext)
        return manager!!
    }
}
