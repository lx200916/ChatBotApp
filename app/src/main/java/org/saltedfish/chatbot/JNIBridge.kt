package org.saltedfish.chatbot

enum class ModelType{
    QWEN25,FUYU,EMBEDDING,PhoneLM,QWEN15,EMPTY
}
object JNIBridge {
    var modelType_:ModelType = ModelType.EMPTY
    private var callback: ((Int,String, Boolean,DoubleArray) -> Unit)? = null
    init {
//        Set Environment Variable

                 System.loadLibrary("chatbot")
      }
    fun setCallback(callback: (Int,String,Boolean,DoubleArray) -> Unit) {
        this.callback = callback
        setCallback()
    }
    fun Callback(id:Int,value: String,isStream:Boolean,profile:DoubleArray){


        callback?.let {
            it(id,value,isStream,profile)
        }
    }
    fun Init(modelType:Int,basePath:String,modelPath:String,qnnmodelPath:String,vacabPath:String,mergePath:String="",backend: Int=0):Boolean{
        modelType_ = when(modelType){
            0->ModelType.QWEN25
            1->ModelType.FUYU
            2->ModelType.EMBEDDING
            3->ModelType.PhoneLM
            4->ModelType.QWEN15
            else->ModelType.EMPTY
        }
//        val mergePath=basePath+mergePath
//        val vacabPath=basePath+vacabPath
//        val modelPath=basePath+modelPath
//        val basePath=""
        return init(modelType,basePath,modelPath,qnnmodelPath,vacabPath,mergePath,backend)
    }
    private external fun init(modelType:Int, basePath:String, modelPath:String, qnnmodelPath:String, vacabPath:String,mergePath:String,backend:Int):Boolean
    external fun run(id:Int, input:String, maxStep:Int, applyChatTemplate:Boolean=true)
    external fun runImage(id:Int,image:ByteArray,text:String,maxStep:Int)
    external fun runForOnce(input: String):FloatArray
    external fun setCallback()
    external fun stop()
    external fun initForInstance(modelType:Int,basePath:String, modelPath:String, qnnmodelPath:String, vacabPath:String,mergePath:String):Long
    external fun runForInstance(instance:Long,input:String):FloatArray
    external fun releaseInstance(instance:Long)
}
