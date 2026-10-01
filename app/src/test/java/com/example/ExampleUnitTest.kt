package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testParseRawProxyFormattedString() {
        val raw = "192.168.1.50:1080:myuser:mypass"
        val profile = ProxyProfile.parseFromRaw(raw)
        assertEquals("192.168.1.50", profile.host)
        assertEquals(1080, profile.port)
        assertEquals("myuser", profile.username)
        assertEquals("mypass", profile.password)
    }

    @Test
    fun testParseSocks5UrlFormat() {
        val raw = "socks5://admin:secret123@proxy.example.com:9050"
        val profile = ProxyProfile.parseFromRaw(raw)
        assertEquals("proxy.example.com", profile.host)
        assertEquals(9050, profile.port)
        assertEquals("admin", profile.username)
        assertEquals("secret123", profile.password)
        assertEquals("SOCKS5", profile.proxyType)
    }

    @Test
    fun testParseHttpUrlFormat() {
        val raw = "http://admin:secret123@proxy.example.com:8080"
        val profile = ProxyProfile.parseFromRaw(raw)
        assertEquals("proxy.example.com", profile.host)
        assertEquals(8080, profile.port)
        assertEquals("HTTP", profile.proxyType)
    }
}
