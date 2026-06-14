package com.aivos.eterna.session;

  import android.content.BroadcastReceiver;
  import android.content.Context;
  import android.content.Intent;
  import android.util.Log;
  import org.chromium.base.ApplicationStatus;

  /**
   * Eterna Browser — BootReceiver
   *
   * Receives BOOT_COMPLETED and triggers session recovery.
   * The browser will auto-restore all previously protected tabs when the user
   * next opens the app (recovery is pulled lazily from snapshots).
   * We do NOT auto-launch the browser on boot — the user decides when to open it.
   */
  public class BootReceiver extends BroadcastReceiver {

      private static final String TAG = "EternaBootReceiver";

      @Override
      public void onReceive(@NonNull Context context, @NonNull Intent intent) {
          String action = intent.getAction();
          if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                  || "android.intent.action.QUICKBOOT_POWERON".equals(action)
                  || "com.htc.intent.action.QUICKBOOT_POWERON".equals(action)) {
              Log.i(TAG, "Boot detected — session snapshots will be restored on next app launch.");
              // Snapshots are already persisted in SharedPreferences by SessionSnapshotManager.
              // Recovery happens in EternaApplication.onCreate() via SessionRecoveryCoordinator.
          }
      }
  }
  