// IInputInjectorService.aidl
package org.saltedfish.chatbot;

// Declare any non-default types here with import statements

interface IInputInjectorService {
   // 必须定义的destroy方法，用于在服务需要被销毁时调用
       void destroy() = 16777114;

       // 注入点击事件
       void injectTap(float x, float y) =1;

       void injectLongPress(float x, float y, int duration) = 2;
}