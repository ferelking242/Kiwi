// Copyright 2024 Kiwi Browser Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license.

package org.chromium.chrome.browser.tab;

import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.profiles.ProfileManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Java-side manager for Tab Session Isolation.
 *
 * <p>Tracks which tabs are "isolated" and their corresponding isolation keys.
 * Delegates actual storage partition management to the C++ layer via
 * {@link TabSessionIsolationBridge}.
 *
 * <p>Usage:
 * <pre>
 *   // When opening an isolated tab:
 *   String key = TabSessionIsolationManager.getInstance()
 *       .createIsolatedSession(profile, tab.getId());
 *
 *   // When closing an isolated tab:
 *   TabSessionIsolationManager.getInstance()
 *       .releaseIsolatedSession(profile, tab.getId());
 *
 *   // Check if a tab is isolated:
 *   boolean isIsolated = TabSessionIsolationManager.getInstance()
 *       .isTabIsolated(tab.getId());
 * </pre>
 */
@MainThread
public class TabSessionIsolationManager {
    private static final String TAG = "TabSessionIsolation";
    private static final int MAX_ISOLATED_SESSIONS = 10;

    private static volatile TabSessionIsolationManager sInstance;

    // Maps tab ID -> isolation_key (UUID)
    private final Map<Integer, String> mTabToIsolationKey = new HashMap<>();

    private TabSessionIsolationManager() {}

    /** Returns the singleton instance. */
    public static TabSessionIsolationManager getInstance() {
        if (sInstance == null) {
            synchronized (TabSessionIsolationManager.class) {
                if (sInstance == null) {
                    sInstance = new TabSessionIsolationManager();
                }
            }
        }
        return sInstance;
    }

    /**
     * Creates an isolated session for the given tab.
     *
     * <p>Delegates to the C++ layer to allocate a unique
     * {@code StoragePartitionConfig} for this tab. The tab will get its own
     * cookies, localStorage, IndexedDB, cache, and service worker scope.
     *
     * @param profile The current user profile.
     * @param tabId   The Android tab ID.
     * @return The isolation_key, or {@code null} if the max is reached.
     */
    @Nullable
    public String createIsolatedSession(@NonNull Profile profile, int tabId) {
        if (!TabSessionIsolationBridge.canCreateIsolatedSession()) {
            Log.w(TAG, "Cannot create isolated session: maximum of "
                    + MAX_ISOLATED_SESSIONS + " sessions reached.");
            return null;
        }

        if (mTabToIsolationKey.containsKey(tabId)) {
            Log.w(TAG, "Tab " + tabId + " already has an isolated session.");
            return mTabToIsolationKey.get(tabId);
        }

        String isolationKey = TabSessionIsolationBridge.createIsolationSession(profile);
        if (isolationKey == null || isolationKey.isEmpty()) {
            Log.e(TAG, "Native failed to create isolation session.");
            return null;
        }

        mTabToIsolationKey.put(tabId, isolationKey);
        Log.i(TAG, "Created isolated session for tab " + tabId
                + ": " + isolationKey.substring(0, 8) + "...");
        return isolationKey;
    }

    /**
     * Releases the isolated session for the given tab.
     *
     * <p>Schedules asynchronous deletion of all on-disk storage for this
     * session (cookies, IndexedDB, localStorage, cache, service workers).
     * This should be called when the tab is destroyed.
     *
     * @param profile The current user profile.
     * @param tabId   The Android tab ID.
     */
    public void releaseIsolatedSession(@NonNull Profile profile, int tabId) {
        String isolationKey = mTabToIsolationKey.remove(tabId);
        if (isolationKey == null) {
            return; // Tab was not isolated, nothing to do.
        }

        TabSessionIsolationBridge.destroyIsolationSession(profile, isolationKey);
        Log.i(TAG, "Released isolated session for tab " + tabId);
    }

    /**
     * Returns the isolation_key for the given tab, or {@code null} if
     * the tab is not isolated.
     */
    @Nullable
    public String getIsolationKey(int tabId) {
        return mTabToIsolationKey.get(tabId);
    }

    /**
     * Returns {@code true} if the given tab has an active isolated session.
     */
    public boolean isTabIsolated(int tabId) {
        return mTabToIsolationKey.containsKey(tabId);
    }

    /**
     * Returns the number of currently active isolated tab sessions.
     */
    public int getActiveSessionCount() {
        return mTabToIsolationKey.size();
    }

    /**
     * Returns {@code true} if a new isolated session can be created.
     */
    public boolean canCreateIsolatedSession() {
        return TabSessionIsolationBridge.canCreateIsolatedSession();
    }
}
