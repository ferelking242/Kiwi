package com.aivos.eterna.session;

  import android.content.Context;
  import androidx.annotation.NonNull;
  import com.aivos.eterna.immortal.ImmortalModeService;
  import java.util.Collections;
  import java.util.HashSet;
  import java.util.Set;

  /**
   * Eterna Browser — SessionProtectionManager
   *
   * High-level coordinator for session protection:
   *   - Tracks which tabs are "protected"
   *   - Delegates snapshot persistence to SessionSnapshotManager
   *   - Delegates keep-alive to ImmortalModeService
   *   - Triggers automatic recovery at startup
   */
  public class SessionProtectionManager {

      private static SessionProtectionManager sInstance;

      private final Context               mContext;
      private final SessionSnapshotManager mSnapshotManager;
      private final Set<Integer>           mProtectedTabIds = new HashSet<>();

      private SessionProtectionManager(@NonNull Context context) {
          mContext = context.getApplicationContext();
          mSnapshotManager = SessionSnapshotManager.getInstance(context);
      }

      @NonNull
      public static SessionProtectionManager getInstance(@NonNull Context context) {
          if (sInstance == null) sInstance = new SessionProtectionManager(context);
          return sInstance;
      }

      /** Mark a tab as protected. This persists its session and keeps it alive. */
      public void protectTab(int tabId) {
          if (mProtectedTabIds.add(tabId)) {
              ImmortalModeService.protectTab(mContext, tabId);
          }
      }

      /** Remove session protection from a tab. */
      public void unprotectTab(int tabId) {
          if (mProtectedTabIds.remove(tabId)) {
              ImmortalModeService.unprotectTab(mContext, tabId);
              mSnapshotManager.clearSnapshot(tabId);
          }
      }

      public boolean isProtected(int tabId) {
          return mProtectedTabIds.contains(tabId);
      }

      @NonNull
      public Set<Integer> getProtectedTabIds() {
          return Collections.unmodifiableSet(mProtectedTabIds);
      }

      /** Take an immediate snapshot of all protected tabs (e.g., on app pause). */
      public void snapshotNow() {
          if (!mProtectedTabIds.isEmpty()) {
              mSnapshotManager.snapshotAllProtectedTabs(mProtectedTabIds);
          }
      }
  }
  