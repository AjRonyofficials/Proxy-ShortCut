package com.example

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var edtProfileName: EditText
    private lateinit var edtHost: EditText
    private lateinit var edtPort: EditText
    private lateinit var edtUsername: EditText
    private lateinit var edtPassword: EditText
    private lateinit var rgProtocol: RadioGroup
    private lateinit var rbSocks5: RadioButton
    private lateinit var rbHttp: RadioButton
    private lateinit var switchRouteAll: Switch

    private lateinit var btnClipboard: Button
    private lateinit var btnSelectApps: Button
    private lateinit var btnSave: Button
    private lateinit var btnStartFloating: Button

    private var currentProfile: ProxyProfile = ProxyProfile()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        loadExistingProfile()
        setupListeners()
    }

    private fun bindViews() {
        edtProfileName = findViewById(R.id.edtProfileName)
        edtHost = findViewById(R.id.edtHost)
        edtPort = findViewById(R.id.edtPort)
        edtUsername = findViewById(R.id.edtUsername)
        edtPassword = findViewById(R.id.edtPassword)
        rgProtocol = findViewById(R.id.rgProtocol)
        rbSocks5 = findViewById(R.id.rbSocks5)
        rbHttp = findViewById(R.id.rbHttp)
        switchRouteAll = findViewById(R.id.switchRouteAll)

        btnClipboard = findViewById(R.id.btnClipboard)
        btnSelectApps = findViewById(R.id.btnSelectApps)
        btnSave = findViewById(R.id.btnSave)
        btnStartFloating = findViewById(R.id.btnStartFloating)
    }

    private fun loadExistingProfile() {
        ProfileStorage.getActiveProfile(this)?.let {
            currentProfile = it
            edtProfileName.setText(it.profileName)
            edtHost.setText(it.host)
            edtPort.setText(it.port.toString())
            edtUsername.setText(it.username)
            edtPassword.setText(it.password)
            switchRouteAll.isChecked = it.routeAllApps

            if (it.proxyType.equals("HTTP", ignoreCase = true)) {
                rbHttp.isChecked = true
            } else {
                rbSocks5.isChecked = true
            }
        }
    }

    private fun setupListeners() {
        // ⚡ 1-Click Smart Auto Paste
        btnClipboard.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val raw = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
            if (raw.isNotEmpty()) {
                val parsed = ProxyProfile.parseRawString(raw)
                currentProfile.host = parsed.host
                currentProfile.port = parsed.port
                currentProfile.username = parsed.username
                currentProfile.password = parsed.password
                currentProfile.proxyType = parsed.proxyType

                edtProfileName.setText(parsed.profileName)
                edtHost.setText(parsed.host)
                edtPort.setText(parsed.port.toString())
                edtUsername.setText(parsed.username)
                edtPassword.setText(parsed.password)

                if (parsed.proxyType.equals("HTTP", ignoreCase = true)) {
                    rbHttp.isChecked = true
                } else {
                    rbSocks5.isChecked = true
                }

                Toast.makeText(this, "⚡ Auto-Parsed: ${parsed.host}:${parsed.port}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Clipboard is empty!", Toast.LENGTH_SHORT).show()
            }
        }

        btnSelectApps.setOnClickListener {
            syncFormToProfile()
            ProfileStorage.saveProfiles(this, listOf(currentProfile))
            ProfileStorage.setActiveProfileId(this, currentProfile.id)
            startActivity(Intent(this, AppSelectActivity::class.java))
        }

        btnSave.setOnClickListener {
            if (validateAndSave()) {
                Toast.makeText(this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show()
            }
        }

        btnStartFloating.setOnClickListener {
            if (validateAndSave()) {
                checkPermissionsAndLaunchOverlay()
            }
        }
    }

    private fun syncFormToProfile() {
        val name = edtProfileName.text.toString().trim()
        val host = edtHost.text.toString().trim()
        val port = edtPort.text.toString().trim().toIntOrNull() ?: 1080
        val user = edtUsername.text.toString().trim()
        val pass = edtPassword.text.toString().trim()
        val type = if (rbHttp.isChecked) "HTTP" else "SOCKS5"

        currentProfile.profileName = if (name.isNotEmpty()) name else "Proxy Server"
        currentProfile.host = host
        currentProfile.port = port
        currentProfile.username = user
        currentProfile.password = pass
        currentProfile.proxyType = type
        currentProfile.routeAllApps = switchRouteAll.isChecked
    }

    private fun validateAndSave(): Boolean {
        syncFormToProfile()
        if (currentProfile.host.isEmpty()) {
            edtHost.error = "Host IP cannot be empty"
            Toast.makeText(this, "Please enter Server Host/IP", Toast.LENGTH_SHORT).show()
            return false
        }
        ProfileStorage.saveProfiles(this, listOf(currentProfile))
        ProfileStorage.setActiveProfileId(this, currentProfile.id)
        return true
    }

    private fun checkPermissionsAndLaunchOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Grant Display Over Other Apps permission", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            return
        }

        val vpnIntent = VpnService.prepare(this)
        if (vpnIntent != null) {
            @Suppress("DEPRECATION")
            startActivityForResult(vpnIntent, 100)
        } else {
            startService(Intent(this, FloatingWidgetService::class.java))
            finish()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            startService(Intent(this, FloatingWidgetService::class.java))
            finish()
        }
    }
}
