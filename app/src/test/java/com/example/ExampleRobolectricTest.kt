package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Proxy Shortcut", appName)
    }

    @Test
    fun `test parse raw proxy formatted string`() {
        val raw = "192.168.1.50:1080:myuser:mypass"
        val profile = ProxyProfile.parseFromRaw(raw)
        assertEquals("192.168.1.50", profile.host)
        assertEquals(1080, profile.port)
        assertEquals("myuser", profile.username)
        assertEquals("mypass", profile.password)
    }

    @Test
    fun `test parse socks5 url format`() {
        val raw = "socks5://admin:secret123@proxy.example.com:9050"
        val profile = ProxyProfile.parseFromRaw(raw)
        assertEquals("proxy.example.com", profile.host)
        assertEquals(9050, profile.port)
        assertEquals("admin", profile.username)
        assertEquals("secret123", profile.password)
        assertEquals("SOCKS5", profile.proxyType)
    }

    @Test
    fun `test profile storage save and retrieve`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val profile = ProxyProfile(
            id = "test-prof-1",
            host = "10.0.0.1",
            port = 8888,
            username = "user",
            password = "pwd"
        )
        ProfileStorage.saveProfiles(context, listOf(profile))
        ProfileStorage.setActiveProfileId(context, "test-prof-1")

        val retrieved = ProfileStorage.getActiveProfile(context)
        assertNotNull(retrieved)
        assertEquals("10.0.0.1", retrieved?.host)
        assertEquals(8888, retrieved?.port)
        assertTrue(retrieved?.routeAllApps == true)
    }
}
