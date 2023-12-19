// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("chatbot");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("chatbot")
//      }
//    }
#define ANDROID_API
#include "LibHelper.hpp"
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

#include <jni.h>

using namespace mllm;
using namespace std;
static LibHelper *libHelper;
std::unique_ptr<callback_t> callback=  std::make_unique<callback_t>();
;

//JavaVM *g_jvm = nullptr;
jobject g_jniBridgeObject;
jmethodID g_callbackMethod;

jint JNI_OnLoad(JavaVM *pJvm, void *reserved) {
//    g_jvm = pJvm;
    return JNI_VERSION_1_6;
}

jstring charToJString(JNIEnv *env, string pat) {
    //定义java String类 strClass
    jclass strClass = (env)->FindClass("java/lang/String");
    //获取java String类方法String(byte[],String)的构造器,用于将本地byte[]数组转换为一个新String
    jmethodID ctorID = (env)->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
    //建立byte数组
    jbyteArray bytes = (env)->NewByteArray((jsize) pat.size());
    //将char* 转换为byte数组
    (env)->SetByteArrayRegion(bytes, 0, (jsize) pat.size(), (jbyte *) pat.c_str());
    //设置String, 保存语言类型,用于byte数组转换至String时的参数
    jstring encoding = (env)->NewStringUTF("UTF-8");
    //将byte数组转换为java String,并输出
    return (jstring) (env)->NewObject(strClass, ctorID, bytes, encoding);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_saltedfish_chatbot_JNIBridge_init(JNIEnv *env, jobject thiz,  jint modelType,jstring basePath, jstring modelPath, jstring vacabPath
                                           ) {
    const std::string weights_path_c = string(env->GetStringUTFChars(modelPath, nullptr));
    const auto vacab_path_c = string(env->GetStringUTFChars(vacabPath, nullptr));
    const auto base_path_c = string(env->GetStringUTFChars(basePath, nullptr));
//    auto fpath = (base_path_c+weights_path_c).c_str();
//    // fopen
//    auto fp = fopen(fpath,"rb");
//    ftell(fp);
    if (libHelper!= nullptr) delete libHelper;
    libHelper = new LibHelper();
    if(!libHelper->setUp(base_path_c, weights_path_c, vacab_path_c,
                        static_cast<PreDefinedModel>(modelType))) return JNI_FALSE;
    return JNI_TRUE;
}
extern "C" JNIEXPORT void JNICALL
Java_org_saltedfish_chatbot_JNIBridge_run(JNIEnv *env, jobject thiz,jint id,jstring input, jint maxStep) {
    callback.reset(new callback_t([env,id](std::string str,bool isEnd){
        try {
            jstring jstr = charToJString(env, str);
            env->CallVoidMethod(g_jniBridgeObject, g_callbackMethod,id, jstr, !isEnd);
            env->DeleteLocalRef(jstr);
        }catch (std::exception &e){
            __android_log_print(ANDROID_LOG_ERROR,"MLLM","%s",e.what());
        }

    }));
    libHelper->setCallback(*callback);
    const auto input_c = string(env->GetStringUTFChars(input, nullptr));
    libHelper->run(input_c, maxStep);
}
extern "C" JNIEXPORT void JNICALL
Java_org_saltedfish_chatbot_JNIBridge_setCallback(JNIEnv *env, jobject thiz) {
    // Get a reference to the JNIBridge class
    jclass jniBridgeClass = env->GetObjectClass(thiz);

    // Get a reference to the Callback method
    g_callbackMethod = env->GetMethodID(jniBridgeClass, "Callback", "(ILjava/lang/String;Z)V");

    // Store the JNIBridge object in a global variable
    g_jniBridgeObject = env->NewGlobalRef(thiz);
}
extern "C" JNIEXPORT void JNICALL Java_org_saltedfish_chatbot_JNIBridge_stop(JNIEnv *env, jobject thiz) {
    env->DeleteGlobalRef(g_jniBridgeObject);
}