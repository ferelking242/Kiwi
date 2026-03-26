// Copyright 2024 Kiwi Browser Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license.

package org.chromium.chrome.browser.app;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.chromium.chrome.browser.profiles.Profile;
import org.chromium.chrome.browser.tab.IsolatedTabCreator;
import org.chromium.chrome.browser.tab.Tab;
import org.chromium.chrome.browser.tab.TabObserverForIsolation;
import org.chromium.chrome.browser.tab.TabSessionIsolationManager;
import org.chromium.chrome.browser.tabmodel.TabCreatorManager;
import org.chromium.chrome.browser.tabmodel.TabModel;

/**
 * Handles the "New Isolated Tab" user action in Kiwi Browser.
 *
 * <p>Wire this into the app's overflow menu, long-press tab strip, or
 * any other entry point. Call {@link #openNewIsolatedTab(String)} to
 * create an isolated tab, optionally navigating to a URL.
 *
 * <p>How to wire into the existing Chrome menu (AppMenuHandler):
 * <pre>
 *   // In your menu XML (app_menu.xml), add:
 *   &lt;item
 *       android:id="@+id/new_isolated_tab_menu_id"
 *       android:title="@string/menu_new_isolated_tab"
 *       android:icon="@drawable/ic_isolated_tab"
 *       android:showAsAction="never" /&gt;
 *
 *   // In AppMenuHandlerImpl (or AppMenuDelegate), handle the click:
 *   case R.id.new_isolated_tab_menu_id:
 *       isolatedTabMenuHandler.openNewIsolatedTab(currentUrl);
 *       return true;
 * </pre>
 */
public class IsolatedTabMenuHandler {
    private static final String TAG = "IsolatedTabMenuHandler";

    // Maximum number of isolated sessions (mirrors C++ constant).
    private static final int MAX_SESSIONS = 10;

    private final Context mContext;
    private final Profile mProfile;
    private final TabModel mTabModel;
    private final TabCreatorManager.TabCreator mTabCreator;

    /**
     * @param context    Android context (for Toast messages).
     * @param profile    The current Chrome profile.
     * @param tabModel   The active TabModel.
     * @param tabCreator The TabCreator for normal (non-incognito) tabs.
     */
    public IsolatedTabMenuHandler(
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
     * Opens a new isolated tab.
     *
     * <p>If {@code url} is non-null and non-empty, the tab navigates to it
     * immediately. Otherwise it opens an empty new-tab page.
     *
     * @param url Optional URL to navigate to (may be null).
     */
    public void openNewIsolatedTab(@Nullable String url) {
        TabSessionIsolationManager manager = TabSessionIsolationManager.getInstance();

        if (!manager.canCreateIsolatedSession()) {
            String msg = "Maximum of " + MAX_SESSIONS
                    + " isolated tabs reached. Close some isolated tabs to open new ones.";
            Toast.makeText(mContext, msg, Toast.LENGTH_LONG).show();
            return;
        }

        IsolatedTabCreator creator = new IsolatedTabCreator(
                mContext, mProfile, mTabModel, mTabCreator);

        Tab isolatedTab = creator.openIsolatedTab(url);

        if (isolatedTab == null) {
            Toast.makeText(mContext,
                    "Could not create isolated tab.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Attach observer to clean up storage when the tab is closed.
        isolatedTab.addObserver(
                new TabObserverForIsolation(mProfile, isolatedTab.getId()));

        // Show a brief confirmation toast.
        String sessionInfo = manager.getActiveSessionCount()
                + "/" + MAX_SESSIONS + " isolated sessions active";
        Toast.makeText(mContext,
                "Isolated tab opened – " + sessionInfo,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Returns {@code true} if a new isolated tab can currently be opened.
     * Use this to enable/disable the menu item dynamically.
     */
    public boolean canOpenIsolatedTab() {
        return TabSessionIsolationManager.getInstance().canCreateIsolatedSession();
    }

    /**
     * Returns a display string showing how many isolated sessions are active.
     * Useful for updating a badge or subtitle on the menu item.
     *
     * @return e.g. "3 / 10 isolated sessions"
     */
    public String getSessionCountDisplay() {
        int active = TabSessionIsolationManager.getInstance().getActiveSessionCount();
        return active + " / " + MAX_SESSIONS + " isolated sessions";
    }
}
