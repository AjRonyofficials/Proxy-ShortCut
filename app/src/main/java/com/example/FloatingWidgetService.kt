package com.example

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.math.abs

class FloatingWidgetService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams

    private lateinit var imgBubble: ImageView
    private lateinit var statusIndicatorDot: View
    private lateinit var infoContainer: View
    private lateinit var txtCountry: TextView
    private lateinit var txtIp: TextView
    private lateinit var txtTimer: TextView

    private val timerHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("InflateParams")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        bindViews()
        setupTouchListener()
        windowManager.addView(floatingView, params)
        startTimerLoop()
    }

    private fun bindViews() {
        imgBubble = floatingView.findViewById(R.id.imgBubble)
        statusIndicatorDot = floatingView.findViewById(R.id.statusIndicatorDot)
        infoContainer = floatingView.findViewById(R.id.infoContainer)
        txtCountry = floatingView.findViewById(R.id.txtCountry)
        txtIp = floatingView.findViewById(R.id.txtIp)
        txtTimer = floatingView.findViewById(R.id.txtTimer)

        updateUi()
    }

    private fun toggleVpnConnection() {
        val targetAction = if (ProxyVpnService.isConnected) {
            ProxyVpnService.ACTION_STOP
        } else {
            val active = ProfileStorage.getActiveProfile(this)
            if (active == null || active.host.isEmpty()) {
                Toast.makeText(this, "Please configure proxy server first in app!", Toast.LENGTH_SHORT).show()
                return
            }
            ProxyVpnService.ACTION_START
        }

        val intent = Intent(this, ProxyVpnService::class.java).apply {
            action = targetAction
        }
        startService(intent)

        // Instant visual feedback
        if (targetAction == ProxyVpnService.ACTION_START) {
            imgBubble.setBackgroundResource(R.drawable.bg_floating_bubble_connected)
            imgBubble.setColorFilter(0xFF00E676.toInt())
            statusIndicatorDot.setBackgroundResource(R.drawable.bg_status_dot_connected)
            txtCountry.text = "Connecting..."
            txtCountry.setTextColor(0xFF00E676.toInt())
            txtIp.text = "Routing..."
            txtTimer.setTextColor(0xFF00E676.toInt())
        } else {
            imgBubble.setBackgroundResource(R.drawable.bg_floating_bubble_disconnected)
            imgBubble.setColorFilter(0xFFFF5252.toInt())
            statusIndicatorDot.setBackgroundResource(R.drawable.bg_status_dot_disconnected)
            txtCountry.text = "Disconnected"
            txtCountry.setTextColor(0xFFFFFFFF.toInt())
            txtIp.text = "IP: ---"
            txtTimer.text = "00:00:00"
            txtTimer.setTextColor(0xFF78909C.toInt())
        }

        Handler(Looper.getMainLooper()).postDelayed({ updateUi() }, 1000)
    }

    private fun updateUi() {
        if (ProxyVpnService.isConnected) {
            imgBubble.setBackgroundResource(R.drawable.bg_floating_bubble_connected)
            imgBubble.setColorFilter(0xFF00E676.toInt())
            statusIndicatorDot.setBackgroundResource(R.drawable.bg_status_dot_connected)
            txtCountry.setTextColor(0xFF00E676.toInt())
            txtTimer.setTextColor(0xFF00E676.toInt())
            fetchRealGeoData()
        } else {
            imgBubble.setBackgroundResource(R.drawable.bg_floating_bubble_disconnected)
            imgBubble.setColorFilter(0xFFFF5252.toInt())
            statusIndicatorDot.setBackgroundResource(R.drawable.bg_status_dot_disconnected)
            txtCountry.text = "Disconnected"
            txtCountry.setTextColor(0xFFFFFFFF.toInt())
            txtIp.text = "IP: ---"
            txtTimer.text = "00:00:00"
            txtTimer.setTextColor(0xFF78909C.toInt())
        }
    }

    private fun fetchRealGeoData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient.Builder().build()
                val request = Request.Builder()
                    .url("http://ip-api.com/json")
                    .header("User-Agent", "Mozilla/5.0")
                    .build()

                val response = client.newCall(request).execute()
                val json = JSONObject(response.body?.string() ?: "")

                val country = json.optString("countryCode", "US")
                val city = json.optString("city", "Unknown")
                val realIp = json.optString("query", "0.0.0.0")

                withContext(Dispatchers.Main) {
                    if (ProxyVpnService.isConnected) {
                        txtCountry.text = "$country • $city"
                        txtIp.text = realIp
                        txtCountry.setTextColor(0xFF00E676.toInt())
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    if (ProxyVpnService.isConnected) {
                        txtCountry.text = "Routing Active"
                        txtIp.text = "Connected"
                    }
                }
            }
        }
    }

    private fun startTimerLoop() {
        timerHandler.post(object : Runnable {
            override fun run() {
                if (ProxyVpnService.isConnected && ProxyVpnService.startTimeMs > 0) {
                    val sec = (System.currentTimeMillis() - ProxyVpnService.startTimeMs) / 1000
                    val h = sec / 3600
                    val m = (sec % 3600) / 60
                    val s = sec % 60
                    txtTimer.text = String.format("%02d:%02d:%02d", h, m, s)
                }
                timerHandler.postDelayed(this, 1000)
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        var initX = 0; var initY = 0; var touchX = 0f; var touchY = 0f
        var isClick = true

        val dragAndClickListener = View.OnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initX = params.x; initY = params.y
                    touchX = event.rawX; touchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) isClick = false
                    params.x = initX + dx
                    params.y = initY + dy
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        toggleVpnConnection()
                    }
                    true
                }
                else -> false
            }
        }

        imgBubble.setOnTouchListener(dragAndClickListener)
        infoContainer.setOnTouchListener(dragAndClickListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacksAndMessages(null)
        if (::floatingView.isInitialized) windowManager.removeView(floatingView)
    }
}
