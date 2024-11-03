package org.saltedfish.chatbot

import android.content.res.AssetManager
enum class ModelType{
    LLAMA,FUYU,EMBEDDING,EMPTY
}
object JNIBridge {
    var modelType_:ModelType = ModelType.EMPTY
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
    fun Init(modelType:Int,basePath:String,modelPath:String,vacabPath:String,mergePath:String=""):Boolean{
        modelType_ = when(modelType){
            0->ModelType.LLAMA
            1->ModelType.FUYU
            2->ModelType.EMBEDDING
            else->ModelType.EMPTY
        }
//        val mergePath=basePath+mergePath
//        val vacabPath=basePath+vacabPath
//        val modelPath=basePath+modelPath
//        val basePath=""
        return init(modelType,basePath,modelPath,vacabPath,mergePath)
    }
    private external fun init(modelType:Int, basePath:String, modelPath:String, vacabPath:String,mergePath:String):Boolean
    external fun run(id:Int,input:String,maxStep:Int)
    external fun runImage(id:Int,image:ByteArray,text:String,maxStep:Int)
    external fun runForOnce(input: String):FloatArray
    external fun setCallback()
    external fun stop()
    external fun initForInstance(modelType:Int,basePath:String, modelPath:String, vacabPath:String,mergePath:String):Long
    external fun runForInstance(instance:Long,input:String):FloatArray
    external fun releaseInstance(instance:Long)
}
