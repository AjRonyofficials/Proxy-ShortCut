package com.example

import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import hev.socks5.TProxy
import java.io.File
import java.io.FileOutputStream

class ProxyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    companion object {
        var isConnected = false
        var startTimeMs: Long = 0
        const val ACTION_START = "ACTION_START_PROXY"
        const val ACTION_STOP = "ACTION_STOP_PROXY"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRealTunnel()
            ACTION_STOP -> stopTunnel()
        }
        return START_NOT_STICKY
    }

    private fun startRealTunnel() {
        if (isConnected) return
        val profile = ProfileStorage.getActiveProfile(this) ?: return

        try {
            val builder = Builder()
                .setSession("ProxyShortcut")
                .setMtu(1500)
                .addAddress("10.0.0.2", 24)
                .addRoute("0.0.0.0", 0)
                .addDnsServer("8.8.8.8")
                .addDnsServer("1.1.1.1")

            if (!profile.routeAllApps && profile.selectedApps.isNotEmpty()) {
                for (pkg in profile.selectedApps) {
                    try {
                        builder.addAllowedApplication(pkg)
                    } catch (_: PackageManager.NameNotFoundException) {}
                }
            }

            vpnInterface = builder.establish()
            val fd = vpnInterface?.fd ?: return

            val authUser = if (profile.username.isNotEmpty()) "username: '${profile.username}'" else ""
            val authPass = if (profile.password.isNotEmpty()) "password: '${profile.password}'" else ""

            val configContent = """
                misc:
                  task-stack-size: 81920
                  log-level: warn
                tunnel:
                  name: tun0
                  mtu: 1500
                socks5:
                  port: ${profile.port}
                  address: '${profile.host}'
                  udp: 'tcp'
                  $authUser
                  $authPass
            """.trimIndent()

            val configFile = File(cacheDir, "socks5.conf")
            FileOutputStream(configFile).use { it.write(configContent.toByteArray()) }

            TProxy.Start(configFile.absolutePath, fd)

            isConnected = true
            startTimeMs = System.currentTimeMillis()
        } catch (e: Exception) {
            e.printStackTrace()
            stopTunnel()
        }
    }

    private fun stopTunnel() {
        try {
            TProxy.Stop()
            vpnInterface?.close()
            vpnInterface = null
        } catch (_: Exception) {}

        isConnected = false
        startTimeMs = 0
        stopSelf()
    }

    override fun onDestroy() {
        stopTunnel()
        super.onDestroy()
    }
}
