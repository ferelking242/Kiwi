package com.aivos.eterna.download;

  import android.content.BroadcastReceiver;
  import android.content.Context;
  import android.content.Intent;
  import android.util.Log;

  /**
   * Eterna Browser — DownloadBootReceiver
   *
   * On device boot, reschedules any incomplete downloads that were
   * interrupted by shutdown so they resume automatically.
   */
  public class DownloadBootReceiver extends BroadcastReceiver {

      private static final String TAG = "EternaDownloadBoot";

      @Override
      public void onReceive(Context context, Intent intent) {
          if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
              Log.i(TAG, "Boot completed — queuing interrupted download recovery.");
              EternaDownloadManager.getInstance(context).recoverInterruptedDownloads();
          }
      }
  }
  