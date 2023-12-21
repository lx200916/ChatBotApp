package org.saltedfish.chatbot

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class chatViewModel : ViewModel() {
//    private var _inputText: MutableLiveData<String> = MutableLiveData<String>()
//    val inputText: LiveData<String> = _inputText
    private var _messageList: MutableLiveData<List<Message>> = MutableLiveData<List<Message>>(
    listOf()
)
    private var _lastId = 0;
    val messageList= _messageList
    var _isExternalStorageManager = MutableLiveData<Boolean>(false)
    var _isBusy = MutableLiveData<Boolean>(true)
    val isBusy = _isBusy
    private var _modelType = MutableLiveData<Int>(0)
    val modelType = _modelType

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
            updateMessage(id,value,isStream)
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
    fun sendMessage(message: Message){
        if (message.isUser){
            addMessage(message)
            val bot_message = Message("...",false,0)
            bot_message.id = _lastId++
            addMessage(bot_message)
            _isBusy.value = true

            CoroutineScope(Dispatchers.IO).launch {
//                val run_text = "A dialog, where User interacts with AI. AI is helpful, kind, obedient, honest, and knows its own limits.\nUser: ${message.text}"
                JNIBridge.run(bot_message.id,message.text,50)
            }
        }
    }
    fun initStatus(context: Context,modelType:Int=_modelType.value?:0){
        if (_isExternalStorageManager.value != true) return;
        val modelPath = when(modelType){
            0->"model/llama_2.mllm"
            1->"model/fuyu"
            else->"model/llama"
        }
        val vacabPath = when(modelType){
            0->"model/vocab.mllm"
            1->"model/fuyu_uni.mllm"
            else->"model/llama_vocab.mllm"
        }
        var downloadsPath = "/sdcard/Download/"
        if (!downloadsPath.endsWith("/")){
            downloadsPath = downloadsPath.plus("/")
        }
        //list files of downloadsPath
        val files = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.listFiles()
        Log.i("chatViewModel","files:${files?.size}")
        CoroutineScope(Dispatchers.IO).launch {
            val result = JNIBridge.init( modelType, downloadsPath,modelPath, vacabPath)
            if (result){
                addMessage(Message("模型加载成功",false,0),true)
                _isBusy.postValue(false)
            }else{
                addMessage(Message("模型加载失败",false,0),true)
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