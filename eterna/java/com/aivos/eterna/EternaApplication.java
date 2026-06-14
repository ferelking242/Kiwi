package com.aivos.eterna;

  import android.app.Application;
  import android.util.Log;
  import com.aivos.eterna.session.SessionRecoveryCoordinator;
  import com.aivos.eterna.session.SessionProtectionManager;
  import com.aivos.eterna.download.EternaDownloadManager;
  import com.aivos.eterna.shizuku.ShizukuIntegration;

  /**
   * Eterna Browser — EternaApplication
   *
   * Application entry point. Initialises all Eterna subsystems at startup:
   *   - Session recovery (restores protected tabs after crash/reboot)
   *   - Session protection manager
   *   - Download manager (recovers interrupted downloads)
   *   - Shizuku (optional, graceful degradation)
   */
  public class EternaApplication extends Application {

      private static final String TAG = "EternaApplication";

      @Override
      public void onCreate() {
          super.onCreate();
          Log.i(TAG, "Eterna Browser starting — Never Sleep. Never Forget.");
          initSubsystems();
      }

      private void initSubsystems() {
          // Session protection must be ready before recovery
          SessionProtectionManager.getInstance(this);

          // Recover any tabs that were protected before a crash/reboot
          SessionRecoveryCoordinator.getInstance(this).recover((url, isIsolated, scrollX, scrollY) -> {
              Log.i(TAG, "Recovering tab: " + url + " isolated=" + isIsolated);
              // Tab recreation is handled by the Chromium tab model via native bridge
              nativeRecoverTab(url, isIsolated, scrollX, scrollY);
          }, count -> Log.i(TAG, "Session recovery complete: " + count + " tab(s) restored."));

          // Download recovery
          EternaDownloadManager.getInstance(this).recoverInterruptedDownloads();

          // Shizuku — optional, never blocks startup
          ShizukuIntegration.getInstance(this).init(new ShizukuIntegration.ShizukuCallback() {
              @Override public void onShizukuReady() {
                  Log.i(TAG, "Shizuku ready.");
              }
              @Override public void onShizukuUnavailable() {
                  Log.d(TAG, "Shizuku unavailable — running without it.");
              }
              @Override public void onBatteryRestrictionDetected(String oem, String suggestion) {
                  Log.w(TAG, "Battery restriction detected on " + oem + ": " + suggestion);
              }
              @Override public void onOemKillerDetected(String killerName) {
                  Log.w(TAG, "OEM killer: " + killerName);
              }
          });
      }

      // Native bridge: requests the Chromium browser engine to reopen a recovered tab
      private static native void nativeRecoverTab(String url, boolean isIsolated, int scrollX, int scrollY);
  }
  