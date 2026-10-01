package com.example

import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import hev.socks5.TProxy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress

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

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Resolve host IP to prevent DNS resolution loop inside the tunnel
                val resolvedIp = try {
                    InetAddress.getByName(profile.host).hostAddress ?: profile.host
                } catch (_: Exception) {
                    profile.host
                }

                // 2. Setup VPN Interface with Google & Cloudflare DNS
                val builder = Builder()
                    .setSession("ProxyShortcut")
                    .setMtu(1500)
                    .addAddress("10.0.0.2", 24)
                    .addRoute("0.0.0.0", 0)
                    .addDnsServer("8.8.8.8")
                    .addDnsServer("1.1.1.1")

                // 3. Selective Routing / Split Tunneling Rule
                val useAll = profile.routeAllApps || profile.selectedApps.isEmpty()
                if (!useAll) {
                    for (pkg in profile.selectedApps) {
                        try {
                            builder.addAllowedApplication(pkg)
                        } catch (_: PackageManager.NameNotFoundException) {
                        } catch (_: Exception) {}
                    }
                    try {
                        builder.addAllowedApplication(packageName)
                    } catch (_: Exception) {}
                }

                vpnInterface = builder.establish()
                val fd = vpnInterface?.fd ?: return@launch

                // 4. Generate tun2socks config
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
                      address: '$resolvedIp'
                      udp: 'tcp'
                      $authUser
                      $authPass
                """.trimIndent()

                val configFile = File(cacheDir, "socks5.conf")
                FileOutputStream(configFile).use { it.write(configContent.toByteArray()) }

                // 5. Start native tun2socks engine
                TProxy.Start(configFile.absolutePath, fd)

                isConnected = true
                startTimeMs = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
                stopTunnel()
            }
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
