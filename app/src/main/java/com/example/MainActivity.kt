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
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var edtPaste: EditText
    private lateinit var switchAllApps: Switch
    private lateinit var btnSave: Button
    private lateinit var btnSelectApps: Button
    private lateinit var btnStartFloating: Button
    private var currentProfile: ProxyProfile = ProxyProfile()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edtPaste = findViewById(R.id.edtPaste)
        switchAllApps = findViewById(R.id.switchAllApps)
        btnSave = findViewById(R.id.btnSave)
        btnSelectApps = findViewById(R.id.btnSelectApps)
        btnStartFloating = findViewById(R.id.btnStartFloating)

        ProfileStorage.getActiveProfile(this)?.let {
            currentProfile = it
            val authPart = if (it.username.isNotEmpty()) ":${it.username}:${it.password}" else ""
            if (it.host.isNotEmpty()) {
                edtPaste.setText("${it.host}:${it.port}$authPart")
            }
            switchAllApps.isChecked = it.routeAllApps
        }

        findViewById<Button>(R.id.btnClipboard).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val raw = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            if (raw.isNotEmpty()) {
                edtPaste.setText(raw)
                currentProfile = ProxyProfile.parseFromRaw(raw)
                Toast.makeText(this, "Profile Parsed Automatically!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Clipboard is empty!", Toast.LENGTH_SHORT).show()
            }
        }

        btnSelectApps.setOnClickListener {
            startActivity(Intent(this, AppSelectActivity::class.java))
        }

        btnSave.setOnClickListener {
            val text = edtPaste.text.toString().trim()
            if (text.isNotEmpty()) {
                currentProfile = ProxyProfile.parseFromRaw(text)
                currentProfile.routeAllApps = switchAllApps.isChecked
                ProfileStorage.saveProfiles(this, listOf(currentProfile))
                ProfileStorage.setActiveProfileId(this, currentProfile.id)
                Toast.makeText(this, "Proxy Config Saved!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter proxy details first", Toast.LENGTH_SHORT).show()
            }
        }

        btnStartFloating.setOnClickListener {
            val text = edtPaste.text.toString().trim()
            if (text.isNotEmpty()) {
                currentProfile = ProxyProfile.parseFromRaw(text)
                currentProfile.routeAllApps = switchAllApps.isChecked
                ProfileStorage.saveProfiles(this, listOf(currentProfile))
                ProfileStorage.setActiveProfileId(this, currentProfile.id)
            }
            checkPermissionsAndLaunch()
        }
    }

    private fun checkPermissionsAndLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Grant Overlay Permission to display floating widget", Toast.LENGTH_LONG).show()
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
