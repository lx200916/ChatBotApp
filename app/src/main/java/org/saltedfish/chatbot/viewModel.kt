package org.saltedfish.chatbot

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.graphics.painter.Painter
import androidx.lifecycle.LiveData
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
class chatViewModel : ViewModel() {
//    private var _inputText: MutableLiveData<String> = MutableLiveData<String>()
//    val inputText: LiveData<String> = _inputText
    private var _messageList: MutableLiveData<List<Message>> = MutableLiveData<List<Message>>(
    listOf()
)
    private var _photoList: MutableLiveData<List<Photo>> = MutableLiveData<List<Photo>>(
        listOf()
    )
    val photoList = _photoList
    private var _previewUri: MutableLiveData<Uri?> = MutableLiveData<Uri?>(null)
    val previewUri = _previewUri
    var _scrollstate:ScrollState? = null
    private var _lastId = 0;
    val messageList= _messageList
    var _isExternalStorageManager = MutableLiveData<Boolean>(false)
    var _isBusy = MutableLiveData<Boolean>(true)
    val isBusy = _isBusy
    private var _modelType = MutableLiveData<Int>(0)
    val modelType = _modelType
    fun setModelType(type:Int){
        _modelType.value = type
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
    fun sendMessage(context:Context,message: Message){
        if (message.isUser){
            addMessage(message)
            val bot_message = Message("...",false,0)
            bot_message.id = _lastId++
            addMessage(bot_message)
            _isBusy.value = true
            if (modelType.value ==0){
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
        val modelPath = when(modelType){
            0->"model/llama_2.mllm"
            1->"model/fuyu.mllm"
            else->"model/llama"
        }
        val vacabPath = when(modelType){
            0->"model/vocab.mllm"
            1->"model/vocab_uni.mllm"
            else->"model/vocab.mllm"
        }
        var downloadsPath = "/sdcard/Download/"
        if (!downloadsPath.endsWith("/")){
            downloadsPath = downloadsPath.plus("/")
        }
        //list files of downloadsPath
        val files = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.listFiles()
        Log.i("chatViewModel","files:${files?.size}")
        viewModelScope.launch(Dispatchers.IO)  {
            val result = JNIBridge.init( modelType, downloadsPath,modelPath, vacabPath)
            if (result){
                addMessage(Message("Model Loaded!",false,0),true)
                _isBusy.postValue(false)
            }else{
                addMessage(Message("Fail To Load Models.",false,0),true)
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
    }
}

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
        _bitmap.value = bitmap
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
            val result =JNIBridge.init(1,"/sdcard/Download/","model/fuyu.mllm","model/vocab_uni.mllm")
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

    private fun bitmap2Bytes(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }
}