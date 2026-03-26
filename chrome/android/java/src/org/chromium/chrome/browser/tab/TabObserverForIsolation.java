// Copyright 2024 Kiwi Browser Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license.

package org.chromium.chrome.browser.tab;

import androidx.annotation.NonNull;

import org.chromium.chrome.browser.profiles.Profile;

/**
 * A {@link TabObserver} that listens for tab destruction and releases the
 * associated isolated session when an isolated tab is closed.
 *
 * <p>Attach one instance per tab that is opened via {@link IsolatedTabCreator}.
 *
 * <p>Example:
 * <pre>
 *   Tab isolatedTab = creator.openIsolatedTab(url);
 *   if (isolatedTab != null) {
 *       isolatedTab.addObserver(
 *           new TabObserverForIsolation(profile, isolatedTab.getId()));
 *   }
 * </pre>
 */
public class TabObserverForIsolation extends EmptyTabObserver {
    private final Profile mProfile;
    private final int mTabId;

    /**
     * @param profile The Chrome profile owning the tab.
     * @param tabId   The ID of the isolated tab to observe.
     */
    public TabObserverForIsolation(@NonNull Profile profile, int tabId) {
        mProfile = profile;
        mTabId = tabId;
    }

    @Override
    public void onDestroyed(Tab tab) {
        // Release storage partition when the tab is closed.
        IsolatedTabCreator.onIsolatedTabDestroyed(mProfile, mTabId);
    }
}
