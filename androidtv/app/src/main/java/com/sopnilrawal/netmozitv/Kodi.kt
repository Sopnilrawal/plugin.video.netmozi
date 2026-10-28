package com.sopnilrawal.netmozitv

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Opens a page of the NetMozi add-on in Kodi: starts Kodi, then navigates it there through
 * Kodi's JSON-RPC (needs "Allow remote control via HTTP" turned on in Kodi).
 */
object Kodi {
    private const val PACKAGE = "org.xbmc.kodi"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    class Settings(val port: String, val user: String, val pass: String)

    fun settings(ctx: Context): Settings {
        val p = ctx.getSharedPreferences("kodi", Context.MODE_PRIVATE)
        return Settings(p.getString("port", "8080")!!, p.getString("user", "kodi")!!, p.getString("pass", "")!!)
    }

    fun saveSettings(ctx: Context, s: Settings) {
        ctx.getSharedPreferences("kodi", Context.MODE_PRIVATE).edit()
            .putString("port", s.port.ifBlank { "8080" })
            .putString("user", s.user)
            .putString("pass", s.pass)
            .apply()
    }

    fun open(ctx: Context, pluginPath: String) {
        val app = ctx.applicationContext
        val launch = app.packageManager.getLeanbackLaunchIntentForPackage(PACKAGE)
            ?: app.packageManager.getLaunchIntentForPackage(PACKAGE)
        if (launch == null) {
            Toast.makeText(app, R.string.kodi_missing, Toast.LENGTH_LONG).show()
            return
        }
        app.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        scope.launch {
            // Kodi may still be starting up: retry for about 30 seconds.
            repeat(30) {
                if (activateWindow(settings(app), pluginPath)) return@launch
                delay(1000)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(app, R.string.kodi_rpc_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun activateWindow(s: Settings, path: String): Boolean = try {
        val body = JSONObject()
            .put("jsonrpc", "2.0")
            .put("id", 1)
            .put("method", "GUI.ActivateWindow")
            .put("params", JSONObject().put("window", "videos").put("parameters", JSONArray().put(path).put("return")))
        val conn = URL("http://127.0.0.1:${s.port}/jsonrpc").openConnection() as HttpURLConnection
        conn.connectTimeout = 1500
        conn.readTimeout = 5000
        conn.requestMethod = "POST"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        if (s.user.isNotEmpty() || s.pass.isNotEmpty()) {
            val auth = Base64.encodeToString("${s.user}:${s.pass}".toByteArray(), Base64.NO_WRAP)
            conn.setRequestProperty("Authorization", "Basic $auth")
        }
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        val ok = conn.responseCode == 200 && conn.inputStream.bufferedReader().readText().contains("\"result\"")
        conn.disconnect()
        ok
    } catch (e: Exception) {
        false
    }
}
