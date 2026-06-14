package com.aivos.eterna.immortal;

  import android.app.Notification;
  import android.app.NotificationChannel;
  import android.app.NotificationManager;
  import android.app.PendingIntent;
  import android.app.Service;
  import android.content.Context;
  import android.content.Intent;
  import android.os.IBinder;
  import android.os.PowerManager;
  import androidx.annotation.NonNull;
  import androidx.annotation.Nullable;
  import androidx.core.app.NotificationCompat;
  import com.aivos.eterna.R;
  import com.aivos.eterna.session.SessionSnapshotManager;
  import java.util.HashSet;
  import java.util.Set;
  import java.util.concurrent.Executors;
  import java.util.concurrent.ScheduledExecutorService;
  import java.util.concurrent.ScheduledFuture;
  import java.util.concurrent.TimeUnit;

  /**
   * Eterna Browser — ImmortalModeService
   *
   * A foreground service that maximises the likelihood of protected tabs
   * surviving Android's background process management.
   *
   * Strategy:
   *  1. Runs as a foreground service (visible notification) — highest OOM priority.
   *  2. Acquires a timed WakeLock only when a tab requests an active operation.
   *  3. Takes periodic session snapshots so tabs can self-heal after any kill.
   *  4. On receiving TASK_REMOVED, triggers an immediate full snapshot.
   *
   * We never claim 100 % uptime — we maximise reliability within Android constraints.
   */
  public class ImmortalModeService extends Service {

      public static final String ACTION_ADD_TAB    = "com.aivos.eterna.immortal.ADD_TAB";
      public static final String ACTION_REMOVE_TAB = "com.aivos.eterna.immortal.REMOVE_TAB";
      public static final String EXTRA_TAB_ID      = "tab_id";

      private static final String CHANNEL_ID      = "eterna_immortal_mode";
      private static final int    NOTIFICATION_ID = 1001;
      private static final long   SNAPSHOT_INTERVAL_SEC = 30L;
      private static final long   WAKELOCK_TIMEOUT_MS   = 10_000L;

      private final Set<Integer> mProtectedTabIds = new HashSet<>();

      private PowerManager.WakeLock mWakeLock;
      private ScheduledExecutorService mScheduler;
      private ScheduledFuture<?> mSnapshotTask;
      private SessionSnapshotManager mSnapshotManager;

      // ── Lifecycle ────────────────────────────────────────────────────────────

      @Override
      public void onCreate() {
          super.onCreate();
          mSnapshotManager = SessionSnapshotManager.getInstance(this);
          mScheduler = Executors.newSingleThreadScheduledExecutor();
          createNotificationChannel();
          startForeground(NOTIFICATION_ID, buildNotification());
          acquireWakeLock();
          scheduleSnapshots();
      }

      @Override
      public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
          if (intent != null) {
              String action = intent.getAction();
              int tabId = intent.getIntExtra(EXTRA_TAB_ID, -1);
              if (ACTION_ADD_TAB.equals(action) && tabId != -1) {
                  mProtectedTabIds.add(tabId);
              } else if (ACTION_REMOVE_TAB.equals(action) && tabId != -1) {
                  mProtectedTabIds.remove(tabId);
                  if (mProtectedTabIds.isEmpty()) stopSelf();
              }
              updateNotification();
          }
          return START_STICKY;
      }

      @Override
      public void onTaskRemoved(Intent rootIntent) {
          // App was swiped away — take an emergency snapshot before we lose state
          mSnapshotManager.snapshotAllProtectedTabsSync(mProtectedTabIds);
          super.onTaskRemoved(rootIntent);
      }

      @Override
      public void onDestroy() {
          if (mSnapshotTask != null) mSnapshotTask.cancel(false);
          if (mScheduler != null)    mScheduler.shutdownNow();
          releaseWakeLock();
          super.onDestroy();
      }

      @Nullable
      @Override
      public IBinder onBind(Intent intent) { return null; }

      // ── Internal ─────────────────────────────────────────────────────────────

      private void scheduleSnapshots() {
          mSnapshotTask = mScheduler.scheduleAtFixedRate(() -> {
              if (!mProtectedTabIds.isEmpty()) {
                  mSnapshotManager.snapshotAllProtectedTabs(mProtectedTabIds);
              }
          }, SNAPSHOT_INTERVAL_SEC, SNAPSHOT_INTERVAL_SEC, TimeUnit.SECONDS);
      }

      private void acquireWakeLock() {
          PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
          if (pm != null) {
              mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "eterna:immortal_mode");
              mWakeLock.setReferenceCounted(false);
              mWakeLock.acquire(WAKELOCK_TIMEOUT_MS);
          }
      }

      private void releaseWakeLock() {
          if (mWakeLock != null && mWakeLock.isHeld()) mWakeLock.release();
      }

      private void createNotificationChannel() {
          NotificationChannel channel = new NotificationChannel(
                  CHANNEL_ID,
                  getString(R.string.immortal_mode_channel_name),
                  NotificationManager.IMPORTANCE_LOW);
          channel.setDescription(getString(R.string.immortal_mode_channel_desc));
          channel.setShowBadge(false);
          NotificationManager nm = getSystemService(NotificationManager.class);
          if (nm != null) nm.createNotificationChannel(channel);
      }

      @NonNull
      private Notification buildNotification() {
          return new NotificationCompat.Builder(this, CHANNEL_ID)
                  .setSmallIcon(R.drawable.ic_eterna_notification)
                  .setContentTitle(getString(R.string.immortal_mode_notification_title))
                  .setContentText(getString(R.string.immortal_mode_notification_text, mProtectedTabIds.size()))
                  .setPriority(NotificationCompat.PRIORITY_LOW)
                  .setOngoing(true)
                  .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                  .build();
      }

      private void updateNotification() {
          NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
          if (nm != null) nm.notify(NOTIFICATION_ID, buildNotification());
      }

      // ── Static helpers ────────────────────────────────────────────────────────

      public static void protectTab(@NonNull Context context, int tabId) {
          Intent intent = new Intent(context, ImmortalModeService.class);
          intent.setAction(ACTION_ADD_TAB);
          intent.putExtra(EXTRA_TAB_ID, tabId);
          context.startForegroundService(intent);
      }

      public static void unprotectTab(@NonNull Context context, int tabId) {
          Intent intent = new Intent(context, ImmortalModeService.class);
          intent.setAction(ACTION_REMOVE_TAB);
          intent.putExtra(EXTRA_TAB_ID, tabId);
          context.startService(intent);
      }
  }
  