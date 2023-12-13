package org.saltedfish.chatbot
enum class MessageType{
    TEXT,IMAGE,
}
data class Message(val text:String, val isUser:Boolean,val timeStamp:Int,val type:MessageType=MessageType.TEXT,var content:Any?=null){

}