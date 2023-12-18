package org.saltedfish.chatbot

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class chatViewModel : ViewModel() {
//    private var _inputText: MutableLiveData<String> = MutableLiveData<String>()
//    val inputText: LiveData<String> = _inputText
    private var _messageList: MutableLiveData<MutableList<Message>> = MutableLiveData<MutableList<Message>>(
    mutableListOf()
)
    private var _lastId = 0;
    val messageList: LiveData<MutableList<Message>> = _messageList
//    private var _assetUri = MutableLiveData<Uri?>(null)
//    val assetUri = _assetUri
//    fun setInputText(text: String) {
//        _inputText.value = text
//    }
    init {
        JNIBridge.setCallback { value, isStream ->
            val message = Message(
                value,
                false,
                0,
                type = MessageType.TEXT,
                isStreaming = isStream
            )
        }

    }
    fun addMessage(message: Message) {
        message.id = _lastId++
        _messageList.value?.add(message)
        _messageList.value = _messageList.value
    }
    fun initStatus(context: Context,modelType:Int){
        val modelPath = when(modelType){
            0->"model/llama"
            1->"model/fuyu"
            else->"model/llama"
        }
        val vacabPath = when(modelType){
            0->"model/llama_vocab.mllm"
            1->"model/fuyu_uni.mllm"
            else->"model/llama_vocab.mllm"
        }
        val result = JNIBridge.init(context.assets,modelType,modelPath,vacabPath)
        if (result){
            addMessage(Message("模型加载成功",false,0))
        }else{
            addMessage(Message("模型加载失败",false,0))
        }

    }
    fun updateMessage(id:Int,content:String,isStreaming:Boolean=true){
        val index = _messageList.value?.indexOfFirst { it.id == id }?:-1
        var message = _messageList.value?.get(index)

        if (index != -1&&message!=null){
            message.text = content
            message.isStreaming= isStreaming
            _messageList.value?.set(index,message)
            _messageList.value = _messageList.value
        }
    }

//    fun setAssetUri(uri: Uri?) {
//        _assetUri.value = uri
//    }
//    fun sendMessage() {
//        val text = _inputText.value?:""
//        val asset = _assetUri.value
//        val message = Message(
//            text,
//            true,
//            0,
//            type = if (asset == null) MessageType.TEXT else MessageType.IMAGE,
//            content = asset
//
//        )
//        addMessage(message)
//        setInputText("")
//        setAssetUri(null)
//    }



}