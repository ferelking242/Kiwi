// Copyright 2024 Kiwi Browser Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license.

#ifndef CHROME_BROWSER_TAB_SESSION_ISOLATION_ANDROID_TAB_SESSION_ISOLATION_BRIDGE_H_
#define CHROME_BROWSER_TAB_SESSION_ISOLATION_ANDROID_TAB_SESSION_ISOLATION_BRIDGE_H_

// JNI bridge declarations for TabSessionIsolation.
// Auto-generated JNI glue is in:
//   out/android_arm64/gen/chrome/android/tab_session_isolation_jni_headers/

#include <jni.h>

#include "base/android/jni_android.h"
#include "base/android/scoped_java_ref.h"

namespace tab_session_isolation {

// Called from Java: creates a new isolated session.
// Returns the isolation_key (UUID string) or an empty string on failure.
base::android::ScopedJavaLocalRef<jstring> CreateIsolationSession(
    JNIEnv* env,
    const base::android::JavaParamRef<jobject>& j_profile);

// Called from Java: destroys an isolated session and its on-disk storage.
void DestroyIsolationSession(
    JNIEnv* env,
    const base::android::JavaParamRef<jobject>& j_profile,
    const base::android::JavaParamRef<jstring>& j_isolation_key);

// Called from Java: returns how many isolated sessions are currently active.
int GetActiveSessionCount(JNIEnv* env);

// Called from Java: returns true if kMaxIsolatedSessions has not been reached.
bool CanCreateIsolatedSession(JNIEnv* env);

}  // namespace tab_session_isolation

#endif  // CHROME_BROWSER_TAB_SESSION_ISOLATION_ANDROID_TAB_SESSION_ISOLATION_BRIDGE_H_
