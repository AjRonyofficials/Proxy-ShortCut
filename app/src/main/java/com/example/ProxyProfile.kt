package com.example

import java.io.Serializable

data class ProxyProfile(
    var id: String = System.currentTimeMillis().toString(),
    var profileName: String = "Proxy Server",
    var host: String = "",
    var port: Int = 1080,
    var username: String = "",
    var password: String = "",
    var proxyType: String = "SOCKS5",
    var routeAllApps: Boolean = true,
    var selectedApps: HashSet<String> = HashSet()
) : Serializable {

    companion object {
        fun parseRawString(rawInput: String): ProxyProfile {
            val profile = ProxyProfile()
            var cleanText = rawInput.trim()

            if (cleanText.isEmpty()) return profile

            // Check if protocol specified (e.g. socks5:// or http://)
            if (cleanText.contains("://")) {
                val parts = cleanText.split("://")
                profile.proxyType = if (parts[0].contains("http", ignoreCase = true)) "HTTP" else "SOCKS5"
                cleanText = parts[1]
            }

            // Check if user:pass@host:port format
            if (cleanText.contains("@")) {
                val authAndHost = cleanText.split("@")
                val auth = authAndHost[0].split(":")
                if (auth.isNotEmpty()) profile.username = auth[0].trim()
                if (auth.size > 1) profile.password = auth[1].trim()

                val hostParts = authAndHost[1].split(":")
                profile.host = hostParts[0].trim()
                if (hostParts.size > 1) {
                    profile.port = hostParts[1].filter { it.isDigit() }.toIntOrNull() ?: 1080
                }
            } else {
                // host:port:user:pass or host:port format
                val parts = cleanText.split(":")
                if (parts.isNotEmpty()) profile.host = parts[0].trim()
                if (parts.size >= 2) profile.port = parts[1].trim().filter { it.isDigit() }.toIntOrNull() ?: 1080
                if (parts.size >= 3) profile.username = parts[2].trim()
                if (parts.size >= 4) profile.password = parts[3].trim()
            }

            profile.profileName = if (profile.host.isNotEmpty()) "Proxy (${profile.host})" else "Proxy Server"
            return profile
        }

        fun parseFromRaw(rawInput: String): ProxyProfile = parseRawString(rawInput)
    }
}
