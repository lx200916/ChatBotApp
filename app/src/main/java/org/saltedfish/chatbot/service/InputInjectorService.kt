package org.saltedfish.chatbot.service

import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.view.InputDevice
import android.view.InputEvent
import android.view.MotionEvent
import org.saltedfish.chatbot.IInputInjectorService
import rikka.shizuku.SystemServiceHelper
import java.lang.reflect.Method

class InputInjectorService :IInputInjectorService.Stub(){
    private var inputManager: Any? = null
    private var injectInputEventMethod: Method? = null
    companion object {
        private const val TAG = "InputInjectorService"
    }
    init {
        try {
            // 获取正确的 IInputManager 系统服务
            val inputManagerBinder = SystemServiceHelper.getSystemService("input")

            // 我们需要使用的是 IInputManager 而不是 InputManager
            // IInputManager 是系统内部接口，负责实际的输入注入
            val iInputManagerClass = Class.forName("android.hardware.input.IInputManager")
            val stubClass = Class.forName("android.hardware.input.IInputManager\$Stub")
            val asInterfaceMethod = stubClass.getMethod("asInterface", IBinder::class.java)

            // 通过 asInterface 获取 IInputManager 实例
            inputManager = asInterfaceMethod.invoke(null, inputManagerBinder)

            // 获取注入事件的方法
            injectInputEventMethod = iInputManagerClass.getMethod(
                "injectInputEvent",
                InputEvent::class.java,
                Int::class.java
            )

            Log.d(TAG, "成功初始化 IInputManager: ${inputManager?.javaClass?.name}")
        } catch (e: Exception) {
            Log.e(TAG, "初始化 IInputManager 失败", e)
        }
    }

    override fun destroy() {
        Log.e("InputInjectorService", "destroy")
        System.exit(0)
    }

    override fun injectTap(x: Float, y: Float) {
        try {
            if (inputManager == null || injectInputEventMethod == null) {
                Log.e(TAG, "InputManager 或 injectInputEvent 方法未初始化")
                return
            }

            // 创建按下事件
            val downTime = System.currentTimeMillis()
            var event = MotionEvent.obtain(
                downTime, downTime, MotionEvent.ACTION_DOWN,
                x.toFloat(), y.toFloat(), 0
            )
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            injectInputEventMethod?.invoke(inputManager, event, 0)
            event.recycle()

            // 创建抬起事件
            val upTime = downTime + 50
            event = MotionEvent.obtain(
                downTime, upTime, MotionEvent.ACTION_UP,
                x.toFloat(), y.toFloat(), 0
            )
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            injectInputEventMethod?.invoke(inputManager, event, 0)
            event.recycle()

            Log.d(TAG, "成功模拟点击: ($x, $y)")
        } catch (e: Exception) {
            Log.e(TAG, "模拟点击失败", e)
            throw RemoteException(e.message)
        }
    }

    override fun injectLongPress(x: Float, y: Float, duration: Int) {
        try {
            if (inputManager == null || injectInputEventMethod == null) {
                Log.e(TAG, "InputManager 或 injectInputEvent 方法未初始化")
                return
            }

            // 创建按下事件
            val downTime = System.currentTimeMillis()
            var event = MotionEvent.obtain(
                downTime, downTime, MotionEvent.ACTION_DOWN,
                x.toFloat(), y.toFloat(), 0
            )
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            injectInputEventMethod?.invoke(inputManager, event, 0)
            event.recycle()


            // 创建抬起事件
            val upTime = downTime + duration
            event = MotionEvent.obtain(
                downTime, upTime, MotionEvent.ACTION_UP,
                x.toFloat(), y.toFloat(), 0
            )
            event.source = InputDevice.SOURCE_TOUCHSCREEN
            injectInputEventMethod?.invoke(inputManager, event, 0)
            event.recycle()

            Log.d(TAG, "成功模拟长按: ($x, $y)")
        } catch (e: Exception) {
            Log.e(TAG, "模拟长按失败", e)
        }
    }

}