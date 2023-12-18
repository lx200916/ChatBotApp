package org.saltedfish.chatbot

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
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
import org.saltedfish.chatbot.ui.theme.ChatBotTheme
import org.saltedfish.chatbot.ui.theme.Purple80

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        //enableEdgeToEdge()
        setContent {
            ChatBotTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { Home(navController) }
                    composable("chat/{id}?type={type}",
                        arguments = listOf(navArgument("id") { type = NavType.IntType },
                            navArgument("type") { type = NavType.StringType;defaultValue = "text" }
                        )) {
                        Chat(navController)
                    }
                    // A surface container using the 'background' color from the theme


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
            modifier = Modifier
                .padding(it)

        ) {
            MainEntryCards(navController = navController)
            Column(Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp)) {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(10.dp))
                HistoryItem(icon = R.drawable.text, text = "The meaning of life is to achieve ....")
                HistoryItem(icon = R.drawable.image, text = "How many horses in this picture?")

            }

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun Chat(navController: NavController,vm:chatViewModel= viewModel()) {
    val messages by vm.messageList.observeAsState(mutableListOf())
    val context = LocalContext.current
    LaunchedEffect(key1 = Unit){
        vm.initStatus(context,0)
    }
    Scaffold(
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(title = {
                Text(
                    text = "Chat",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                }
            })
        }, bottomBar = {
            BottomAppBar() {
                ChatInput() {
                    //TODO
                    //Get timestamp
                    vm.addMessage(it)

                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()

        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(it)
                    .consumeWindowInsets(it)
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                ChatBubble(message = Message("Hello", true, 0))
                ChatBubble(message = Message("Hi,I am A ChatBot.What Can I do for you?", false, 0))
                messages.forEach {
                    ChatBubble(message = it)
                }
//                Spacer(
//                    modifier = Modifier
//                        .weight(1f)
//
//                )
            }


        }
    }

}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChatInput(onMessageSend: (Message) -> Unit = {}) {
    var text by remember { mutableStateOf("") }
    val result = remember { mutableStateOf<Uri?>(null) }
//softkeyborad
    val keyboardController = LocalSoftwareKeyboardController.current

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) {
        result.value = it
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
    ) {
        IconButton(onClick = {
            launcher.launch(
                PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }, Modifier.padding(10.dp)) {
            Image(
                painter = painterResource(id = if (result.value != null) R.drawable.add_done else R.drawable.add_other),
                contentDescription = "Add Other Resources.",
                Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        OutlinedTextField(
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
            keyboardController?.hide();
            onMessageSend(
                Message(
                    text,
                    true,
                    0,
                    type = if (result.value == null) MessageType.TEXT else MessageType.IMAGE,
                    content = result.value
                )
            );text = "";result.value=null;
        }) {
            Icon(
                painter = painterResource(id = R.drawable.up),
                contentDescription = "Send",
                Modifier.size(36.dp)
            )
        }

    }

}
@Composable
fun ColumnScope.ChatBubble(message: Message){
    if (message.text.isNotEmpty()) ChatBubbleBox(isUser = message.isUser) {
        Text(text = message.text, fontSize = 18.sp)
    }
    if (message.type == MessageType.IMAGE) ChatBubbleBox(isUser = message.isUser) {

        (message.content as Uri?)?.let { image->
            val painter = rememberAsyncImagePainter(
                ImageRequest
                    .Builder(LocalContext.current)
                    .data(data = image)
                    .build()
            )
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = ContentScale.None,
                modifier = Modifier
                    .heightIn(min = 60.dp,)
                    .widthIn(min = 60.dp),
            )
        }

    }
}
@Composable
fun ColumnScope.ChatBubbleBox(isUser: Boolean,content: @Composable ()->Unit){
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
            fontWeight = FontWeight.Bold, fontSize = 32.sp,
            lineHeight = 30.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
//            style = MaterialTheme.typography.headlineSmall.copy( lineHeightStyle = LineHeightStyle(alignment = LineHeightStyle.Alignment.Bottom, trim = LineHeightStyle.Trim.Both )),
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
    Column(
        Modifier
            .padding(8.dp)
            .padding(top = 20.dp)
    ) {

        Row {
            EntryCard(
                icon = R.drawable.text,
                backgoundColor = Color(0xEDADE6AA),
                title = "Text Reader",
                subtitle = "\" The meaning of life is ....\"",
                onClick = { navController.navigate("chat/1?type=text") }
            )
            Spacer(Modifier.width(8.dp))
            EntryCard(
                icon = R.drawable.image,
                backgoundColor = Purple80,
                title = "Image Reader",
                subtitle = "\" say..How many stars in the sky?\""
            )

        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RowScope.EntryCard(
    icon: Int,
    backgoundColor: Color,
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
            containerColor = backgoundColor
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
                modifier = Modifier.fillMaxWidth()
            ) {

                Image(
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
            .fillMaxWidth(), shape = RoundedCornerShape(15), colors = CardDefaults.cardColors(
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