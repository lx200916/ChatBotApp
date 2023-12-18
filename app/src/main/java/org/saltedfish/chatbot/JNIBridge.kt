package org.saltedfish.chatbot

import android.content.res.AssetManager

object JNIBridge {
    private var callback: ((String, Boolean) -> Unit)? = null
    init {
                 System.loadLibrary("chatbot")
      }
    fun setCallback(callback: (String,Boolean) -> Unit) {
        this.callback = callback
    }
    fun Callback(value: String,isStream:Boolean) {
//        callback?.invoke(value,isStream)

        callback?.let {
            it(value,isStream)
        }
    }
    external fun init(assetManager: AssetManager,modelType:Int,modelPath:String,vacabPath:String):Boolean
    external fun run(input:String,maxStep:Int)
    external fun setCallback()
    external fun stop()
}