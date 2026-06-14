package com.aivos.eterna.shizuku;

  import android.content.Context;
  import android.content.pm.PackageManager;
  import android.util.Log;
  import androidx.annotation.NonNull;
  import rikka.shizuku.Shizuku;

  /**
   * Eterna Browser — ShizukuIntegration
   *
   * Optional Shizuku integration for detecting OEM battery restrictions
   * and background process killers.
   *
   * The browser works perfectly without Shizuku — this module gracefully
   * degrades when Shizuku is unavailable.
   */
  public class ShizukuIntegration {

      private static final String TAG           = "EternaShizuku";
      private static final String SHIZUKU_PKG   = "moe.shizuku.privileged.api";
      private static final int    REQUEST_CODE  = 42;

      public interface ShizukuCallback {
          void onBatteryRestrictionDetected(@NonNull String oem, @NonNull String suggestion);
          void onOemKillerDetected(@NonNull String killerName);
          void onShizukuReady();
          void onShizukuUnavailable();
      }

      private static ShizukuIntegration sInstance;
      private final Context mContext;
      private boolean mShizukuAvailable = false;

      private ShizukuIntegration(@NonNull Context context) {
          mContext = context.getApplicationContext();
      }

      @NonNull
      public static ShizukuIntegration getInstance(@NonNull Context context) {
          if (sInstance == null) sInstance = new ShizukuIntegration(context);
          return sInstance;
      }

      /**
       * Initialise Shizuku. Calls callback immediately based on availability.
       * Safe to call even if Shizuku is not installed — will call onShizukuUnavailable().
       */
      public void init(@NonNull ShizukuCallback callback) {
          if (!isShizukuInstalled()) {
              Log.i(TAG, "Shizuku not installed — running without it.");
              callback.onShizukuUnavailable();
              return;
          }
          try {
              if (Shizuku.pingBinder()) {
                  mShizukuAvailable = true;
                  Log.i(TAG, "Shizuku binder available.");
                  if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                      callback.onShizukuReady();
                      runChecks(callback);
                  } else {
                      Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
                          if (requestCode == REQUEST_CODE
                                  && grantResult == PackageManager.PERMISSION_GRANTED) {
                              callback.onShizukuReady();
                              runChecks(callback);
                          } else {
                              callback.onShizukuUnavailable();
                          }
                      });
                      Shizuku.requestPermission(REQUEST_CODE);
                  }
              } else {
                  callback.onShizukuUnavailable();
              }
          } catch (Exception e) {
              Log.e(TAG, "Shizuku init error — continuing without it", e);
              callback.onShizukuUnavailable();
          }
      }

      public boolean isAvailable() { return mShizukuAvailable; }

      // ── Private checks (Shizuku-powered) ────────────────────────────────────

      private void runChecks(@NonNull ShizukuCallback callback) {
          detectBatteryRestrictions(callback);
          detectOemKillers(callback);
      }

      private void detectBatteryRestrictions(@NonNull ShizukuCallback callback) {
          try {
              // Read dumpsys deviceidle via Shizuku shell
              String result = nativeShizukuShell("dumpsys deviceidle | grep -i whitelist");
              String pkg = mContext.getPackageName();
              if (!result.contains(pkg)) {
                  callback.onBatteryRestrictionDetected(
                          getOemName(),
                          "Add Eterna to the battery whitelist in System Settings → Battery → Unrestricted.");
              }
          } catch (Exception e) {
              Log.w(TAG, "Battery restriction check failed", e);
          }
      }

      private void detectOemKillers(@NonNull ShizukuCallback callback) {
          try {
              String oem = getOemName().toLowerCase();
              if (oem.contains("xiaomi") || oem.contains("miui")) {
                  callback.onOemKillerDetected("MIUI Memory Manager — enable Autostart for Eterna in Security app.");
              } else if (oem.contains("huawei") || oem.contains("emui")) {
                  callback.onOemKillerDetected("EMUI PowerGenie — add Eterna to Protected Apps.");
              } else if (oem.contains("oppo") || oem.contains("oneplus")) {
                  callback.onOemKillerDetected("ColorOS/OxygenOS — enable Auto Launch for Eterna.");
              } else if (oem.contains("samsung")) {
                  callback.onOemKillerDetected("Samsung Device Care — disable Adaptive Battery for Eterna.");
              }
          } catch (Exception e) {
              Log.w(TAG, "OEM killer detection failed", e);
          }
      }

      @NonNull
      private String getOemName() {
          return android.os.Build.MANUFACTURER + " " + android.os.Build.BRAND;
      }

      private boolean isShizukuInstalled() {
          try {
              mContext.getPackageManager().getPackageInfo(SHIZUKU_PKG, 0);
              return true;
          } catch (PackageManager.NameNotFoundException e) {
              return false;
          }
      }

      // Returns shell output via Shizuku
      private static native String nativeShizukuShell(String command);
  }
  