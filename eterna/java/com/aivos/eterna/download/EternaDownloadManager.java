package com.aivos.eterna.download;

  import android.content.Context;
  import android.util.Log;
  import androidx.annotation.NonNull;
  import androidx.annotation.Nullable;
  import org.json.JSONArray;
  import org.json.JSONException;
  import org.json.JSONObject;
  import android.content.SharedPreferences;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.UUID;
  import java.util.concurrent.ExecutorService;
  import java.util.concurrent.Executors;

  /**
   * Eterna Browser — EternaDownloadManager
   *
   * Crash-safe download manager with:
   *   - Pause / Resume / Retry
   *   - Parallel downloads (configurable concurrency)
   *   - Queue ordering (FIFO)
   *   - Persistent state — survives crash and reboot
   *   - Automatic recovery on BootReceiver signal
   */
  public class EternaDownloadManager {

      private static final String TAG         = "EternaDownloadManager";
      private static final String PREFS_NAME  = "eterna_downloads";
      private static final String KEY_QUEUE   = "download_queue";
      private static final int    MAX_PARALLEL = 3;

      public enum DownloadState { QUEUED, IN_PROGRESS, PAUSED, FAILED, COMPLETE }

      public static class DownloadItem {
          public final String id;
          public String url;
          public String fileName;
          public String filePath;
          public long   totalBytes;
          public long   downloadedBytes;
          public DownloadState state;
          public long   timestamp;

          public DownloadItem(@NonNull String url, @NonNull String fileName) {
              this.id        = UUID.randomUUID().toString();
              this.url       = url;
              this.fileName  = fileName;
              this.state     = DownloadState.QUEUED;
              this.timestamp = System.currentTimeMillis();
          }

          @NonNull
          public JSONObject toJson() throws JSONException {
              JSONObject o = new JSONObject();
              o.put("id",               id);
              o.put("url",              url);
              o.put("fileName",         fileName);
              o.put("filePath",         filePath != null ? filePath : "");
              o.put("totalBytes",       totalBytes);
              o.put("downloadedBytes",  downloadedBytes);
              o.put("state",            state.name());
              o.put("timestamp",        timestamp);
              return o;
          }

          @NonNull
          public static DownloadItem fromJson(@NonNull JSONObject o) throws JSONException {
              DownloadItem item = new DownloadItem(o.getString("url"), o.getString("fileName"));
              item.filePath         = o.optString("filePath");
              item.totalBytes       = o.optLong("totalBytes");
              item.downloadedBytes  = o.optLong("downloadedBytes");
              item.state            = DownloadState.valueOf(o.optString("state", "QUEUED"));
              item.timestamp        = o.optLong("timestamp", System.currentTimeMillis());
              return item;
          }
      }

      private static EternaDownloadManager sInstance;

      private final Context          mContext;
      private final SharedPreferences mPrefs;
      private final List<DownloadItem> mQueue     = new ArrayList<>();
      private final ExecutorService  mExecutor   = Executors.newFixedThreadPool(MAX_PARALLEL);

      private EternaDownloadManager(@NonNull Context context) {
          mContext = context.getApplicationContext();
          mPrefs   = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
          loadQueueFromPrefs();
      }

      @NonNull
      public static EternaDownloadManager getInstance(@NonNull Context context) {
          if (sInstance == null) sInstance = new EternaDownloadManager(context);
          return sInstance;
      }

      /** Enqueue a new download. */
      public synchronized void enqueue(@NonNull String url, @NonNull String fileName) {
          DownloadItem item = new DownloadItem(url, fileName);
          mQueue.add(item);
          persistQueue();
          processQueue();
      }

      /** Pause a download by id. */
      public synchronized void pause(@NonNull String id) {
          DownloadItem item = findById(id);
          if (item != null && item.state == DownloadState.IN_PROGRESS) {
              item.state = DownloadState.PAUSED;
              nativePauseDownload(id);
              persistQueue();
          }
      }

      /** Resume a paused download. */
      public synchronized void resume(@NonNull String id) {
          DownloadItem item = findById(id);
          if (item != null && item.state == DownloadState.PAUSED) {
              item.state = DownloadState.QUEUED;
              persistQueue();
              processQueue();
          }
      }

      /** Retry a failed download. */
      public synchronized void retry(@NonNull String id) {
          DownloadItem item = findById(id);
          if (item != null && item.state == DownloadState.FAILED) {
              item.state            = DownloadState.QUEUED;
              item.downloadedBytes  = 0;
              persistQueue();
              processQueue();
          }
      }

      /** Called by DownloadBootReceiver on device reboot. */
      public synchronized void recoverInterruptedDownloads() {
          for (DownloadItem item : mQueue) {
              if (item.state == DownloadState.IN_PROGRESS) {
                  item.state = DownloadState.QUEUED; // reset to re-attempt
              }
          }
          persistQueue();
          processQueue();
      }

      // ── Private helpers ──────────────────────────────────────────────────────

      private void processQueue() {
          long inFlight = mQueue.stream()
                  .filter(i -> i.state == DownloadState.IN_PROGRESS)
                  .count();
          for (DownloadItem item : mQueue) {
              if (inFlight >= MAX_PARALLEL) break;
              if (item.state == DownloadState.QUEUED) {
                  item.state = DownloadState.IN_PROGRESS;
                  inFlight++;
                  final DownloadItem target = item;
                  mExecutor.execute(() -> nativeStartDownload(
                          target.id, target.url, target.filePath, target.downloadedBytes));
              }
          }
          persistQueue();
      }

      @Nullable
      private DownloadItem findById(@NonNull String id) {
          for (DownloadItem item : mQueue) { if (item.id.equals(id)) return item; }
          return null;
      }

      private void loadQueueFromPrefs() {
          String raw = mPrefs.getString(KEY_QUEUE, "[]");
          try {
              JSONArray arr = new JSONArray(raw);
              for (int i = 0; i < arr.length(); i++) {
                  mQueue.add(DownloadItem.fromJson(arr.getJSONObject(i)));
              }
          } catch (JSONException e) {
              Log.e(TAG, "Failed to load download queue", e);
          }
      }

      private void persistQueue() {
          JSONArray arr = new JSONArray();
          for (DownloadItem item : mQueue) {
              try { arr.put(item.toJson()); } catch (JSONException ignored) {}
          }
          mPrefs.edit().putString(KEY_QUEUE, arr.toString()).apply();
      }

      // Native bridges
      private static native void nativeStartDownload(String id, String url, String path, long resumeOffset);
      private static native void nativePauseDownload(String id);
  }
  