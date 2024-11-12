package org.saltedfish.chatbot

import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.commandiron.compose_loading.Wave
import com.origeek.imageViewer.previewer.ImagePreviewer
import com.origeek.imageViewer.previewer.ImagePreviewerState
import com.origeek.imageViewer.previewer.rememberPreviewerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.saltedfish.chatbot.ui.theme.ChatBotTheme
import org.saltedfish.chatbot.ui.theme.Purple80
import kotlin.reflect.KFunction
import kotlin.reflect.full.memberFunctions

fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                JNIBridge.stop()
                this.startActivity(intent)
            }

        }
        //enableEdgeToEdge()
        setContent {
            ChatBotTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { Home(navController) }
                    composable(
                        "chat/{id}?type={type}&device={device}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType },
                            navArgument("type") { type = NavType.IntType;defaultValue = 0 },
                            navArgument("device") { type = NavType.IntType;defaultValue = 0 }
                            )
                    ) {
                        Chat(navController, it.arguments?.getInt("type") ?: 3, it.arguments?.getInt("id") ?: 0, it.arguments?.getInt("device") ?: 0)
                    }
                    composable("photo") {
                        Photo(navController)
                    }
                    composable("vqa") {
                        VQA(navController)
                    }
                    // A surface container using the 'background' color from the theme


                }
            }
        }
    }
    val functionsMap: Map<String, KFunction<*>> by lazy {
        this::class.memberFunctions
            .associateBy { it.name }
    }
    val functions  by lazy {
         Functions(context = this, outerFunctionsMap = functionsMap)
    }
    fun web_search(query: String, engine: String="google") {
        // use google or baidu to search
        // directly open the browser using action view
        val searchUri = when (engine.lowercase()) {
            "google" -> Uri.parse("https://www.google.com/search?q=$query")
            "baidu" -> Uri.parse("https://www.baidu.com/s?wd=$query")
            else -> Uri.parse("https://www.google.com/search?q=$query")
        }
        val intent = Intent(Intent.ACTION_VIEW, searchUri)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    fun open_settings(setting_type:String = "general"){
        val intent = when (setting_type) {
            "general" -> Intent(Settings.ACTION_SETTINGS)
            "wireless" -> Intent(Settings.ACTION_WIRELESS_SETTINGS)
            "airplane_mode" -> Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "apn" -> Intent(Settings.ACTION_APN_SETTINGS)
            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "date" -> Intent(Settings.ACTION_DATE_SETTINGS)
            "locale" -> Intent(Settings.ACTION_LOCALE_SETTINGS)
            "input_method" -> Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
            "security" -> Intent(Settings.ACTION_SECURITY_SETTINGS)
            "location" -> Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            "internal_storage" -> Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            "memory_card" -> Intent(Settings.ACTION_MEMORY_CARD_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS) // Default to general settings if unknown type
        }

        // Check if there is an activity available to handle this intent
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    fun search_location(query: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("geo:0,0?q=$query")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

//    fun INTENT_ACTION_VIDEO_CAMERA() {
//        val intent = Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA)
//        if (intent.resolveActivity(packageManager) != null) {
//            startActivity(intent)
//        }
//    }
//
//    fun INTENT_ACTION_STILL_IMAGE_CAMERA() {
//        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
//        if (intent.resolveActivity(packageManager) != null) {
//            startActivity(intent)
//        }
//    }
//
//    fun ACTION_VIDEO_CAPTURE(): String {
//        val videoURI = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, ContentValues())
//        videoURI?.let { uri ->
//            takeVideoLauncher.launch(uri)
//        }
//        latch.await()
//        latch = CountDownLatch(1)
//        return videoURI.toString()
//    }
//
//    fun ACTION_IMAGE_CAPTURE(): String {
//        val photoURI = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues())
//        photoURI?.let { uri ->
//            takePicLauncher.launch(uri)
//        }
//        latch.await()
//        latch = CountDownLatch(1)
//        return photoURI.toString()
//    }
//
//    fun ACTION_GET_RINGTONE(): String {
//        getRingtone.launch(RingtoneManager.TYPE_ALL)
//        latch.await()
//        latch = CountDownLatch(1)
//        return uri
//    }
//
//    fun ACTION_CREATE_DOCUMENT(mime_type: String, initial_name: String): String {
//        creatDocumentLauncher.launch(mapOf("mime_type" to mime_type, "file_name" to initial_name))
//        latch.await()
//        latch = CountDownLatch(1)
//        return uri
//    }
//
//    fun ACTION_OPEN_DOCUMENT(mime_type: List<String>, allow_multiple: Boolean=false): List<String> {
//        if (allow_multiple) {
//            openMultipleDocumentLauncher.launch(mime_type.toTypedArray())
//            latch.await()
//            latch = CountDownLatch(1)
//            return uris
//        } else {
//            openDocumentLauncher.launch(mime_type.toTypedArray())
//            latch.await()
//            latch = CountDownLatch(1)
//            return listOf(uri)
//        }
//    }
//
//    fun ACTION_GET_CONTENT(mime_type: String, allow_multiple: Boolean=false): List<String> {
//        if (allow_multiple) {
//            getMultipleContents.launch(mime_type)
//            latch.await()
//            latch = CountDownLatch(1)
//            return uris
//        } else {
//            getContent.launch(mime_type)
//            latch.await()
//            latch = CountDownLatch(1)
//            return listOf(uri)
//        }
//    }

//    fun ACTION_PICK(data_type: String="ALL"): String {
//        getContact.launch(data_type)
//        latch.await()
//        latch = CountDownLatch(1)
//        return uri
//    }
//
//
//    fun ACTION_VIEW_CONTACT(contact_uri: String) {
//        val contactUri = Uri.parse(uri)
//        val intent = Intent(Intent.ACTION_VIEW, contactUri)
//        if (intent.resolveActivity(packageManager) != null) {
//            startActivity(intent)
//        }
//    }

    fun send_message(phone_number: String, subject: String, body: String, attachments: List<String>? = null) {
        val intent = when {
            attachments.isNullOrEmpty() -> Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phone_number") // 只设置短信协议
            }
            attachments.size == 1 -> Intent(Intent.ACTION_SEND).apply {
                type = "*/*" // 单个附件，使用通用 MIME 类型
                putExtra(Intent.EXTRA_STREAM, Uri.parse(attachments.first()))
            }
            else -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*" // 多个附件，使用通用 MIME 类型
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachments.map { Uri.parse(it) }))
            }
        }

        // 设置短信的基本信息
        intent.putExtra("sms_body", body)
        intent.putExtra("subject", subject)

        // 检查是否有应用可以处理这个 Intent
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(intent, "Choose a messaging app:"))
        }
    }

    fun send_email(
        to: List<String>,
        subject: String,
        body: String,
        cc: List<String>? = null,
        bcc: List<String>? = null,
        attachments: List<String>? = null
    ) {
        // 根据附件数量选择合适的 Intent 动作
        val intent = when {
            attachments.isNullOrEmpty() -> Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // 只设置邮件协议
            }
            attachments.size == 1 -> Intent(Intent.ACTION_SEND).apply {
                type = "*/*" // 单个附件，使用通用 MIME 类型
                putExtra(Intent.EXTRA_STREAM, Uri.parse(attachments.first()))
            }
            else -> Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*" // 多个附件，使用通用 MIME 类型
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(attachments.map { Uri.parse(it) }))
            }
        }

        // 设置邮件的基本信息
        intent.putExtra(Intent.EXTRA_EMAIL, to.toTypedArray())
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, body)
        cc?.let { intent.putExtra(Intent.EXTRA_CC, it.toTypedArray()) }
        bcc?.let { intent.putExtra(Intent.EXTRA_BCC, it.toTypedArray()) }

        // 检查是否有应用可以处理这个 Intent
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(intent, "Choose an Email client:"))
        }
    }

    fun get_contact_info_from_uri(uri: String, key: String): String {
        val contactUri = Uri.parse(uri)

        val dataMap = mapOf(
            "email" to ContactsContract.CommonDataKinds.Email.ADDRESS,
            "phone" to ContactsContract.CommonDataKinds.Phone.NUMBER,
            "address" to ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS
        )
        contentResolver.query(contactUri, null, null, null, null)?.use { cursor->
            if (cursor.moveToFirst()) {
                val column = dataMap[key]
                val dataIdx = cursor.getColumnIndex(column)
                return cursor.getString(dataIdx)
            }
        }
        return ""
    }

    fun dial(phone_number: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phone_number")
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    data class ContactDetail(
        val uri: Uri,
        val column: String,
        val selection: String,
        val extraArgs: Array<String>? = null
    )

    @SuppressLint("Range")
    private fun queryContactDetail(uri: Uri, column: String, selection: String, selectionArgs: Array<String>): String {
        contentResolver.query(uri, arrayOf(column), selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndex(column))
            }
        }
        return ""
    }

    @SuppressLint("Range")
    fun get_contact_info(name: String, key: String): String {
        val uri = ContactsContract.Contacts.CONTENT_FILTER_URI.buildUpon().appendPath(name).build()
        val key_lower = key.lowercase()
        val detailsMap = mapOf(
            "email" to ContactDetail(
                uri = ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                column = ContactsContract.CommonDataKinds.Email.ADDRESS,
                selection = "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?"
            ),
            "phone" to ContactDetail(
                uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                column = ContactsContract.CommonDataKinds.Phone.NUMBER,
                selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
            ),
//            "company" to ContactDetail(
//                uri = ContactsContract.CommonDataKinds.Organization.CONTENT_URI,
//                column = ContactsContract.CommonDataKinds.Organization.COMPANY,
//                selection = "${ContactsContract.CommonDataKinds.Organization.CONTACT_ID} = ?"
//            ),
            "address" to ContactDetail(
                uri = ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
                column = ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                selection = "${ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID} = ?"
            )
        )

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID))
                val detail = detailsMap[key_lower]
                if (key_lower == "uri") {
                    cursor.apply {
                        // Gets the lookup key column index
                        val lookupKeyIndex = getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
                        // Gets the lookup key value
                        val currentLookupKey = getString(lookupKeyIndex)
                        // Gets the _ID column index
                        val idIndex = getColumnIndex(ContactsContract.Contacts._ID)
                        val currentId = getLong(idIndex)
                        val selectedContactUri = ContactsContract.Contacts.getLookupUri(currentId, currentLookupKey)
                        return selectedContactUri.toString()
                    }
                }
                detail?.let {
                    return queryContactDetail(it.uri, it.column, it.selection, arrayOf(contactId, *(it.extraArgs ?: emptyArray())))
                }
            }
        }
        return ""
    }

    fun ACTION_EDIT_CONTACT(contact_uri: String, contact_info: Map<String, String>) {
        val intent = Intent(Intent.ACTION_EDIT).apply {
            data = Uri.parse(contact_uri)

            // Set the Name, Email, and other contact fields from the map
            contact_info["name"]?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
            contact_info["email"]?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
            contact_info["phone"]?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
            contact_info["company"]?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
            contact_info["address"]?.let { putExtra(ContactsContract.Intents.Insert.POSTAL, it) }
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    fun ACTION_INSERT_CONTACT(contact_info: Map<String, String>) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE

            // Set the Name, Email, and other contact fields from the map
            contact_info["name"]?.let { putExtra(ContactsContract.Intents.Insert.NAME, it) }
            contact_info["email"]?.let { putExtra(ContactsContract.Intents.Insert.EMAIL, it) }
            contact_info["phone"]?.let { putExtra(ContactsContract.Intents.Insert.PHONE, it) }
            contact_info["company"]?.let { putExtra(ContactsContract.Intents.Insert.COMPANY, it) }
            contact_info["address"]?.let { putExtra(ContactsContract.Intents.Insert.POSTAL, it) }
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun ACTION_INSERT_EVENT(
        TITLE: String,
        DESCRIPTION: String,
        EVENT_LOCATION: String?=null,
        EXTRA_EVENT_ALL_DAY: Boolean = false,
        EXTRA_EVENT_BEGIN_TIME: String? = null,
        EXTRA_EVENT_END_TIME: String? = null,
        EXTRA_EMAIL: List<String>? = null
    ) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, TITLE)
            putExtra(CalendarContract.Events.DESCRIPTION, DESCRIPTION)

            putExtra(CalendarContract.Events.ALL_DAY, EXTRA_EVENT_ALL_DAY)

            EVENT_LOCATION?.let {
                putExtra(CalendarContract.Events.EVENT_LOCATION, EVENT_LOCATION)
            }
            // Convert ISO 8601 string times to milliseconds since epoch
            EXTRA_EVENT_BEGIN_TIME?.let {
                putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, iso8601ToMillis(it))
            }
            EXTRA_EVENT_END_TIME?.let {
                putExtra(CalendarContract.EXTRA_EVENT_END_TIME, iso8601ToMillis(it))
            }

            // If there are emails, add them as attendees
            EXTRA_EMAIL?.let {
                putExtra(Intent.EXTRA_EMAIL, it.toTypedArray())
            }
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun iso8601ToMillis(dateTime: String): Long {
        val formatter = java.time.format.DateTimeFormatter.ISO_DATE_TIME
        val localDateTime = java.time.LocalDateTime.parse(dateTime, formatter)
        val zonedDateTime = localDateTime.atZone(java.time.ZoneId.systemDefault())
        return zonedDateTime.toInstant().toEpochMilli()
    }

    fun ACTION_SHOW_ALARMS() {
        val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    fun ACTION_SET_TIMER(
        duration: String,
        EXTRA_MESSAGE: String = "",
        EXTRA_SKIP_UI: Boolean = true
    ) {
        val EXTRA_LENGTH = parseDuration(duration)
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, EXTRA_LENGTH)
            putExtra(AlarmClock.EXTRA_MESSAGE, EXTRA_MESSAGE)
            putExtra(AlarmClock.EXTRA_SKIP_UI, EXTRA_SKIP_UI)
        }

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    fun parseDuration(input: String): Int {
        // 匹配一个或多个数字，后面可能有任意数量的空格，然后是时间单位的完整形式或简写形式
        val regex = "(\\d+)\\s*(hours?|h|minutes?|m|seconds?|s)".toRegex()
        var totalSeconds = 0

        regex.findAll(input).forEach { matchResult ->
            val (number, unit) = matchResult.destructured
            totalSeconds += when (unit.lowercase()) {
                "hour", "hours", "h" -> number.toInt() * 3600
                "minute", "minutes", "m" -> number.toInt() * 60
                "second", "seconds", "s" -> number.toInt()
                else -> 0
            }
        }

        return totalSeconds
    }

    fun ACTION_SET_ALARM(
        EXTRA_HOUR: Int,
        EXTRA_MINUTES: Int,
        EXTRA_MESSAGE: String = "",
        EXTRA_DAYS: List<String>? = null,
        EXTRA_RINGTONE: String? = null,
        EXTRA_VIBRATE: Boolean = false,
        EXTRA_SKIP_UI: Boolean = true
    ) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, EXTRA_HOUR)
            putExtra(AlarmClock.EXTRA_MINUTES, EXTRA_MINUTES)
            putExtra(AlarmClock.EXTRA_MESSAGE, EXTRA_MESSAGE)
            putExtra(AlarmClock.EXTRA_SKIP_UI, EXTRA_SKIP_UI)
            putExtra(AlarmClock.EXTRA_VIBRATE, EXTRA_VIBRATE)

            // Handle days of the week; this part is not directly supported by the standard alarm intent,
            // so usually alarms are set for the next occurrence of the specified time.
            EXTRA_DAYS?.let {
                val dayList = ArrayList<Int>()
                it.forEach{day->
                    dayList.add(dayOfWeekToInt(day))
                }
                putExtra(AlarmClock.EXTRA_DAYS, dayList)
            }
            EXTRA_RINGTONE?.let {
                putExtra(AlarmClock.EXTRA_RINGTONE, EXTRA_RINGTONE)
            }
        }

        if (intent.resolveActivity(packageManager) != null){
            startActivity(intent)
        }
    }

    fun dayOfWeekToInt(day: String): Int {
        return when (day.lowercase()) {
            "monday" -> Calendar.MONDAY
            "tuesday" -> Calendar.TUESDAY
            "wednesday" -> Calendar.WEDNESDAY
            "thursday" -> Calendar.THURSDAY
            "friday" -> Calendar.FRIDAY
            "saturday" -> Calendar.SATURDAY
            "sunday" -> Calendar.SUNDAY
            else -> throw IllegalArgumentException("$day is not an illegal day of a week")
        }
    }
}
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun rememberModalBottomSheetState(
//    skipPartiallyExpanded: Boolean = false,
//    confirmValueChange: (SheetValue) -> Boolean = { true },
//) = rememberSaveable(
//    skipPartiallyExpanded, confirmValueChange,
//    saver = SheetState.Saver(
//        skipPartiallyExpanded = skipPartiallyExpanded,
//        confirmValueChange = confirmValueChange
//    )
//) {
//    SheetState(skipPartiallyExpanded, SheetValue.Expanded, confirmValueChange, true)
//}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VQA(navController: NavController, viewModel: VQAViewModel = viewModel()) {
    val selectedMessage by viewModel.selectedMessage.observeAsState()
    val answer by viewModel.answerText.observeAsState()
    val messages = viewModel.messages;
    val context = LocalContext.current
    LaunchedEffect(key1 = false) {
        viewModel.initStatus(context)
    }
    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) {

        Box(modifier = Modifier.padding(it)) {
            Image(
                painter = painterResource(id = R.drawable.chat_record_demo),
                contentDescription = "Demo",
                Modifier.fillMaxSize()
            )

            NeverHideBottomSheet {
                BackHandler {
                    if (selectedMessage != null && selectedMessage != -1) {
                        viewModel.setSelectedMessage(-1)
                    } else {
                        navController.popBackStack()
                    }
                }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
//                        Text(text = "🤖", fontSize = 32.sp, modifier = Modifier.padding(end = 10.dp))
                        Image(
                            painter = painterResource(id = R.drawable.robot),
                            contentDescription = "",
                            Modifier
                                .padding(end = 10.dp)
                                .size(56.dp)
                        )
                        Column {
                            Text(
                                text = "Ask Me Something",
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "About the screen",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (selectedMessage == -1 || selectedMessage == null) {
                        Text(
                            text = "You may want to ask",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontStyle = FontStyle.Italic
                        )
                        FlowRow {
                            messages.forEachIndexed { index: Int, s: String ->
                                FilterChip(label = {
                                    Text(text = s)
                                },
                                    selected = true,
                                    onClick = { viewModel.setSelectedMessage(index) },

                                    leadingIcon = {
                                        Image(
                                            modifier = Modifier.size(16.dp),
                                            painter = painterResource(id = R.drawable.star_filled),
                                            contentDescription = ""
                                        )
                                    })
                            }

                        }
                    } else {
                        Text(
                            text = messages[selectedMessage!!],
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontStyle = FontStyle.Italic
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        if (answer == null) Box(
                            modifier = Modifier
                                .height(80.dp)
                                .fillMaxWidth(), contentAlignment = Alignment.Center
                        ) {
                            Wave(
                                size = 50.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )

                        } else {
                            Text(
                                text = answer!!,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }

//                        Text(
//                            text = "Answer",
//                            fontWeight = FontWeight.SemiBold,
//                            fontSize = 18.sp,
//                            fontFamily = FontFamily.Monospace,
//                            fontStyle = FontStyle.Italic
//                        )
                    }

                }
            }

        }
    }

}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun Photo(navController: NavController, viewModel: PhotoViewModel = viewModel()) {
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }
    val message by viewModel.message.observeAsState()

    val resultContracts =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
            // Handle the returned Uri
            it?.let {
                bitmap.value = it

                viewModel.setBitmap(it)
            }
            if (it == null) {
                navController.popBackStack()
            }
        }
    LaunchedEffect(key1 = bitmap) {
        if (bitmap.value == null) {
            resultContracts.launch(null)
        }
    }
    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(title = {
                Text(
                    text = "Photo", fontWeight = FontWeight.Bold, fontSize = 24.sp
                )
            }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
            })
        },
    ) {
        Column(
            modifier = Modifier.fillMaxHeight()

        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(it)
                    .consumeWindowInsets(it)
                    .systemBarsPadding()
                    .padding(top = 16.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                bitmap.value?.let {
                    // round corner
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.FillHeight,
                        // make the image height of 1/3
                        modifier = Modifier
                            .fillMaxHeight(0.3f)
                            .heightIn(min = 60.dp)
                            .widthIn(min = 60.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                ChatBubble(message = Message("👆🏻 What's this?", true, 0))
                message?.let {
                    ChatBubble(message = it)
                }


            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Home(navController: NavController) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(0) }
    var selectedBackend by remember { mutableStateOf(0) }
    val modelNames = listOf("PhoneLM","Qwen 2.5")
    val deviceNames = listOf("CPU","NPU")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        contentWindowInsets = WindowInsets(16, 20, 16, 0),
        topBar = {
            Greeting("Android")
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.padding(bottom = 10.dp),
                icon = { Icon(Icons.Rounded.Settings, "Star Us!") },
                text = { Text(text = "Model Settings") },
                onClick = {
                    // visit Github!
//                    val intent = Intent(Intent.ACTION_VIEW)
//                    intent.data = Uri.parse("https://github.com/UbiquitousLearning/mllm")
//                    context.startActivity(intent)
                    showBottomSheet = true
                },
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(it)

        ) {
            MainEntryCards(navController = navController, selectedIndex = selectedIndex, selectedBackend = selectedBackend)
        }
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        "Choose a Instructed LLM for non-multimodal tasks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        modelNames.forEachIndexed { index, s ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = modelNames.size
                                ),
                                selected = selectedIndex == index,

                                onClick = {
                                    selectedIndex = index
                                }
                            ) {
                                Text(text = s)
                            }
                        }

                    }
                    Text(
                        "Choose a Backend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                        deviceNames.forEachIndexed { index, s ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = modelNames.size
                                ),
                                selected = selectedBackend == index,

                                onClick = {
                                    selectedBackend = index
                                }
                            ) {
                                Text(text = s)
                            }
                        }

                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Chat(navController: NavController, chatType: Int = 0,modelId:Int=0,deviceId:Int=0, vm: ChatViewModel = viewModel()) {
    LaunchedEffect(key1 = chatType, key2 = modelId) {
        vm.setModelType(chatType)
        vm.setModelId(modelId)
    }

    val previewUri by vm.previewUri.observeAsState()
    val messages by vm.messageList.observeAsState(mutableListOf())
    val context = LocalContext.current
    val isBusy by vm.isBusy.observeAsState(false)
    val isLoading by vm.isLoading.observeAsState(true)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val photoList by vm.photoList.observeAsState(listOf())
    val imageViewerState = rememberPreviewerState(pageCount = { photoList.size })
    val modelType by vm.modelType.observeAsState(0)
    val isExternalStorageManager = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }
    if (!isExternalStorageManager) {
        vm._isExternalStorageManager.value = false
        // request permission with ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION

    } else {
        vm._isExternalStorageManager.value = true
        LaunchedEffect(key1 = true) {
            vm.initStatus(context, chatType)
            vm._scrollstate = scrollState
            vm.functions_ = (context.getActivity() as MainActivity).functions
            vm.docVecDB = DocumentVecDB
            vm.docVecDB?.init(context,"api_vec.jsonl")
        }
    }
    LaunchedEffect(key1 = isBusy) {
        scrollState.animateScrollTo(scrollState.maxValue)

    }
    Scaffold(modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (imageViewerState.visible) null else CenterAlignedTopAppBar(title = {
                Text(
                    text = "Chat", fontWeight = FontWeight.Bold, fontSize = 24.sp
                )
            }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
            })
        },
        bottomBar = {
            if (imageViewerState.visible) null else BottomAppBar {
                ChatInput(!isBusy && !isLoading, withImage = modelType == 1, onImageSelected = {
                    vm.setPreviewUri(it)
                }
                ) {
                    //TODO
                    //Get timestamp
//                    vm.sendInstruct(context, it)
                    vm.sendMessage(context,it)
                }
            }
        }) {

        Column(
            modifier = Modifier.fillMaxHeight()

        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(it)
                    .consumeWindowInsets(it)
                    .systemBarsPadding()
                    .verticalScroll(scrollState)
            ) {
//                ChatBubble(message = Message("Hello", true, 0))
                if (!isLoading) ChatBubble(
                    message = Message(
                        if (modelType == 3) "Hi! I am a AI ChatBot. How can I help you today?" else "Hi! I am a Phone Assistant!How can I assist you today?",
                        false,
                        0
                    )
                )
                messages.forEach {
                    ChatBubble(message = it, vm, scope, imageViewerState)
                }
//                Spacer(
//                    modifier = Modifier
//                        .weight(1f)
//
//                )
            }


        }
        if (previewUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(it)
                    .consumeWindowInsets(it)
                    .systemBarsPadding()
                    .background(Color.Transparent)
            ) {
                PreviewBubble(previewUri!!)

            }
        }
//            if (isLoading) Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(it)
//                    .consumeWindowInsets(it)
//                    .systemBarsPadding()
//                    .background(Color.Transparent)
//            ) {
//               Column (
//                   Modifier.align(Alignment.Center),
//                   horizontalAlignment = Alignment.CenterHorizontally,
//                   verticalArrangement = Arrangement.Center
//                   ){
//                   Wave(
//                       size = 100.dp,
//                       color = MaterialTheme.colorScheme.onPrimaryContainer,
//                   )
//                   Spacer(modifier = Modifier.height(80.dp))
//                   Text(text = "Loading Model...",color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
//                   )
//
//               }
//
//        }

        ImagePreviewer(state = imageViewerState, imageLoader = { pageIndex ->
            rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current).data(data = photoList[pageIndex].uri)
                    .size(coil.size.Size.ORIGINAL).build()
            )
        }, detectGesture = {
            onTap = {
                scope.launch {
                    imageViewerState.close()
                }
            }
        })
    }

}

fun getBubbleShape(
    density: Density,
    cornerRadius: Dp,
    arrowWidth: Dp,
    arrowHeight: Dp,
    arrowOffset: Dp
): GenericShape {

    val cornerRadiusPx: Float
    val arrowWidthPx: Float
    val arrowHeightPx: Float
    val arrowOffsetPx: Float

    with(density) {
        cornerRadiusPx = cornerRadius.toPx()
        arrowWidthPx = arrowWidth.toPx()
        arrowHeightPx = arrowHeight.toPx()
        arrowOffsetPx = arrowOffset.toPx()
    }

    return GenericShape { size: Size, layoutDirection: LayoutDirection ->

        this.addRoundRect(
            RoundRect(
                rect = Rect(
                    offset = Offset(0f, 0f),
                    size = Size(size.width, size.height - arrowHeightPx)
                ),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )
        )
        moveTo(arrowOffsetPx, size.height - arrowHeightPx)
        lineTo(arrowOffsetPx + arrowWidthPx / 2, size.height)
        lineTo(arrowOffsetPx + arrowWidthPx, size.height - arrowHeightPx)

    }
}

@Composable
fun BoxScope.PreviewBubble(preview: Uri) {
    val density = LocalDensity.current
    val arrowHeight = 8.dp

    val bubbleShape = remember {
        getBubbleShape(
            density = density,
            cornerRadius = 10.dp,
            arrowWidth = 20.dp,
            arrowHeight = arrowHeight,
            arrowOffset = 30.dp
        )
    }
    Box(

        modifier = Modifier
            .align(Alignment.BottomStart)
            .shadow(10.dp, bubbleShape)
            .padding(start = 5.dp)
            .fillMaxWidth(0.2f)
            .background(MaterialTheme.colorScheme.primaryContainer)

    ) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current).data(data = preview)
                    .size(coil.size.Size.ORIGINAL).build()
            ),
            contentDescription = "Image Description",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = arrowHeight)
                .clip(RoundedCornerShape(10.dp))
        )

    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatInput(
    enable: Boolean,
    withImage: Boolean,
    onImageSelected: (Uri?) -> Unit = {},

    onMessageSend: (Message) -> Unit = {}
) {
    var text by remember { mutableStateOf("") }
    var imageUri = remember { mutableStateOf<Uri?>(null) }

//softkeyborad
    val keyboardController = LocalSoftwareKeyboardController.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        it?.let {
            imageUri.value = it
            onImageSelected(it)
        }

    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        if (withImage) IconButton(onClick = {
            launcher.launch(
                PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }, Modifier.padding(10.dp)) {
            Image(
                painter = painterResource(id = if (imageUri.value != null) R.drawable.add_done else R.drawable.add_other),
                contentDescription = "Add Other Resources.",
                Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        OutlinedTextField(
//            enabled = enable,
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
                .weight(1f)
                .padding(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White.copy(0.5f),
                focusedContainerColor = Color.White.copy(0.5f),

                )

        )
        IconButton(onClick = {
            keyboardController?.hide()
            val punctuation = listOf('.', '?', '!', ',', ';', ':', '。', '？', '！', '，', '；', '：')
            if (text.isNotEmpty() && !punctuation.contains(text.last()) && text.last() != '\n') text += "."
            onMessageSend(
                Message(
                    text,
                    true,
                    0,
                    type = if (imageUri.value == null) MessageType.TEXT else MessageType.IMAGE,
                    content = imageUri.value
                )
            );text = "";imageUri.value = null;onImageSelected(null)
        }, enabled = enable) {
            Icon(
                painter = painterResource(id = R.drawable.up),
                contentDescription = "Send",
                Modifier.size(36.dp)
            )
        }

    }

}

@Composable
fun ColumnScope.ChatBubble(
    message: Message,
    vm: ChatViewModel? = null,
    scope: CoroutineScope = rememberCoroutineScope(),
    imageViewerState: ImagePreviewerState = rememberPreviewerState(pageCount = { 1 })
) {
    if (message.text.isNotEmpty()) ChatBubbleBox(isUser = message.isUser) {
        SelectionContainer {
            Text(text = message.text, fontSize = 18.sp)
        }
    }
    if (message.type == MessageType.IMAGE && vm != null) ChatBubbleBox(isUser = message.isUser) {

        (message.content as Uri?)?.let { image ->
            val photo = vm.photoList.value!!.find { it.uri == image }
            val (painter, id) = if (photo == null) {
                val requests = ImageRequest.Builder(LocalContext.current).data(data = image).build()
                val painter_ = rememberAsyncImagePainter(
                    requests
                )
                val photo_ = Photo(uri = image, request = requests)
                val id = vm.addPhoto(photo_)
                painter_ to id
            } else {
                rememberAsyncImagePainter(photo.request) to photo.id
            }
            Image(painter = painter,
                contentDescription = "Image Description",
                modifier = Modifier
                    .clickable {
                        scope.launch {
                            imageViewerState.open(id)
                        }
                    }
                    .size(200.dp)
                    .clip(RoundedCornerShape(20.dp)))

        }

    }
}

@Composable
fun ColumnScope.ChatBubbleBox(isUser: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .padding(5.dp)
            .align(if (isUser) Alignment.End else Alignment.Start)
            .clip(
                RoundedCornerShape(
                    topStart = 48f,
                    topEnd = 48f,
                    bottomStart = if (isUser) 48f else 0f,
                    bottomEnd = if (isUser) 0f else 48f
                )
            )
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp)
            .widthIn(max = 300.dp)
    ) {
        content()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {

    Column(modifier = modifier.padding(top = 56.dp, start = 20.dp)) {
        Text(
            text = "Let's Chat",
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 30.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        // Subtitle
        Text(
            text = "See what I can do for you",
            style = MaterialTheme.typography.titleLarge,
            lineHeight = 24.sp

        )
    }

}

@Composable
fun MainEntryCards(modifier: Modifier = Modifier, navController: NavController,selectedIndex:Int = 0,selectedBackend:Int = 0) {
    val context = LocalContext.current

    Column(
        Modifier
            .padding(8.dp)
            .padding(top = 10.dp)
    ) {
//        Text("Choose a Instructed LLM \n for non-multimodal tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
//        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()){
//            options.forEachIndexed { index, s ->
//                SegmentedButton(
//                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
//                    selected = selectedIndex == index,
//
//                    onClick = {
//                        selectedIndex = index
//                    }
//                ){
//                    Text(text = s)
//                }
//            }
//
//        }
////        HorizontalDivider()
//        Spacer(modifier = Modifier.height(8.dp))

        Row {
            EntryCard(icon = R.drawable.text,
                backgroundColor = Color(0xEDADE6AA),
                title = "Chat",
                subtitle = "\" The meaning of life is ....\"",
                onClick = { navController.navigate("chat/$selectedIndex?type=3") })
            Spacer(Modifier.width(8.dp))
            EntryCard(icon = R.drawable.image,
                backgroundColor = Purple80,
                title = "Image Reader",
                subtitle = "\" say..How many stars in the sky?\"",
                onClick = { navController.navigate("chat/$selectedIndex?type=1") }

            )

        }
        Spacer(Modifier.height(8.dp))
        Row {
            EntryCard(icon = R.drawable.tools,
                // Pick up a pink
                backgroundColor = Color(0xEDF8BBD0),
                title = "Takeover My Phone",
                subtitle = "\" Show me the power\"",
                onClick = { navController.navigate("chat/$selectedIndex?type=4") })
            Spacer(Modifier.width(8.dp))
            EntryCard(icon = R.drawable.more_text,
                // pick a blue style
                backgroundColor = Color(0xEDB3E5FC),
                title = "Read My Screen",
                subtitle = "",
                onClick = {
                    // visit Github!
//                    val intent = Intent(Intent.ACTION_VIEW)
//                    intent.data = Uri.parse("https://github.com/UbiquitousLearning/mllm")
//                    context.startActivity(intent)
                    navController.navigate("vqa")
                }

            )
        }

    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.EntryCard(
    icon: Int,
    backgroundColor: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .weight(0.5f)
            .aspectRatio(0.8f),
        shape = RoundedCornerShape(20),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            RoundIcon(id = icon, backgoundColor = Color.White)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.Black

            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                color = Color.Black

            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(36.dp)
            ) {

                Icon(
                    painter = painterResource(id = R.drawable.next),
                    contentDescription = "Icon Description",
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .size(36.dp),
                )
            }

        }


    }
}

@Composable
fun RoundIcon(id: Int, backgoundColor: Color) {
    Box(
        modifier = Modifier
            .padding(end = 16.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(backgoundColor)
    ) {
        Image(
            painter = painterResource(id),
            contentDescription = "Icon Description",
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
fun HistoryItem(icon: Int, text: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .padding(top = 5.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(15),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(10.dp)) {
            Image(
                painter = painterResource(id = icon),
                contentDescription = "Icon Description",
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(26.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.Black)

            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.Black,
                fontSize = 20.sp,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                modifier = Modifier.weight(1f)
            )
            Image(
                painter = painterResource(id = R.drawable.more),
                contentDescription = "Icon Description",
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(32.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color.Black)

            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ChatBotTheme {
        Greeting("Android")
    }
}