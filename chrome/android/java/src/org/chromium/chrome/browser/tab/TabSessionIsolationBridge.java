// Copyright 2024 Kiwi Browser Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license.

package org.chromium.chrome.browser.tab;

import org.chromium.base.annotations.JNINamespace;
import org.chromium.base.annotations.NativeMethods;
import org.chromium.chrome.browser.profiles.Profile;

/**
 * JNI bridge for native TabSessionIsolationManager.
 *
 * <p>This class exposes the C++ TabSessionIsolationManager to Java.
 * All methods must be called on the UI thread.
 */
@JNINamespace("tab_session_isolation")
public class TabSessionIsolationBridge {

    /**
     * Creates a new isolated session.
     *
     * @param profile The current user profile.
     * @return A UUID string (isolation_key) identifying the session,
     *         or an empty string if the maximum number of sessions
     *         ({@code TabSessionIsolationManager::kMaxIsolatedSessions}) is
     *         already reached.
     */
    public static String createIsolationSession(Profile profile) {
        return TabSessionIsolationBridgeJni.get().createIsolationSession(profile);
    }

    /**
     * Destroys an isolated session and schedules asynchronous deletion of all
     * on-disk storage associated with it (cookies, IndexedDB, localStorage,
     * cache, service workers).
     *
     * <p>Must be called when the corresponding tab is closed.
     *
     * @param profile       The current user profile.
     * @param isolationKey  The UUID returned by {@link #createIsolationSession}.
     */
    public static void destroyIsolationSession(Profile profile, String isolationKey) {
        if (isolationKey == null || isolationKey.isEmpty()) return;
        TabSessionIsolationBridgeJni.get().destroyIsolationSession(profile, isolationKey);
    }

    /**
     * Returns the number of currently active isolated sessions.
     *
     * @return Active session count (0 to kMaxIsolatedSessions).
     */
    public static int getActiveSessionCount() {
        return TabSessionIsolationBridgeJni.get().getActiveSessionCount();
    }

    /**
     * Returns {@code true} if a new isolated session can be created.
     *
     * @return {@code false} if kMaxIsolatedSessions is already reached.
     */
    public static boolean canCreateIsolatedSession() {
        return TabSessionIsolationBridgeJni.get().canCreateIsolatedSession();
    }

    @NativeMethods
    interface Natives {
        String createIsolationSession(Profile profile);
        void destroyIsolationSession(Profile profile, String isolationKey);
        int getActiveSessionCount();
        boolean canCreateIsolatedSession();
    }

    private TabSessionIsolationBridge() {}
}
