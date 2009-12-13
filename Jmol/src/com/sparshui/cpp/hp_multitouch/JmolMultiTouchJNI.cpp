// JmolMultiTouchDLL.cpp
//

#include "stdafx.h"
#include <stdio.h>
#include "InstanceMethodCall.h"

BOOL APIENTRY DllMain( HANDLE hModule, 
                       DWORD  ul_reason_for_call, 
                       LPVOID lpReserved
                                         )
{
    return TRUE;
}

JNIEXPORT void JNICALL
Java_HelloWorld_print(JNIEnv *env, jobject obj)
{
printf("Hello World!\n");
return;
}

JNIEXPORT void JNICALL
Java_org_jmol_multitouch_jni_InstanceMethodCall_nativeMethod(JNIEnv *env, jobject obj)
{
//jclass cls = (*env)->GetObjectClass(env, obj);
//jmethodID mid =
//(*env)->GetMethodID(env, cls, "callback", "()V");
//if (mid == NULL) {
//return; /* method not found */
//}
printf("In C\n");
//(*env)->CallVoidMethod(env, obj, mid);
}
