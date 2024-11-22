package org.saltedfish.chatbot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
data class Photo(
    var id :Int=0,
    val uri: Uri,
    val request: ImageRequest?
)
val PROMPT = """<|im_start|>system
You are an expert in composing function.<|im_end|>
<|im_start|>user

Here is a list of functions:

%DOC%

Now my query is: %QUERY%
<|im_end|>
<|im_start|>assistant
"""
val MODEL_NAMES = arrayOf("Qwen 2.5","","Bert","PhoneLM", "Qwen 1.5")
class ChatViewModel : ViewModel() {
//    private var _inputText: MutableLiveData<String> = MutableLiveData<String>()
//    val inputText: LiveData<String> = _inputText
    private var _messageList: MutableLiveData<List<Message>> = MutableLiveData<List<Message>>(
    listOf()
)
    private var _photoList: MutableLiveData<List<Photo>> = MutableLiveData<List<Photo>>(
        listOf()
    )
    var functions_:Functions? = null
    var docVecDB:DocumentVecDB? = null
    val photoList = _photoList
    private var _previewUri: MutableLiveData<Uri?> = MutableLiveData<Uri?>(null)
    val previewUri = _previewUri
    var _scrollstate:ScrollState? = null
    private var _lastId = 0;
    val messageList= _messageList
    var _isExternalStorageManager = MutableLiveData<Boolean>(false)
    var _isBusy = MutableLiveData<Boolean>(true)
    val isBusy = _isBusy
    val _isLoading = MutableLiveData<Boolean>(true)
    val isLoading = _isLoading
    private var _modelType = MutableLiveData<Int>(0)
    private var _modelId = MutableLiveData<Int>(0)
    val modelId = _modelId
    val modelType = _modelType
    private var _backendType = -1
    fun setModelType(type:Int){
        _modelType.value = type
    }
    fun setBackendType(type:Int){
        _backendType=type
    }
    fun setModelId(id:Int){
        _modelId.value = id
    }
    fun setPreviewUri(uri: Uri?){
        _previewUri.value = uri
    }
    fun addPhoto(photo: Photo):Int{
        photo.id = _photoList.value?.size?:0
        val list = (_photoList.value?: listOf()).plus(photo)
        _photoList.postValue(list)
        return photo.id
    }

//    private var _assetUri = MutableLiveData<Uri?>(null)
//    val assetUri = _assetUri
//    fun setInputText(text: String) {
//        _inputText.value = text
//    }
    init {

    JNIBridge.setCallback { id,value, isStream ->
//            val message = Message(
//                value,
//                false,
//                0,
//                type = MessageType.TEXT,
//                isStreaming = isStream
//            )
            Log.i("chatViewModel","id:$id,value:$value,isStream:$isStream")
            updateMessage(id,value.trim().replace("|NEWLINE|","\n").replace("▁"," "),isStream)
            if (!isStream){
                _isBusy.postValue(false)
            }
        }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        _isExternalStorageManager.value = Environment.isExternalStorageManager()
        } else {
            TODO("VERSION.SDK_INT < R")
        }


    }
    fun addMessage(message: Message,remote:Boolean=false) {
        if (message.isUser){
                message.id = _lastId++
            }
        val list = (_messageList.value?: listOf()).plus(message)

        if (remote){
            _messageList.postValue(list)
        }
        else{
            _messageList.value = list

        }
    }
    fun sendInstruct(content: Context,message: Message){
        if (message.isUser){
            addMessage(message)
            val bot_message = Message("...",false,0)
            bot_message.id = _lastId++
            addMessage(bot_message)
            _isBusy.value = true
            CoroutineScope(Dispatchers.IO).launch {
                val query = docVecDB?.queryDocument(message.text)
                Log.i("chatViewModel","query:$query")
                val query_docs = query?.map { it.generateAPIDoc() }?.joinToString("==================================================\n")
                val prompt = PROMPT.replace("%QUERY%",message.text).replace("%DOC%",query_docs?:"")
                Log.i("prompt", prompt)
                val len = prompt.length
                Log.i("prompt Len  ","$len")
                JNIBridge.run(bot_message.id,prompt,100,false)
            }
        }
    }
    fun sendMessage(context:Context,message: Message){
        if (modelType.value==4){
            sendInstruct(context,message)
            return
        }
        if (message.isUser){
            addMessage(message)
            val bot_message = Message("...",false,0)
            bot_message.id = _lastId++
            addMessage(bot_message)
            _isBusy.value = true
            if (arrayOf(0,2,3).contains(modelType.value)){
//            if (modelType.value == 0){
                CoroutineScope(Dispatchers.IO).launch {
//                val run_text = "A dialog, where User interacts with AI. AI is helpful, kind, obedient, honest, and knows its own limits.\nUser: ${message.text}"
                    JNIBridge.run(bot_message.id,message.text,100)
                }
            }else if (modelType.value ==1){
                val image_content = if (message.type==MessageType.IMAGE){
                   val uri =  message.content as Uri?
                    val inputStream = uri?.let { context.contentResolver.openInputStream(it) }
                    inputStream?.readBytes()?:byteArrayOf()
                } else {
                    byteArrayOf()
                }

                viewModelScope.launch(Dispatchers.IO)  {
                    JNIBridge.runImage(bot_message.id, image = image_content,message.text,100)
                }

        }}}

    fun initStatus(context: Context,modelType:Int=_modelType.value?:0){
        if (_isExternalStorageManager.value != true) return;
        Log.e("chatViewModel", "initStatus$modelType")
        val model_info = "$modelId:$modelType"
        val model_id = modelId.value
        //modelId: 0->PhoneLM,1->Qwen
        val modelPath = when(modelType){
            3->{
                when(model_id){
                    0->"model/phonelm-1.5b-instruct-q4_0_4_4.mllm"
                    1->"model/qwen-2.5-1.5b-instruct-q4_0_4_4.mllm"
                    2->"model/qwen-1.5-1.8b-chat-q4_0_4_4.mllm"

                    else->"model/phonelm-1.5b-instruct-q4_0_4_4.mllm"
                }
            }
            1->"model/fuyu-8b-q4_k.mllm"
            4->{
                when(model_id){
                    0->"model/phonelm-1.5b-call-q8_0.mllm"
                    1->"model/qwen-2.5-1.5b-call-q4_0_4_4.mllm"
                    2->"model/qwen-1.5-1.8b-call-q4_0_4_4.mllm"
                    else->"qwen-2.5-1.5b-call-q4_0_4_4.mllm"
//                    1->"model/qwen-2.5-1.5b-call-fp32.mllm"
//                    else->"qwen-2.5-1.5b-call-fp32.mllm"
                }
            }
            else -> "model/phonelm-1.5b-instruct-q4_0_4_4.mllm"
        }
        val qnnmodelPath = when(modelType){
            3->{
                when(model_id){
                    0->"model/phonelm-1.5b-instruct-int8.mllm"
                    1->"model/qwen-2.5-1.5b-chat-int8.mllm"
                    2->"model/qwen-1.5-1.8b-chat-int8.mllm"
                    else->"model/phonelm-1.5b-instruct-int8.mllm"
                }
            }
            1->""
            4->{
                when(model_id){
                    0->"model/phonelm-1.5b-call-int8.mllm"
                    1->"model/qwen-2.5-1.5b-call-int8.mllm"
                    2->"model/qwen-1.5-1.8b-call-int8.mllm"
                    else->"qwen-2.5-1.5b-call-int8.mllm"
                }
            }
            else -> "model/phonelm-1.5b-instruct-int8.mllm"
        }

        val vacabPath = when(modelType){
            1->"model/fuyu_vocab.mllm"
            3->{
                when(model_id){
                    0->"model/phonelm_vocab.mllm"
                    1->"model/qwen2.5_vocab.mllm"
                    2->"model/qwen_vocab.mllm"
                    else->""
                }
            }
            4->{
                when(model_id){
                    0->"model/phonelm_vocab.mllm"
                    1->"model/qwen2.5_vocab.mllm"
                    2->"model/qwen_vocab.mllm"
                    else->""
                }
            }
            else -> ""
        }
        val mergePath = when (model_id){
            1->"model/qwen2.5_merges.txt"
            0->"model/phonelm_merges.txt"
            2->"model/qwen_merges.txt"
            else->""
        }
        var downloadsPath = "/sdcard/Download/"
        if (!downloadsPath.endsWith("/")){
            downloadsPath = downloadsPath.plus("/")
        }
        //list files of downloadsPath
        val files = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.listFiles()

        Log.i("chatViewModel", "files:${files?.size}")
        val load_model = when (modelType) {
            1 -> 1
            3, 4 -> {
//                if (model_id == 0) {
//                    3
//                } else {
//                    0
//                }
                when(model_id){
                    0->3
                    1->0
                    2->4
                    else->0
                }


            }else -> 0
        }
        viewModelScope.launch(Dispatchers.IO)  {
            Log.i("chatViewModel", "load_model:$load_model on $_backendType")
            val result = JNIBridge.Init( load_model, downloadsPath,modelPath, qnnmodelPath, vacabPath,mergePath,_backendType)
            if (result){
                addMessage(Message("Model ${MODEL_NAMES[load_model]} Loaded!",false,0),true)
                _isLoading.postValue(false)
                _isBusy.postValue(false)
            }else{
                addMessage(Message("Fail To Load Models! Please Check if models exists at /sdcard/Download/model and restart app.",false,0),true)
            }
        }


    }
    fun updateMessage(id:Int,content:String,isStreaming:Boolean=true){
        val index = _messageList.value?.indexOfFirst { it.id == id }?:-1
        if (index == -1) {
            Log.i("chatViewModel","updateMessage: index == -1")
            return
        }
        val message = _messageList.value?.get(index)?.copy()

        if (message!=null){
            message.text = content
            message.isStreaming= isStreaming
            val list = (_messageList.value?: mutableListOf()).toMutableList()
            // change the item of immutable list
            list[index] = message
            _messageList.postValue(list.toList())
        }
        if (!isStreaming&&modelType.value==4){
            message?.text="Done for you."
           val functions = parseFunctionCall(content)
            functions.forEach {
                functions_?.execute(it)
            }
        }
    }
}
class VQAViewModel:ViewModel(){
    val messages = listOf(
        "What's the message conveyed by screen?",
        "When is the meal reservation?",
        "Summarize The Screenshot."
    )
    lateinit var  bitmap : Bitmap
    private var _selectedMessage: MutableLiveData<Int> = MutableLiveData<Int>(-1)
    val selectedMessage = _selectedMessage
    private var _answerText: MutableLiveData<String?> = MutableLiveData<String?>(null)
    val answerText = _answerText
    var result_:Boolean = false

    fun setSelectedMessage(id:Int){
        _selectedMessage.value = id
        if (result_&&id>-1){
            sendMessage(messages[id])
        }
    }

    init {
        JNIBridge.setCallback { id,value, isStream ->
            Log.i("PhotoViewModel","id:$id,value:$value,isStream:$isStream")
            _answerText.postValue(value.trim().replace("|NEWLINE|","\n").replace("▁"," "))
        }

    }

    fun initStatus(context: Context){
        if (result_||answerText.value!=null) return;
        viewModelScope.launch(Dispatchers.IO) {
            bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.chat_record_demo)
            bitmap = Bitmap.createScaledBitmap(bitmap, 210, 453, true)
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result =JNIBridge.Init(1,"/sdcard/Download/","model/fuyu.mllm","", "model/vocab_uni.mllm")
            result_ = result
            if (result&&selectedMessage.value!=null&& selectedMessage.value!! >-1){
                sendMessage(messages[selectedMessage.value!!])
            }
            else if (!result){
                _answerText.postValue("Fail to Load Models.")
            }
        }
    }
    fun sendMessage(message: String){
//        answerText.postValue(msg)
        viewModelScope.launch(Dispatchers.IO)  {
            JNIBridge.runImage(0, bitmap2Bytes(bitmap),message,100)
        }

    }

}
class SummaryViewModel:ViewModel(){
    private var _message: MutableLiveData<Message> = MutableLiveData<Message>()
    val message = _message
    private var _result: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    val result = _result



    private fun updateMessageText(message:String){
        val msg = _message.value?.copy()?: Message("...",false,0)
        msg.text = message
        _message.postValue(msg)
    }

    init {
        JNIBridge.setCallback { id,value, isStream ->
            Log.i("SummaryViewModel","id:$id,value:$value,isStream:$isStream")
            updateMessageText(value.trim().replace("|NEWLINE|","\n").replace("▁"," "))
        }
//        initStatus()
    }
    fun initStatus(){

        viewModelScope.launch(Dispatchers.IO) {
            val result =JNIBridge.Init(1,"/sdcard/Download/","model/qwen.mllm","", "model/vocab_qwen.mllm")
            _result.postValue(result)
            if (!result){
                updateMessageText("Fail to Load Models.")
            }
        }
    }
    fun sendMessage(message: String){
        val msg = Message("...",false,0,)
        _message.postValue(msg)
        viewModelScope.launch(Dispatchers.IO)  {
            JNIBridge.run(msg.id,message,100)
        }
}}
class PhotoViewModel : ViewModel() {
    private var _message: MutableLiveData<Message> = MutableLiveData<Message>()
    val message = _message
    var _bitmap = MutableLiveData<Bitmap>()
    var result_:Boolean = false

    private fun updateMessageText(message:String){
        val msg = _message.value?.copy()?: Message("...",false,0)
        msg.text = message
        _message.postValue(msg)
    }
    fun setBitmap(bitmap: Bitmap){
//        resize bitmap to 224
        val width = bitmap.width
        val height = bitmap.height
        val newWidth = 224
        val newHeight = 224
        val scaleWidth = newWidth.toFloat() / width
        val scaleHeight = newHeight.toFloat() / height
        val matrix = android.graphics.Matrix()
        matrix.postScale(scaleWidth, scaleHeight)
        val bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        _bitmap.value = bitmap
        Log.e("PhotoViewModel","bitmap:${bitmap.width},${bitmap.height}")

        if (result_&&message.value==null){
            sendMessage("Describe this photo.",bitmap)
        }

    }
    init {
        JNIBridge.setCallback { id,value, isStream ->
            Log.i("PhotoViewModel","id:$id,value:$value,isStream:$isStream")
            updateMessageText(value.trim().replace("|NEWLINE|","\n").replace("▁"," "))
        }
        initStatus()
    }
    fun initStatus(){

        viewModelScope.launch(Dispatchers.IO) {
            val result =JNIBridge.Init(1,"/sdcard/Download/","model/fuyu.mllm","","model/vocab_uni.mllm")
            result_ = result
            if (result&&message.value==null&&_bitmap.value!=null){
                sendMessage("Describe this photo.",_bitmap.value!!)
            }
            else if (!result){
                updateMessageText("Fail to Load Models.")
            }
        }
    }
    fun sendMessage(message: String,bitmap: Bitmap){
        val msg = Message("...",false,0,)
        _message.postValue(msg)
        viewModelScope.launch(Dispatchers.IO)  {
            JNIBridge.runImage(msg.id,bitmap2Bytes(bitmap),message,100)
        }

    }


}
private fun bitmap2Bytes(bitmap: Bitmap): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream)
    return byteArrayOutputStream.toByteArray()
}