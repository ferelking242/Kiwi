package com.aivos.eterna.tabs;

  import android.content.Context;
  import android.view.Menu;
  import android.view.MenuItem;
  import android.widget.PopupMenu;
  import androidx.annotation.NonNull;
  import com.aivos.eterna.R;
  import com.aivos.eterna.isolated.IsolatedTabManager;
  import com.aivos.eterna.session.SessionProtectionManager;
  import com.aivos.eterna.immortal.ImmortalModeService;

  /**
   * Eterna Browser — TabCreationMenuDelegate
   *
   * Handles the "+" button and the long-press tab context menu.
   *
   * + tap         → New Tab
   * + long-press  → New Tab / New Isolated Tab / New Private Tab / New Group / New Isolated Group
   *
   * Tab long-press → Reload / Duplicate / Duplicate As Isolated / Move To Group /
   *                  Pin / Protect Session / Keep Alive / Export Session / Close Others
   */
  public class TabCreationMenuDelegate {

      public interface TabMenuListener {
          void onNewTab();
          void onNewIsolatedTab();
          void onNewPrivateTab();
          void onNewGroup();
          void onNewIsolatedGroup();
          // Tab context actions
          void onReload(int tabId);
          void onDuplicate(int tabId);
          void onDuplicateAsIsolated(int tabId);
          void onMoveToGroup(int tabId);
          void onPin(int tabId);
          void onProtectSession(int tabId);
          void onKeepAlive(int tabId);
          void onExportSession(int tabId);
          void onCloseOthers(int tabId);
      }

      private final Context           mContext;
      private final TabMenuListener   mListener;

      public TabCreationMenuDelegate(@NonNull Context context, @NonNull TabMenuListener listener) {
          mContext  = context;
          mListener = listener;
      }

      /** Show the "+" long-press creation menu. */
      public void showCreationMenu(@NonNull android.view.View anchor) {
          PopupMenu popup = new PopupMenu(mContext, anchor);
          Menu menu = popup.getMenu();
          menu.add(Menu.NONE, R.id.menu_new_tab,              0, R.string.new_tab);
          menu.add(Menu.NONE, R.id.menu_new_isolated_tab,     1, R.string.new_isolated_tab);
          menu.add(Menu.NONE, R.id.menu_new_private_tab,      2, R.string.new_private_tab);
          menu.add(Menu.NONE, R.id.menu_new_group,            3, R.string.new_group);
          menu.add(Menu.NONE, R.id.menu_new_isolated_group,   4, R.string.new_isolated_group);

          popup.setOnMenuItemClickListener(item -> {
              int id = item.getItemId();
              if      (id == R.id.menu_new_tab)            mListener.onNewTab();
              else if (id == R.id.menu_new_isolated_tab)   mListener.onNewIsolatedTab();
              else if (id == R.id.menu_new_private_tab)    mListener.onNewPrivateTab();
              else if (id == R.id.menu_new_group)          mListener.onNewGroup();
              else if (id == R.id.menu_new_isolated_group) mListener.onNewIsolatedGroup();
              return true;
          });
          popup.show();
      }

      /** Show the tab long-press context menu for a specific tab. */
      public void showTabContextMenu(@NonNull android.view.View anchor, int tabId) {
          boolean isProtected = SessionProtectionManager.getInstance(mContext).isProtected(tabId);
          boolean isIsolated  = IsolatedTabManager.getInstance(mContext).isIsolatedTab(tabId);

          PopupMenu popup = new PopupMenu(mContext, anchor);
          Menu menu = popup.getMenu();
          menu.add(Menu.NONE, R.id.tab_menu_reload,             0, R.string.tab_menu_reload);
          menu.add(Menu.NONE, R.id.tab_menu_duplicate,          1, R.string.tab_menu_duplicate);
          menu.add(Menu.NONE, R.id.tab_menu_duplicate_isolated, 2, R.string.tab_menu_duplicate_isolated);
          menu.add(Menu.NONE, R.id.tab_menu_move_to_group,      3, R.string.tab_menu_move_to_group);
          menu.add(Menu.NONE, R.id.tab_menu_pin,                4, R.string.tab_menu_pin);
          menu.add(Menu.NONE, R.id.tab_menu_protect_session,    5,
                  isProtected ? "✓ " + mContext.getString(R.string.tab_menu_protect_session)
                               : mContext.getString(R.string.tab_menu_protect_session));
          menu.add(Menu.NONE, R.id.tab_menu_keep_alive,         6, R.string.tab_menu_keep_alive);
          menu.add(Menu.NONE, R.id.tab_menu_export_session,     7, R.string.tab_menu_export_session);
          menu.add(Menu.NONE, R.id.tab_menu_close_others,       8, R.string.tab_menu_close_others);

          popup.setOnMenuItemClickListener(item -> {
              int id = item.getItemId();
              if      (id == R.id.tab_menu_reload)             mListener.onReload(tabId);
              else if (id == R.id.tab_menu_duplicate)          mListener.onDuplicate(tabId);
              else if (id == R.id.tab_menu_duplicate_isolated) mListener.onDuplicateAsIsolated(tabId);
              else if (id == R.id.tab_menu_move_to_group)      mListener.onMoveToGroup(tabId);
              else if (id == R.id.tab_menu_pin)                mListener.onPin(tabId);
              else if (id == R.id.tab_menu_protect_session)    mListener.onProtectSession(tabId);
              else if (id == R.id.tab_menu_keep_alive)         mListener.onKeepAlive(tabId);
              else if (id == R.id.tab_menu_export_session)     mListener.onExportSession(tabId);
              else if (id == R.id.tab_menu_close_others)       mListener.onCloseOthers(tabId);
              return true;
          });
          popup.show();
      }
  }
  