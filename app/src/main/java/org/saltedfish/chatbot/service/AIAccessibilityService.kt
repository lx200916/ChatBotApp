package org.saltedfish.chatbot.service

import android.R
import android.app.Notification
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.saltedfish.chatbot.IInputInjectorService
import rikka.shizuku.Shizuku
import kotlin.reflect.jvm.jvmName


class AIAccessibilityService : Service() {
    private var injectorService: IInputInjectorService? = null

    companion object {
        private const val TAG = "AIAccessibilityService"
        private const val LONG_PRESS_DURATION = 1000L // 长按时间(毫秒)
        private val USERSERVICEARGS = Shizuku.UserServiceArgs(
            ComponentName(
                "org.saltedfish.chatbot",
                InputInjectorService::class.jvmName
            )
        ).daemon(false).debuggable(true).processNameSuffix("InputInjectorService")

    }

    private val actionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return

            when (intent.action) {
                FloatingButtonService.ACTION_PERFORM_CLICK -> {
                    val x = intent.getIntExtra(FloatingButtonService.EXTRA_X_COORDINATE, 0) - 0
                    val y = intent.getIntExtra(FloatingButtonService.EXTRA_Y_COORDINATE, 0) - 100
                    performClick(x, y)
                }

                FloatingButtonService.ACTION_PERFORM_LONG_PRESS -> {
                    val x = intent.getIntExtra(FloatingButtonService.EXTRA_X_COORDINATE, 0) - 0
                    val y = intent.getIntExtra(FloatingButtonService.EXTRA_Y_COORDINATE, 0) - 100
                    performLongPress(x, y)
                }
            }
        }
    }

    override fun onCreate() {

        super.onCreate()
//        val notification: Notification = NotificationCompat.Builder(
//            this, "YOUR_CHANNEL_ID"
//        )
//            .setContentTitle("前台服务运行中")
//            .setContentText("这是一个前台服务")
////            .setSmallIcon(R.mipmap.sym_def_app_icon)
//            .build()
//        startForeground(1, notification)
        // 注册广播接收器
        val filter = IntentFilter().apply {
            addAction(FloatingButtonService.ACTION_PERFORM_CLICK)
            addAction(FloatingButtonService.ACTION_PERFORM_LONG_PRESS)
        }
        Log.e("TAG", "registerReceiver")
        registerReceiver(actionReceiver, filter, RECEIVER_NOT_EXPORTED)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Shizuku.bindUserService(
            USERSERVICEARGS, object : ServiceConnection {


                override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
                    injectorService = IInputInjectorService.Stub.asInterface(p1)
                    Log.e(TAG, "InputInjectorService connected")
                }

                override fun onServiceDisconnected(p0: ComponentName?) {
                    injectorService = null
                    Log.e(TAG, "InputInjectorService disconnected")
                }
            })
        return START_STICKY
    }
    override fun onDestroy() {
        // 注销广播接收器
        unregisterReceiver(actionReceiver)
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }


    private fun performClick(x: Int, y: Int) {
        if (injectorService == null) {
            Log.e(TAG, "InputInjectorService is not connected")
            return
        }
        try {
            injectorService?.injectTap(x.toFloat(), y.toFloat())
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting tap event", e)
        }


    }

    private fun performLongPress(x: Int, y: Int) {
        if (injectorService == null) {
            Log.e(TAG, "InputInjectorService is not connected")
            return
        }
        try {
            injectorService?.injectLongPress(x.toFloat(), y.toFloat(), LONG_PRESS_DURATION.toInt())
        } catch (e: Exception) {
            Log.e(TAG, "Error injecting long press event", e)

        }
    }
}