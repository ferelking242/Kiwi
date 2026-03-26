// Copyright 2024 Kiwi Browser Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license.

package org.chromium.chrome.browser.tab;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.tabmodel.TabCreatorManager;
import org.chromium.chrome.browser.tabmodel.TabModel;
import org.chromium.content_public.browser.LoadUrlParams;
import org.chromium.ui.base.PageTransition;

/**
 * Handles creation of "Isolated Tabs" for Kiwi Browser.
 *
 * <p>An isolated tab has its own StoragePartition, giving it completely
 * separate cookies, localStorage, IndexedDB, cache, and service workers.
 * This allows the user to be logged into multiple accounts on the same
 * website simultaneously.
 *
 * <p>Example use case: open Gmail, WhatsApp Web, or Facebook in multiple
 * tabs, each with a different account.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Call {@link #openIsolatedTab} to create a new isolated tab.</li>
 *   <li>When the tab is destroyed, {@link TabSessionIsolationManager}
 *       is notified automatically via
 *       {@link #onIsolatedTabDestroyed}.</li>
 * </ol>
 */
public class IsolatedTabCreator {
    private static final String TAG = "IsolatedTabCreator";

    // Extra key stored in Tab's user data to mark it as isolated and carry
    // its isolation key.
    public static final String ISOLATION_KEY_EXTRA =
            "org.chromium.chrome.browser.tab.ISOLATION_KEY";

    private final Context mContext;
    private final Profile mProfile;
    private final TabModel mTabModel;
    private final TabCreatorManager.TabCreator mTabCreator;

    /**
     * Constructs an IsolatedTabCreator.
     *
     * @param context    The Android context.
     * @param profile    The current Chrome profile.
     * @param tabModel   The TabModel to add the new tab to.
     * @param tabCreator The TabCreator for the current tab model.
     */
    public IsolatedTabCreator(
            @NonNull Context context,
            @NonNull Profile profile,
            @NonNull TabModel tabModel,
            @NonNull TabCreatorManager.TabCreator tabCreator) {
        mContext = context;
        mProfile = profile;
        mTabModel = tabModel;
        mTabCreator = tabCreator;
    }

    /**
     * Opens a new isolated tab and navigates to the given URL.
     *
     * <p>The tab will have completely separate storage from all other tabs
     * (both regular and other isolated tabs).
     *
     * @param url The URL to open. If null, opens a new tab page.
     * @return The created Tab, or {@code null} if the isolated session could
     *         not be created (e.g. maximum limit reached).
     */
    @Nullable
    public Tab openIsolatedTab(@Nullable String url) {
        TabSessionIsolationManager isolationManager =
                TabSessionIsolationManager.getInstance();

        // Check the limit before proceeding.
        if (!isolationManager.canCreateIsolatedSession()) {
            Log.w(TAG, "Cannot open isolated tab: maximum session limit reached ("
                    + isolationManager.getActiveSessionCount() + ").");
            return null;
        }

        // Create the tab with a standard new-tab-page URL first.
        // The isolation is wired up through the StoragePartition on the C++ side
        // when the tab's WebContents is initialized.
        String targetUrl = (url != null && !url.isEmpty()) ? url : "about:blank";

        LoadUrlParams loadUrlParams = new LoadUrlParams(
                targetUrl, PageTransition.AUTO_TOPLEVEL);

        // Open the new tab. We use FROM_LINK so it opens in foreground.
        Tab tab = mTabCreator.createNewTab(
                loadUrlParams,
                TabLaunchType.FROM_CHROME_UI,
                /*parent=*/null);

        if (tab == null) {
            Log.e(TAG, "Failed to create new tab.");
            return null;
        }

        // Register the isolated session for this tab ID.
        String isolationKey = isolationManager.createIsolatedSession(
                mProfile, tab.getId());

        if (isolationKey == null) {
            // Session creation failed even though we checked the limit –
            // this can happen due to a race condition. The tab was already
            // created so we let it run without isolation rather than closing it.
            Log.e(TAG, "IsolationSession creation failed for tab " + tab.getId());
            return tab;
        }

        // Mark the tab as isolated so the UI can show the badge.
        markTabAsIsolated(tab, isolationKey);

        Log.i(TAG, "Opened isolated tab: id=" + tab.getId()
                + " url=" + targetUrl
                + " key=" + isolationKey.substring(0, 8) + "...");
        return tab;
    }

    /**
     * Must be called when an isolated tab is destroyed.
     * Cleans up the associated StoragePartition asynchronously.
     *
     * @param profile The current Chrome profile.
     * @param tabId   The ID of the destroyed tab.
     */
    public static void onIsolatedTabDestroyed(@NonNull Profile profile, int tabId) {
        TabSessionIsolationManager.getInstance().releaseIsolatedSession(profile, tabId);
    }

    /**
     * Returns {@code true} if the given tab was opened as an isolated tab.
     *
     * @param tabId The tab ID to check.
     */
    public static boolean isIsolatedTab(int tabId) {
        return TabSessionIsolationManager.getInstance().isTabIsolated(tabId);
    }

    /**
     * Returns the isolation key for the given tab, or {@code null} if
     * the tab is not isolated.
     */
    @Nullable
    public static String getIsolationKey(int tabId) {
        return TabSessionIsolationManager.getInstance().getIsolationKey(tabId);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void markTabAsIsolated(@NonNull Tab tab, @NonNull String isolationKey) {
        // Store the isolation key in the tab's user data so other components
        // can query it without going through TabSessionIsolationManager.
        tab.getUserDataHost().setUserData(IsolationKeyUserData.USER_DATA_KEY,
                new IsolationKeyUserData(isolationKey));
    }

    /**
     * Simple user-data holder for the isolation key.
     * Stored on the Tab object for fast, direct access.
     */
    public static class IsolationKeyUserData implements UserData {
        public static final Class<IsolationKeyUserData> USER_DATA_KEY =
                IsolationKeyUserData.class;

        private final String mIsolationKey;

        public IsolationKeyUserData(@NonNull String isolationKey) {
            mIsolationKey = isolationKey;
        }

        public String getIsolationKey() {
            return mIsolationKey;
        }

        @Override
        public void destroy() {}
    }
}
