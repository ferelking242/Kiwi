package com.aivos.eterna.settings;

  import android.os.Bundle;
  import android.view.MenuItem;
  import androidx.appcompat.app.ActionBar;
  import androidx.appcompat.app.AppCompatActivity;
  import androidx.preference.Preference;
  import androidx.preference.PreferenceFragmentCompat;
  import androidx.preference.PreferenceScreen;
  import androidx.preference.SwitchPreferenceCompat;
  import com.aivos.eterna.R;
  import com.aivos.eterna.immortal.ImmortalModeService;
  import com.aivos.eterna.session.SessionProtectionManager;
  import com.aivos.eterna.shizuku.ShizukuIntegration;

  /**
   * Eterna Browser — EternaControlCenterActivity
   *
   * The Eterna Control Center — a dedicated settings screen with sections:
   *   - Session Protection
   *   - Immortal Mode
   *   - Battery Status
   *   - Background Status
   *   - Shizuku
   *   - Extensions
   *   - Downloads
   *   - Storage
   *   - Performance
   */
  public class EternaControlCenterActivity extends AppCompatActivity {

      @Override
      protected void onCreate(Bundle savedInstanceState) {
          super.onCreate(savedInstanceState);
          setContentView(R.layout.activity_eterna_control_center);

          ActionBar ab = getSupportActionBar();
          if (ab != null) {
              ab.setTitle(R.string.eterna_settings_title);
              ab.setDisplayHomeAsUpEnabled(true);
          }

          if (savedInstanceState == null) {
              getSupportFragmentManager()
                      .beginTransaction()
                      .replace(R.id.settings_container, new ControlCenterFragment())
                      .commit();
          }
      }

      @Override
      public boolean onOptionsItemSelected(MenuItem item) {
          if (item.getItemId() == android.R.id.home) { finish(); return true; }
          return super.onOptionsItemSelected(item);
      }

      // ── Fragment ─────────────────────────────────────────────────────────────

      public static class ControlCenterFragment extends PreferenceFragmentCompat {

          @Override
          public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
              setPreferencesFromResource(R.xml.eterna_control_center_prefs, rootKey);
              bindSessionProtection();
              bindImmortalMode();
              bindShizuku();
          }

          private void bindSessionProtection() {
              SwitchPreferenceCompat sw = findPreference("pref_session_protection_global");
              if (sw == null) return;
              sw.setOnPreferenceChangeListener((pref, newVal) -> {
                  boolean enabled = (Boolean) newVal;
                  if (!enabled) {
                      SessionProtectionManager.getInstance(requireContext())
                              .snapshotNow();
                  }
                  return true;
              });
          }

          private void bindImmortalMode() {
              SwitchPreferenceCompat sw = findPreference("pref_immortal_mode_global");
              if (sw == null) return;
              sw.setSummary(sw.isChecked()
                      ? getString(R.string.immortal_mode_notification_title)
                      : "Off");
          }

          private void bindShizuku() {
              Preference shizukuPref = findPreference("pref_shizuku_status");
              if (shizukuPref == null) return;
              ShizukuIntegration shizuku = ShizukuIntegration.getInstance(requireContext());
              shizuku.init(new ShizukuIntegration.ShizukuCallback() {
                  @Override public void onShizukuReady() {
                      shizukuPref.setSummary(getString(R.string.shizuku_connected));
                  }
                  @Override public void onShizukuUnavailable() {
                      shizukuPref.setSummary(getString(R.string.shizuku_unavailable));
                  }
                  @Override public void onBatteryRestrictionDetected(String oem, String suggestion) {
                      shizukuPref.setSummary(getString(R.string.shizuku_battery_restriction_detected));
                  }
                  @Override public void onOemKillerDetected(String killerName) {
                      shizukuPref.setSummary(getString(R.string.shizuku_oem_killer_detected));
                  }
              });
          }
      }
  }
  