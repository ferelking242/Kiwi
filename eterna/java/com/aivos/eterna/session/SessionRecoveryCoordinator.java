package com.aivos.eterna.session;

  import android.content.Context;
  import androidx.annotation.NonNull;
  import androidx.annotation.MainThread;
  import org.json.JSONArray;
  import org.json.JSONException;
  import org.json.JSONObject;
  import java.util.ArrayList;
  import java.util.List;

  /**
   * Eterna Browser — SessionRecoveryCoordinator
   *
   * Called at app startup to restore previously protected tabs.
   * Reads snapshots from SessionSnapshotManager and requests the browser
   * to recreate each tab, restoring URL, scroll position and isolated state.
   */
  public class SessionRecoveryCoordinator {

      private static SessionRecoveryCoordinator sInstance;

      public interface RecoveryListener {
          void onTabRecovered(@NonNull String url, boolean isIsolated, int scrollX, int scrollY);
          void onRecoveryComplete(int tabsRestored);
      }

      private final SessionSnapshotManager mSnapshotManager;

      private SessionRecoveryCoordinator(@NonNull Context context) {
          mSnapshotManager = SessionSnapshotManager.getInstance(context);
      }

      @NonNull
      public static SessionRecoveryCoordinator getInstance(@NonNull Context context) {
          if (sInstance == null) sInstance = new SessionRecoveryCoordinator(context);
          return sInstance;
      }

      /**
       * Run recovery. Must be called on the main thread after the browser engine initialises.
       */
      @MainThread
      public void recover(@NonNull RecoveryListener listener) {
          JSONArray snapshots = mSnapshotManager.loadAndClearSnapshots();
          List<JSONObject> valid = new ArrayList<>();
          for (int i = 0; i < snapshots.length(); i++) {
              try { valid.add(snapshots.getJSONObject(i)); } catch (JSONException ignored) {}
          }
          for (JSONObject snap : valid) {
              try {
                  String  url        = snap.getString("url");
                  boolean isIsolated = snap.optBoolean("isIsolated", false);
                  int     scrollX    = snap.optInt("scrollX", 0);
                  int     scrollY    = snap.optInt("scrollY", 0);
                  if (url != null && !url.isEmpty()) {
                      listener.onTabRecovered(url, isIsolated, scrollX, scrollY);
                  }
              } catch (JSONException ignored) {}
          }
          listener.onRecoveryComplete(valid.size());
      }
  }
  