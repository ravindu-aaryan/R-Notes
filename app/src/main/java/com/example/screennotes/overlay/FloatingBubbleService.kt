package com.example.screennotes.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.screennotes.ScreenNotesApp
import com.example.screennotes.capture.CaptureResult
import com.example.screennotes.capture.ScreenTextService
import com.example.screennotes.ui.MainActivity
import com.example.screennotes.util.openAccessibilitySettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Foreground service that shows a draggable bubble over other apps.
 * Tapping the bubble opens a menu with "Save screen as note".
 */
class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleParams: WindowManager.LayoutParams
    private var bubbleView: View? = null
    private var menuView: View? = null
    private var touchSlop = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        createChannel()
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        _running.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (bubbleView == null) addBubble()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        hideMenu()
        bubbleView?.let { windowManager.removeViewImmediate(it) }
        bubbleView = null
        _running.value = false
        super.onDestroy()
    }

    private fun addBubble() {
        val size = 56.dp()
        bubbleParams = overlayParams(size, size).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300.dp()
        }
        val view = TextView(this).apply {
            text = "N"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            textSize = 20f
            elevation = 8f
            contentDescription = "Screen Notes"
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF3F51B5.toInt())
            }
        }
        attachDrag(view) { toggleMenu() }
        windowManager.addView(view, bubbleParams)
        bubbleView = view
    }

    /** Lets the user drag the bubble. A touch that barely moves counts as a tap. */
    private fun attachDrag(view: View, onTap: () -> Unit) {
        var downX = 0f
        var downY = 0f
        var startX = 0
        var startY = 0
        var dragging = false
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = bubbleParams.x
                    startY = bubbleParams.y
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) dragging = true
                    if (dragging) {
                        bubbleParams.x = startX + dx
                        bubbleParams.y = startY + dy
                        windowManager.updateViewLayout(view, bubbleParams)
                        updateMenuPosition()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging) onTap()
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleMenu() {
        if (menuView != null) hideMenu() else showMenu()
    }

    private fun showMenu() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8.dp(), 8.dp(), 8.dp(), 8.dp())
            background = GradientDrawable().apply {
                cornerRadius = 16.dp().toFloat()
                setColor(Color.WHITE)
                setStroke(1.dp(), 0x33000000)
            }
            addView(menuItem("Save screen as note") { saveScreenAsNote() })
            addView(menuItem("Close") { hideMenu() })
        }
        val params = overlayParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        menuView = container
        menuParams = params
        updateMenuPosition()
        windowManager.addView(container, params)
    }

    private var menuParams: WindowManager.LayoutParams? = null

    private fun updateMenuPosition() {
        val view = menuView ?: return
        val params = menuParams ?: return
        params.x = bubbleParams.x + 64.dp()
        params.y = bubbleParams.y
        windowManager.updateViewLayout(view, params)
    }

    private fun hideMenu() {
        menuView?.let { windowManager.removeViewImmediate(it) }
        menuView = null
        menuParams = null
    }

    private fun menuItem(label: String, onClick: () -> Unit): TextView = TextView(this).apply {
        text = label
        textSize = 16f
        setTextColor(Color.BLACK)
        setPadding(16.dp(), 12.dp(), 16.dp(), 12.dp())
        setOnClickListener { onClick() }
    }

    private fun saveScreenAsNote() {
        hideMenu()
        val service = ScreenTextService.instance
        if (service == null) {
            toast("Turn on Screen Notes in Accessibility settings first")
            openAccessibilitySettings(this)
            return
        }
        val repository = (application as ScreenNotesApp).repository
        scope.launch {
            when (val result = service.captureScreenText()) {
                is CaptureResult.Text -> {
                    repository.saveCapture(result.text, result.source)
                    toast("Saved as note")
                }
                CaptureResult.Empty -> toast("No readable text on this screen")
                CaptureResult.NoWindow -> toast("No app window found")
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun overlayParams(width: Int, height: Int) = WindowManager.LayoutParams(
        width,
        height,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    )

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Floating button",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, FloatingBubbleService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Floating button is on")
            .setContentText("Tap the bubble to save the screen as a note")
            .setContentIntent(openApp)
            .addAction(0, "Turn off", stop)
            .setOngoing(true)
            .build()
    }

    private fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

    companion object {
        private const val CHANNEL_ID = "floating_button"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.example.screennotes.STOP_FLOATING"

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running.asStateFlow()

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, FloatingBubbleService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FloatingBubbleService::class.java))
        }
    }
}
