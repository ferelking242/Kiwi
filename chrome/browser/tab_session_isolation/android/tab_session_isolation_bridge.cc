// Copyright 2024 Kiwi Browser Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license.

#include "chrome/browser/tab_session_isolation/android/tab_session_isolation_bridge.h"

#include "base/android/jni_android.h"
#include "base/android/jni_string.h"
#include "base/android/scoped_java_ref.h"
#include "chrome/android/chrome_jni_headers/TabSessionIsolationBridge_jni.h"
#include "chrome/browser/profiles/profile.h"
#include "chrome/browser/profiles/profile_android.h"
#include "chrome/browser/tab_session_isolation/tab_session_isolation_manager.h"
#include "content/public/browser/browser_context.h"

using base::android::ConvertJavaStringToUTF8;
using base::android::ConvertUTF8ToJavaString;
using base::android::JavaParamRef;
using base::android::ScopedJavaLocalRef;

namespace tab_session_isolation {

// ---------------------------------------------------------------------------
// JNI exported functions – called from Java via generated JNI glue.
// Each function is named JNI_<ClassName>_<MethodName> per Chromium convention.
// ---------------------------------------------------------------------------

// Creates a new isolated session.
// Returns the isolation_key (UUID) or an empty string when the max is reached.
static ScopedJavaLocalRef<jstring>
JNI_TabSessionIsolationBridge_CreateIsolationSession(
    JNIEnv* env,
    const JavaParamRef<jobject>& j_profile) {
  Profile* profile = ProfileAndroid::FromProfileAndroid(env, j_profile);
  if (!profile) {
    return ConvertUTF8ToJavaString(env, "");
  }

  TabSessionIsolationManager* manager =
      TabSessionIsolationManager::GetInstance();

  std::string isolation_key =
      manager->CreateIsolationSession(profile->GetOriginalProfile());

  return ConvertUTF8ToJavaString(env, isolation_key);
}

// Destroys an isolated session and schedules deletion of its on-disk storage.
static void JNI_TabSessionIsolationBridge_DestroyIsolationSession(
    JNIEnv* env,
    const JavaParamRef<jobject>& j_profile,
    const JavaParamRef<jstring>& j_isolation_key) {
  Profile* profile = ProfileAndroid::FromProfileAndroid(env, j_profile);
  if (!profile) {
    return;
  }

  std::string isolation_key = ConvertJavaStringToUTF8(env, j_isolation_key);
  if (isolation_key.empty()) {
    return;
  }

  TabSessionIsolationManager::GetInstance()->DestroyIsolationSession(
      profile->GetOriginalProfile(), isolation_key);
}

// Returns the number of currently active isolated sessions.
static jint JNI_TabSessionIsolationBridge_GetActiveSessionCount(JNIEnv* env) {
  return TabSessionIsolationManager::GetInstance()->GetActiveSessionCount();
}

// Returns true if a new isolated session can be created.
static jboolean JNI_TabSessionIsolationBridge_CanCreateIsolatedSession(
    JNIEnv* env) {
  return TabSessionIsolationManager::GetInstance()->CanCreateIsolatedSession()
             ? JNI_TRUE
             : JNI_FALSE;
}

}  // namespace tab_session_isolation
