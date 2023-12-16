package org.saltedfish.chatbot
enum class MessageType{
    TEXT,IMAGE,
}
data class Message(var text:String, val isUser:Boolean, val timeStamp:Int, val type:MessageType=MessageType.TEXT, var content:Any?=null, var isStreaming:Boolean=true, var id:Int=-1,){

}