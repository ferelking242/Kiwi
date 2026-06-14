package com.aivos.eterna.isolated;

  import android.content.Context;
  import androidx.annotation.NonNull;
  import androidx.annotation.Nullable;
  import org.chromium.base.annotations.JNINamespace;
  import org.chromium.chrome.browser.profiles.Profile;
  import org.chromium.chrome.browser.profiles.ProfileManager;
  import org.chromium.chrome.browser.tab.Tab;
  import org.chromium.chrome.browser.tab.TabCreationState;
  import org.chromium.chrome.browser.tab.TabLaunchType;
  import org.chromium.chrome.browser.tabmodel.TabModel;
  import org.chromium.content_public.browser.LoadUrlParams;
  import java.util.HashMap;
  import java.util.Map;
  import java.util.UUID;

  /**
   * Eterna Browser — IsolatedTabManager
   *
   * Manages isolated tab profiles. Each isolated tab receives its own off-the-record
   * Profile so that cookies, localStorage, IndexedDB, cache, service workers and
   * login sessions are fully separated from every other tab.
   *
   * Multiple accounts on the same origin can run simultaneously in separate
   * isolated tabs with zero data leakage between them.
   */
  @JNINamespace("eterna")
  public class IsolatedTabManager {

      private static IsolatedTabManager sInstance;

      /** Maps isolatedTabId → dedicated OTR Profile key */
      private final Map<Integer, String> mIsolatedTabProfiles = new HashMap<>();
      /** Maps OTR profile key → Profile */
      private final Map<String, Profile> mProfileCache = new HashMap<>();

      private final Context mContext;

      private IsolatedTabManager(@NonNull Context context) {
          mContext = context.getApplicationContext();
      }

      @NonNull
      public static IsolatedTabManager getInstance(@NonNull Context context) {
          if (sInstance == null) {
              sInstance = new IsolatedTabManager(context);
          }
          return sInstance;
      }

      /**
       * Create a new isolated tab with a dedicated off-the-record profile.
       *
       * @param tabModel   The host tab model.
       * @param url        The URL to load in the new isolated tab.
       * @return           The newly created isolated Tab.
       */
      @NonNull
      public Tab createIsolatedTab(@NonNull TabModel tabModel, @NonNull String url) {
          String profileKey = "eterna_isolated_" + UUID.randomUUID().toString();
          Profile isolatedProfile = createDedicatedOtrProfile(profileKey);

          Tab tab = tabModel.getTabCreator(/* incognito= */ false).createNewTab(
                  new LoadUrlParams(url),
                  TabLaunchType.FROM_CHROME_UI,
                  /* parent= */ null);

          if (tab != null) {
              mIsolatedTabProfiles.put(tab.getId(), profileKey);
              attachIsolatedProfile(tab, isolatedProfile);
          }
          return tab;
      }

      /**
       * Returns true if the given tab is an isolated tab managed by Eterna.
       */
      public boolean isIsolatedTab(int tabId) {
          return mIsolatedTabProfiles.containsKey(tabId);
      }

      /**
       * Release the isolated profile associated with a tab when the tab is closed.
       */
      public void onTabClosed(int tabId) {
          String profileKey = mIsolatedTabProfiles.remove(tabId);
          if (profileKey != null) {
              Profile profile = mProfileCache.remove(profileKey);
              if (profile != null) {
                  profile.destroyWhenAppropriate();
              }
          }
      }

      /** Create a fresh off-the-record profile keyed by name. */
      @NonNull
      private Profile createDedicatedOtrProfile(@NonNull String key) {
          Profile regular = ProfileManager.getLastUsedRegularProfile();
          Profile otr = regular.getOffTheRecordProfile(
                  new Profile.OTRProfileID(key), /* createIfNeeded= */ true);
          mProfileCache.put(key, otr);
          return otr;
      }

      /** Attach the isolated profile to the tab at the native layer. */
      private void attachIsolatedProfile(@NonNull Tab tab, @NonNull Profile profile) {
          nativeAttachIsolatedProfile(tab.getId(), profile);
      }

      // Native bridge
      private static native void nativeAttachIsolatedProfile(int tabId, Profile profile);
  }
  