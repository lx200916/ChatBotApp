package org.saltedfish.chatbot

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
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
                        "chat/{id}?type={type}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType },
                            navArgument("type") { type = NavType.IntType;defaultValue = 0 })
                    ) {
                        Chat(navController, it.arguments?.getInt("type") ?: 0)
                    }
                    composable("photo") {
                        Photo(navController)
                    }
                    // A surface container using the 'background' color from the theme


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

@Composable
fun Home(navController: NavController) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        contentWindowInsets = WindowInsets(16, 30, 16, 0),
        topBar = {
            Greeting("Android")
        },
    ) {
        Column(
            modifier = Modifier.padding(it)

        ) {
            MainEntryCards(navController = navController)
//            Column(Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp)) {
//                Text(
//                    text = "History",
//                    style = MaterialTheme.typography.headlineMedium,
//                    fontWeight = FontWeight.Bold,
//                )
//                Spacer(modifier = Modifier.height(10.dp))
//                HistoryItem(icon = R.drawable.text, text = "The meaning of life is to achieve ....")
//                HistoryItem(icon = R.drawable.image, text = "How many horses in this picture?")
//
//            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Chat(navController: NavController, chatType: Int = 0, vm: ChatViewModel = viewModel()) {
    LaunchedEffect(key1 = chatType) {
        vm.setModelType(chatType)
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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
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
                ChatInput(!isBusy&&!isLoading, withImage = modelType==1, onImageSelected = {
                    vm.setPreviewUri(it)
                }
                ) {
                    //TODO
                    //Get timestamp
                    vm.sendMessage(context, it)

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
                if (!isLoading)ChatBubble(message = Message(if (modelType==0) "Hi! I am a AI ChatBot. How can I assist you today?" else "Hi! I am a AI ChatBot. Feel Free to Talk to me or even send me some pictures!", false, 0))
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
    withImage:Boolean,
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
       if(withImage) IconButton(onClick = {
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
    Column(modifier = modifier.padding(top = 64.dp, start = 20.dp)) {
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
fun MainEntryCards(modifier: Modifier = Modifier, navController: NavController) {
    val context = LocalContext.current
    Column(
        Modifier
            .padding(8.dp)
            .padding(top = 20.dp)
    ) {

        Row {
            EntryCard(icon = R.drawable.text,
                backgroundColor = Color(0xEDADE6AA),
                title = "Text Reader",
                subtitle = "\" The meaning of life is ....\"",
                onClick = { navController.navigate("chat/1?type=0") })
            Spacer(Modifier.width(8.dp))
            EntryCard(icon = R.drawable.image,
                backgroundColor = Purple80,
                title = "Image Reader",
                subtitle = "\" say..How many stars in the sky?\"",
                onClick = { navController.navigate("chat/1?type=1") }

            )

        }
        Spacer(Modifier.height(8.dp))
        Row {
            EntryCard(icon = R.drawable.camera,
                // Pick up a pink
                backgroundColor = Color(0xEDF8BBD0),
                title = "Take A Photo",
                subtitle = "\" Show me the real world\"",
                onClick = { navController.navigate("photo") })
            Spacer(Modifier.width(8.dp))
            EntryCard(icon = R.drawable.more_text,
                // pick a blue style
                backgroundColor = Color(0xEDB3E5FC),
                title = "Show Me More Features!",
                subtitle = "",
                onClick = {
                    // visit Github!
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://github.com/UbiquitousLearning/mllm")
                    context.startActivity(intent)
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
            Spacer(modifier = Modifier.height(16.dp))
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