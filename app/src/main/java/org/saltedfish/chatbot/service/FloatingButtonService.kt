package org.saltedfish.chatbot.service
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.saltedfish.chatbot.R
import java.io.ByteArrayOutputStream
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.core.content.ContextCompat.getSystemService
import kotlinx.coroutines.delay


/**
 * AI动作类型
 */
data class AIAction(
    val type: Int,
    val x: Int,
    val y: Int
) {
    companion object {
        const val TYPE_CLICK = 1
        const val TYPE_LONG_PRESS = 2
    }
}

/**
 * AI分析结果
 */
data class AIResult(
    val summaryText: String,  // 界面总结文字
    val action: AIAction?     // 可选的操作动作
)
class ViewSavedStateRegistryOwner : SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    init {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }
}
class FloatingButtonService : LifecycleService(),SavedStateRegistryOwner {
    private val savedStateRegistryController by lazy {
        SavedStateRegistryController.create(this)
    }

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    companion object {
        private const val TAG = "FloatingButtonService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "FloatingButtonChannel"

        // 悬浮窗动作事件通知
        const val ACTION_PERFORM_CLICK = "com.example.aiagent.ACTION_PERFORM_CLICK"
        const val ACTION_PERFORM_LONG_PRESS = "com.example.aiagent.ACTION_PERFORM_LONG_PRESS"
        const val EXTRA_X_COORDINATE = "x_coordinate"
        const val EXTRA_Y_COORDINATE = "y_coordinate"

        // 屏幕截图尺寸
        private const val SCREENSHOT_WIDTH = 1080
        private const val SCREENSHOT_HEIGHT = 1920
    }

    // 窗口管理器
    private lateinit var windowManager: WindowManager

    // 悬浮窗视图
    private var floatingButtonView: ComposeView? = null
    private var floatingResultView: ComposeView? = null

    // 屏幕截图相关
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var screenDensity: Int = 0
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0



    // 处理UI操作的Handler
    private val handler = Handler(Looper.getMainLooper())

    // 悬浮按钮位置状态
    private var floatingButtonPosition = mutableStateOf(IntOffset(20, 20))

    // 标记结果窗口是否正在显示
    private var isResultWindowShowing = false

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        // 初始化窗口管理器
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // 获取屏幕信息
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        screenDensity = metrics.densityDpi
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels



        // 创建前台服务通知
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // 添加悬浮按钮
        addFloatingButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        // 处理来自MainActivity的截屏权限
        if (intent?.hasExtra("resultCode") == true && intent.hasExtra("data")) {
            val resultCode = intent.getIntExtra("resultCode", 0)
            val data = intent.getParcelableExtra<Intent>("data")
            Log.e(TAG, "MediaProjection result: $resultCode $data")
            if (resultCode == Activity.RESULT_OK && data != null) {

                setupMediaProjection(resultCode, data)
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        // 清理资源
        tearDownMediaProjection()

        // 移除悬浮窗
        floatingButtonView?.let {
            windowManager.removeView(it)
            floatingButtonView = null
        }

        floatingResultView?.let {
            windowManager.removeView(it)
            floatingResultView = null
            isResultWindowShowing = false
        }

        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Button Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于保持AI Agent服务在后台运行"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("AI Agent 正在运行")
            .setContentText("点击悬浮按钮以分析当前屏幕")
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private fun addFloatingButton() {
        Log.e(TAG, "Add floating button")
        // 设置悬浮窗参数
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = floatingButtonPosition.value.x
            y = floatingButtonPosition.value.y
        }
        if (floatingButtonView!=null) {
            windowManager.addView(floatingButtonView!!, params)
            return
        }
        // 创建Compose悬浮按钮视图
        val view = ComposeView(this).apply {
            // 设置LifecycleOwner
            setViewTreeLifecycleOwner(this@FloatingButtonService)
            setViewTreeSavedStateRegistryOwner(this@FloatingButtonService)

            setContent {
                FloatingButtonContent(
                    position = floatingButtonPosition.value,
                    onPositionChange = { floatingButtonPosition.value = it },
                    onButtonClick = { takeScreenshot() }
                )
            }
        }



        // 添加到窗口
        windowManager.addView(view, params)
        floatingButtonView = view
    }

    @Composable
    private fun FloatingButtonContent(
        position: IntOffset,
        onPositionChange: (IntOffset) -> Unit,
        onButtonClick: () -> Unit
    ) {
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }

        Box(
            modifier = Modifier.size(64.dp)
        ) {
            FloatingActionButton(
                onClick = onButtonClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(56.dp).offset { position }
//                    .pointerInput(Unit) {
//                        detectDragGestures(
////                        onDragStart = { offsetX = 0f; offsetY = 0f },
//                            onDrag = { change, dragAmount ->
//                                change.consume()
//                                offsetX += dragAmount.x
//                                offsetY += dragAmount.y
//
//                                onPositionChange(
//                                    IntOffset(
//                                        (position.x + offsetX).roundToInt(),
//                                        (position.y + offsetY).roundToInt()
//                                    )
//                                )
//
//                                // 更新WindowManager中的位置
//                                handler.post {
//                                    floatingButtonView?.let { view ->
//                                        val params = view.layoutParams as WindowManager.LayoutParams
//                                        params.x = position.x
//                                        params.y = position.y
//                                        windowManager.updateViewLayout(view, params)
//                                    }
//                                }
//                            }
//                        )
//                    }
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "分析屏幕"
                )
            }
        }
    }

    private fun setupMediaProjection(resultCode: Int, data: Intent) {
        Log.e(TAG, "Setting up MediaProjection")
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

        // 创建ImageReader用于接收屏幕图像
        imageReader = ImageReader.newInstance(
            min(screenWidth, SCREENSHOT_WIDTH),
            min(screenHeight, SCREENSHOT_HEIGHT),
            PixelFormat.RGBA_8888,
            1
        )
    }

    private fun tearDownMediaProjection() {
        virtualDisplay?.release()
        virtualDisplay = null

        imageReader?.close()
        imageReader = null

        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun takeScreenshot() {
        if (mediaProjection == null || imageReader == null) {
            Log.e(TAG, "MediaProjection or ImageReader not initialized")
            return
        }

        // 创建虚拟显示以捕获屏幕
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            min(screenWidth, SCREENSHOT_WIDTH),
            min(screenHeight, SCREENSHOT_HEIGHT),
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        // 延迟一段时间以确保屏幕内容被捕获
        handler.postDelayed({
            val image = imageReader?.acquireLatestImage()

            if (image != null) {
                // 处理图像为Bitmap
                val bitmap = imageToBitmap(image)
                image.close()

                // 对截图进行AI分析
                processScreenshotWithAI(bitmap)

                // 释放虚拟显示
                virtualDisplay?.release()
                virtualDisplay = null
            } else {
                Log.e(TAG, "Failed to acquire screen image")
            }
        }, 200)  // 延迟200毫秒
    }

    private fun imageToBitmap(image: android.media.Image): Bitmap {
        val width = image.width
        val height = image.height
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        // 创建Bitmap
        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        // 裁剪到正确的尺寸
        return Bitmap.createBitmap(bitmap, 0, 0, width, height)
    }

    private fun processScreenshotWithAI(bitmap: Bitmap) {
        // 转换为字节数组
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val imageBytes = outputStream.toByteArray()

        // 调用JNI接口处理图像
        val result = AIResult(
            summaryText = "这是一个测试结果",
            action = AIAction(
                type = AIAction.TYPE_CLICK,
                x = 400,
                y = 400
            )
        )

        // 在UI线程上显示结果
        handler.post {
            showResult(result)
        }
    }

    private fun showResult(result: AIResult) {
        // 移除之前的结果窗口（如果有）
        floatingResultView?.let {
            windowManager.removeView(it)
            isResultWindowShowing = false
            floatingResultView = null
        }


        // 获取屏幕高度
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val screenHeight = metrics.heightPixels

        // 创建结果视图
        val view = ComposeView(this).apply {
            // 设置LifecycleOwner
            setViewTreeLifecycleOwner(this@FloatingButtonService)
            setViewTreeSavedStateRegistryOwner(this@FloatingButtonService)


            setContent {
                CompositionLocalProvider(
                    LocalSaveableStateRegistry provides null
                ) {
                    MaterialTheme {
                ResultWindowContent(
                    result = result,
                    onClose = {
                        windowManager.removeView(this)
                        isResultWindowShowing = false
                        floatingResultView = null
                    },
                    onExecuteAction = { action ->
                        windowManager.removeView(this)
                        isResultWindowShowing = false
                        floatingResultView = null
                        executeAction(action)
//                        wait for 1 second before showing the result window again
                        Handler(Looper.getMainLooper()).postDelayed({
                            addFloatingButton()
                        }, 1000)

                    }
                )
            }}}
        }

        // 设置窗口参数 - 半屏高度
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
//            (screenHeight).toInt(), // 屏幕高度的40%
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN ,

//        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
//        WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE

        }
        windowManager.removeView(floatingButtonView)
        // 添加到窗口
        windowManager.addView(view, params)
        floatingResultView = view
        isResultWindowShowing = true
    }

    @Composable
    private fun ResultWindowContent(
        result: AIResult,
        onClose: () -> Unit,
        onExecuteAction: (AIAction) -> Unit
    ) {
        var userInput by remember { mutableStateOf("") }
        var endPadding by remember { mutableStateOf(24.dp) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ).imePadding()
        ) {
            if (userInput.isEmpty()) {
                endPadding = 0.dp
            }else{
                endPadding = 24.dp
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp,top = 24.dp, bottom = endPadding)
            ) {
                // 顶部把手/指示器
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.White.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 用户输入框 - 集成提示语作为placeholder
                // 使用基本的 TextField 并自定义样式
                CustomInputField(
                    value = userInput,
                    onValueChange = { userInput = it }
                )
//                TextField(
//                    value = userInput,
//                    onValueChange = { userInput = it },
//                    placeholder = {
//                        Text(
//                            text = "你可以对我说...",
//                            style = MaterialTheme.typography.titleMedium,
//                            color = Color.White.copy(alpha = 0.7f)
//                        )
//                    },
//                    textStyle = LocalTextStyle.current.copy(color = Color.White),
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(bottom = 16.dp)
//                        .background(
//                            color = Color.White.copy(alpha = 0.1f),
//                            shape = RoundedCornerShape(12.dp)
//                        ),
//                    colors = TextFieldDefaults.colors(
//                        // 将所有框线和背景相关颜色设置为透明
//                        focusedIndicatorColor = Color.Transparent,
//                        unfocusedIndicatorColor = Color.Transparent,
//                        disabledIndicatorColor = Color.Transparent,
//                        errorIndicatorColor = Color.Transparent,
//                        // 设置容器为透明以便使用我们自定义的背景
//                        focusedContainerColor = Color.Transparent,
//                        unfocusedContainerColor = Color.Transparent,
//                        disabledContainerColor = Color.Transparent,
//                        errorContainerColor = Color.Transparent
//                    ),
//                    singleLine = true
//                )
                Spacer(modifier = Modifier.height(16.dp))

if (userInput.isNotEmpty()){
    // AI分析结果
    Text(
        text = result.summaryText,
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White,
        modifier = Modifier.padding(bottom = 24.dp)
    )

    // 动画波形图（简化版，实际可用图片或更复杂动画）
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(vertical = 8.dp)
            .align(Alignment.CenterHorizontally)
    ) {
        // 这里可以放一个波形动画或图片
        // 简化实现为彩色渐变条
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(3.dp)
                .align(Alignment.Center)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF00C9FF),
                            Color(0xFF92FE9D),
                            Color(0xFFFA709A)
                        )
                    ),
                    shape = RoundedCornerShape(1.5.dp)
                )
        )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 底部操作区域
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 关闭按钮
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭",
                tint = Color.White
            )
        }

        // 如果有动作，显示执行按钮
        if (result.action != null) {
            IconButton(
                onClick = { onExecuteAction(result.action) },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = when(result.action.type) {
                        AIAction.TYPE_CLICK -> "点击 (${result.action.x}, ${result.action.y})"
                        AIAction.TYPE_LONG_PRESS -> "长按 (${result.action.x}, ${result.action.y})"
                        else -> "执行操作"
                    },
                    tint = Color.White
                )
            }
        }
    }
}

            }
        }
    }

    private fun executeAction(action: AIAction) {
        // 发送广播给无障碍服务执行点击或长按操作
        val intent = Intent().apply {
            when (action.type) {
                AIAction.TYPE_CLICK -> {
                    this.action = ACTION_PERFORM_CLICK
                }
                AIAction.TYPE_LONG_PRESS -> {
                    this.action = ACTION_PERFORM_LONG_PRESS
                }
            }

            putExtra(EXTRA_X_COORDINATE, action.x)
            putExtra(EXTRA_Y_COORDINATE, action.y)
        }

        sendBroadcast(intent)
    }
}
// 在ResultWindowContent中，修改输入框部分
@Composable
fun CustomInputField(value: String, onValueChange: (String) -> Unit) {
    var focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }
    val userInputContent = remember { mutableStateOf(value) }

    // 帮助显示键盘的效果
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val view = LocalView.current // 获取当前的View

    LaunchedEffect(Unit) {
        // 延迟请求焦点，这样UI有时间设置好
        delay(100)
        focusRequester.requestFocus()
        isFocused = true

    }

    BasicTextField(
        value = userInputContent.value,
        onValueChange = {
            userInputContent.value = it
        },
        textStyle = LocalTextStyle.current.copy(color = Color.White),
        cursorBrush = SolidColor(Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .background(
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
            .focusRequester(focusRequester),
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Send
        ),
        keyboardActions = KeyboardActions(
            onSend = {
                onValueChange(userInputContent.value)
                focusManager.clearFocus()
            }
        ),
        decorationBox = { innerTextField ->
            Box {
                if (userInputContent.value.isEmpty()) {
                    Text(
                        text = "你可以对我说...",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                innerTextField()
            }
        }
    )
    if (isFocused){
        Spacer(modifier = Modifier.padding(end = 30.dp))
    }

    // 使用单独的LaunchedEffect来处理键盘显示
    LaunchedEffect(Unit) {
        delay(300) // 给UI和焦点更多时间设置好
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}

