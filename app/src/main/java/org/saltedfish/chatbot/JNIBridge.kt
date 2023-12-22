package org.saltedfish.chatbot

import android.content.res.AssetManager

object JNIBridge {
    private var callback: ((Int,String, Boolean) -> Unit)? = null
    init {
                 System.loadLibrary("chatbot")
      }
    fun setCallback(callback: (Int,String,Boolean) -> Unit) {
        this.callback = callback
        setCallback()
    }
    fun Callback(id:Int,value: String,isStream:Boolean) {
//        callback?.invoke(value,isStream)

        callback?.let {
            it(id,value,isStream)
        }
    }
    external fun init(modelType:Int,basePath:String,modelPath:String,vacabPath:String):Boolean
    external fun run(id:Int,input:String,maxStep:Int)
    external fun runImage(id:Int,image:ByteArray,text:String,maxStep:Int)
    external fun setCallback()
    external fun stop()
}