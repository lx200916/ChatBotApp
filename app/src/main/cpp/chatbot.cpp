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

JavaVM *g_jvm = nullptr;
jobject g_jniBridgeObject;
jmethodID g_callbackMethod;

jint JNI_OnLoad(JavaVM *pJvm, void *reserved) {
    g_jvm = pJvm;
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_org_saltedfish_chatbot_JNIBridge_init(JNIEnv *env, jobject thiz, jobject assetManager, jint modelType, jstring modelPath, jstring vacabPath) {
    const char *weights_path_c = env->GetStringUTFChars(modelPath, nullptr);
    const char *vacab_path_c = env->GetStringUTFChars(vacabPath, nullptr);
    AAssetManager *mgr = AAssetManager_fromJava(env, assetManager);
    if (mgr== nullptr) return JNI_FALSE;
    if (libHelper!= nullptr) delete libHelper;
    libHelper = new LibHelper(mgr, weights_path_c, vacab_path_c);
//    env->ReleaseStringUTFChars(weights_path, weights_path_c);
//    env->ReleaseStringUTFChars(vacab_path, vacab_path_c);
libHelper->setUp(static_cast<PreDefinedModel>(modelType));
    return JNI_TRUE;
}
extern "C" JNIEXPORT void JNICALL
Java_org_saltedfish_chatbot_JNIBridge_run(JNIEnv *env, jobject thiz, jstring input, jint maxStep) {
    callback.reset(new callback_t([env](std::string str,bool isEnd){
        jstring jstr = env->NewStringUTF(str.c_str());
        env->CallVoidMethod(g_jniBridgeObject, g_callbackMethod, jstr, isEnd);
        env->DeleteLocalRef(jstr);
    }));
    libHelper->setCallback(*callback);
}
extern "C" JNIEXPORT void JNICALL
Java_org_saltedfish_chatbot_JNIBridge_setCallback(JNIEnv *env, jobject thiz) {
    // Get a reference to the JNIBridge class
    jclass jniBridgeClass = env->GetObjectClass(thiz);

    // Get a reference to the Callback method
    g_callbackMethod = env->GetMethodID(jniBridgeClass, "Callback", "(Ljava/lang/String;Z)V");

    // Store the JNIBridge object in a global variable
    g_jniBridgeObject = env->NewGlobalRef(thiz);
}
extern "C" JNIEXPORT void JNICALL Java_org_saltedfish_chatbot_JNIBridge_stop(JNIEnv *env, jobject thiz) {
    env->DeleteGlobalRef(g_jniBridgeObject);
}