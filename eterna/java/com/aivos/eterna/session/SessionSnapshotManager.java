package com.aivos.eterna.session;

  import android.content.Context;
  import android.content.SharedPreferences;
  import androidx.annotation.NonNull;
  import androidx.annotation.WorkerThread;
  import com.aivos.eterna.isolated.IsolatedTabManager;
  import org.json.JSONArray;
  import org.json.JSONException;
  import org.json.JSONObject;
  import java.util.Set;
  import java.util.concurrent.ExecutorService;
  import java.util.concurrent.Executors;

  /**
   * Eterna Browser — SessionSnapshotManager
   *
   * Persists session state for protected tabs so they can be recovered after
   * crash, reboot, or force-close.
   *
   * Snapshot format (JSON per tab):
   * {
   *   "tabId":      <int>,
   *   "url":        <string>,
   *   "title":      <string>,
   *   "scrollX":    <int>,
   *   "scrollY":    <int>,
   *   "formData":   <object>,
   *   "isIsolated": <bool>,
   *   "timestamp":  <long>
   * }
   */
  public class SessionSnapshotManager {

      private static final String PREFS_NAME    = "eterna_session_snapshots";
      private static final String KEY_SNAPSHOTS = "snapshots";

      private static SessionSnapshotManager sInstance;

      private final SharedPreferences mPrefs;
      private final ExecutorService   mExecutor;
      private final IsolatedTabManager mIsolatedTabManager;

      private SessionSnapshotManager(@NonNull Context context) {
          mPrefs = context.getApplicationContext()
                          .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
          mExecutor = Executors.newSingleThreadExecutor();
          mIsolatedTabManager = IsolatedTabManager.getInstance(context);
      }

      @NonNull
      public static SessionSnapshotManager getInstance(@NonNull Context context) {
          if (sInstance == null) sInstance = new SessionSnapshotManager(context);
          return sInstance;
      }

      /** Asynchronous snapshot — safe to call from any thread. */
      public void snapshotAllProtectedTabs(@NonNull Set<Integer> tabIds) {
          mExecutor.execute(() -> snapshotAllProtectedTabsSync(tabIds));
      }

      /** Synchronous snapshot — call only from a background thread. */
      @WorkerThread
      public void snapshotAllProtectedTabsSync(@NonNull Set<Integer> tabIds) {
          JSONArray snapshots = loadSnapshots();
          for (int tabId : tabIds) {
              JSONObject snap = buildSnapshot(tabId);
              if (snap != null) upsertSnapshot(snapshots, snap);
          }
          persistSnapshots(snapshots);
      }

      /** Restore all previously snapshotted tabs (called at app start / after reboot). */
      @NonNull
      public JSONArray loadAndClearSnapshots() {
          JSONArray result = loadSnapshots();
          mPrefs.edit().remove(KEY_SNAPSHOTS).apply();
          return result;
      }

      /** Delete snapshot for a specific tab (called when user explicitly closes it). */
      public void clearSnapshot(int tabId) {
          mExecutor.execute(() -> {
              JSONArray snaps = loadSnapshots();
              JSONArray updated = new JSONArray();
              for (int i = 0; i < snaps.length(); i++) {
                  try {
                      JSONObject o = snaps.getJSONObject(i);
                      if (o.getInt("tabId") != tabId) updated.put(o);
                  } catch (JSONException ignored) {}
              }
              persistSnapshots(updated);
          });
      }

      // ── Private helpers ──────────────────────────────────────────────────────

      @NonNull
      private JSONArray loadSnapshots() {
          String raw = mPrefs.getString(KEY_SNAPSHOTS, "[]");
          try { return new JSONArray(raw); } catch (JSONException e) { return new JSONArray(); }
      }

      private void persistSnapshots(@NonNull JSONArray snapshots) {
          mPrefs.edit().putString(KEY_SNAPSHOTS, snapshots.toString()).apply();
      }

      private JSONObject buildSnapshot(int tabId) {
          // Native bridge fetches live tab state: URL, scroll, form data
          String[] state = nativeGetTabState(tabId);
          if (state == null || state.length < 2) return null;
          try {
              JSONObject snap = new JSONObject();
              snap.put("tabId",      tabId);
              snap.put("url",        state[0]);
              snap.put("title",      state.length > 2 ? state[2] : "");
              snap.put("scrollX",    state.length > 3 ? Integer.parseInt(state[3]) : 0);
              snap.put("scrollY",    state.length > 4 ? Integer.parseInt(state[4]) : 0);
              snap.put("isIsolated", mIsolatedTabManager.isIsolatedTab(tabId));
              snap.put("timestamp",  System.currentTimeMillis());
              return snap;
          } catch (JSONException e) { return null; }
      }

      private void upsertSnapshot(@NonNull JSONArray arr, @NonNull JSONObject snap) {
          try {
              int tabId = snap.getInt("tabId");
              for (int i = 0; i < arr.length(); i++) {
                  if (arr.getJSONObject(i).getInt("tabId") == tabId) {
                      arr.put(i, snap);
                      return;
                  }
              }
              arr.put(snap);
          } catch (JSONException ignored) {}
      }

      // Returns [url, rawHtml, title, scrollX, scrollY] or null
      private static native String[] nativeGetTabState(int tabId);
  }
  