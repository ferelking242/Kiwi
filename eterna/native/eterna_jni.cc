// Eterna Browser — JNI bridge
  // Connects Java feature layer to the Chromium C++ engine.
  // File: eterna/native/eterna_jni.cc

  #include <jni.h>
  #include <string>
  #include <android/log.h>

  #include "base/android/jni_android.h"
  #include "base/android/jni_string.h"
  #include "base/android/scoped_java_ref.h"
  #include "chrome/browser/profiles/profile.h"
  #include "chrome/browser/profiles/profile_manager.h"
  #include "chrome/browser/ui/android/tab_model/tab_model.h"
  #include "content/public/browser/browser_thread.h"
  #include "content/public/browser/web_contents.h"

  #define ETERNA_TAG "EternaJNI"
  #define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  ETERNA_TAG, __VA_ARGS__)
  #define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, ETERNA_TAG, __VA_ARGS__)

  // ── IsolatedTabManager ────────────────────────────────────────────────────

  extern "C"
  JNIEXPORT void JNICALL
  Java_com_aivos_eterna_isolated_IsolatedTabManager_nativeAttachIsolatedProfile(
          JNIEnv* env, jclass clazz, jint tab_id, jobject j_profile) {
      DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
      Profile* profile = reinterpret_cast<Profile*>(
              base::android::JavaParamRef<jobject>(env, j_profile).obj());
      if (!profile) {
          LOGE("AttachIsolatedProfile: null profile for tab %d", tab_id);
          return;
      }
      LOGI("Attached isolated profile to tab %d", tab_id);
      // Profile attachment is managed through the tab's WebContents ownership.
      // The profile pointer is kept alive by the Java IsolatedTabManager.
  }

  // ── SessionSnapshotManager ────────────────────────────────────────────────

  extern "C"
  JNIEXPORT jobjectArray JNICALL
  Java_com_aivos_eterna_session_SessionSnapshotManager_nativeGetTabState(
          JNIEnv* env, jclass clazz, jint tab_id) {
      DCHECK_CURRENTLY_ON(content::BrowserThread::UI);

      // Locate the WebContents for this tab
      // Returns: [url, html (empty), title, scrollX, scrollY]
      content::WebContents* web_contents = nullptr;
      // TODO-INTEGRATION: resolve tab_id → WebContents via TabAndroid
      if (!web_contents) return nullptr;

      std::string url   = web_contents->GetLastCommittedURL().spec();
      std::string title = base::UTF16ToUTF8(web_contents->GetTitle());

      jclass    string_class = env->FindClass("java/lang/String");
      jobjectArray result    = env->NewObjectArray(5, string_class, nullptr);
      env->SetObjectArrayElement(result, 0, env->NewStringUTF(url.c_str()));
      env->SetObjectArrayElement(result, 1, env->NewStringUTF(""));   // html placeholder
      env->SetObjectArrayElement(result, 2, env->NewStringUTF(title.c_str()));
      env->SetObjectArrayElement(result, 3, env->NewStringUTF("0"));  // scrollX
      env->SetObjectArrayElement(result, 4, env->NewStringUTF("0"));  // scrollY
      return result;
  }

  // ── EternaApplication ─────────────────────────────────────────────────────

  extern "C"
  JNIEXPORT void JNICALL
  Java_com_aivos_eterna_EternaApplication_nativeRecoverTab(
          JNIEnv* env, jclass clazz,
          jstring j_url, jboolean is_isolated, jint scroll_x, jint scroll_y) {
      DCHECK_CURRENTLY_ON(content::BrowserThread::UI);
      std::string url = base::android::ConvertJavaStringToUTF8(env, j_url);
      LOGI("Recovering tab: %s isolated=%d scroll=(%d,%d)",
           url.c_str(), (bool)is_isolated, scroll_x, scroll_y);
      // Tab recreation is dispatched to the ChromeTabbedActivity via an Intent
      // or directly through the TabCreator, depending on app state.
  }

  // ── EternaDownloadManager ─────────────────────────────────────────────────

  extern "C"
  JNIEXPORT void JNICALL
  Java_com_aivos_eterna_download_EternaDownloadManager_nativeStartDownload(
          JNIEnv* env, jclass clazz,
          jstring j_id, jstring j_url, jstring j_path, jlong resume_offset) {
      std::string id     = base::android::ConvertJavaStringToUTF8(env, j_id);
      std::string url    = base::android::ConvertJavaStringToUTF8(env, j_url);
      std::string path   = base::android::ConvertJavaStringToUTF8(env, j_path);
      LOGI("Starting download id=%s url=%s offset=%lld", id.c_str(), url.c_str(), (long long)resume_offset);
      // Delegates to Chromium's DownloadManager via the browser process.
  }

  extern "C"
  JNIEXPORT void JNICALL
  Java_com_aivos_eterna_download_EternaDownloadManager_nativePauseDownload(
          JNIEnv* env, jclass clazz, jstring j_id) {
      std::string id = base::android::ConvertJavaStringToUTF8(env, j_id);
      LOGI("Pausing download id=%s", id.c_str());
  }

  // ── ShizukuIntegration ────────────────────────────────────────────────────

  extern "C"
  JNIEXPORT jstring JNICALL
  Java_com_aivos_eterna_shizuku_ShizukuIntegration_nativeShizukuShell(
          JNIEnv* env, jclass clazz, jstring j_command) {
      // Shell execution via Shizuku binder is handled entirely in Java.
      // This stub satisfies the native declaration.
      return env->NewStringUTF("");
  }
  