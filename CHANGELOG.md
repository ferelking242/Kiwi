# Changelog

  All notable changes to Eterna Browser are documented here.  
  Format follows [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).  
  Versions follow [Semantic Versioning](https://semver.org/).

  ---

  ## [Unreleased]

  ### Added
  - **Isolated Tabs** — per-tab OTR profiles with fully separated cookies, localStorage,
    IndexedDB, cache, service workers and login sessions (`IsolatedTabManager`)
  - **Immortal Mode** — foreground service with smart WakeLock and periodic session
    snapshots to maximise background tab survival (`ImmortalModeService`)
  - **Session Protection** — auto-save URL, scroll, form data and cookies for
    protected tabs; auto-recovery after crash, reboot or force-close
    (`SessionSnapshotManager`, `SessionProtectionManager`, `SessionRecoveryCoordinator`)
  - **BootReceiver** — re-queues protected tab recovery and interrupted downloads
    on device reboot
  - **EternaDownloadManager** — crash-safe download manager with pause, resume,
    retry, parallel downloads (up to 3), queue and boot recovery
  - **Shizuku Integration** — optional detection of OEM battery restrictions and
    background process killers with user-facing suggestions
  - **Tab Creation Menu** — long-press + shows: New Tab / New Isolated Tab /
    New Private Tab / New Group / New Isolated Group
  - **Tab Context Menu** — long-press tab shows: Reload / Duplicate /
    Duplicate As Isolated / Move To Group / Pin / Protect Session /
    Keep Alive / Export Session / Close Others
  - **Eterna Control Center** — dedicated settings screen with sections:
    Session Protection · Immortal Mode · Battery Status · Background Status ·
    Shizuku · Extensions · Downloads · Storage · Performance
  - **Branding** — full rebrand from Kiwi Browser to Eterna Browser by AIVOS;
    package `com.aivos.eterna`, label "Eterna", tagline "Never Sleep. Never Forget."
  - **Logo** — infinity-symbol merged with watchtower eye; deep blue + electric cyan;
    adaptive icon, foreground, monochrome, notification (24dp) and splash variants
  - **CI/CD** — GitHub Actions for arm64-v8a APK build, lint/branding checks,
    tagged release publishing; APK artifacts retained 30 days
  - **JNI Bridge** — `eterna_jni.cc` connecting Java feature layer to Chromium C++
    engine (tab state, download control, session recovery)

  ### Changed
  - Fork base: kiwibrowser/src.next → ferelking242/eterna-browser (branch: `kiwi`)
  - All Kiwi Browser strings, package identifiers and resource names replaced
    via `eterna/scripts/apply_branding.py`
  - Build target restricted to `arm64-v8a` only

  ### Removed
  - All Kiwi Browser branding (name, icons, company references)
  - armeabi-v7a, x86, x86_64 build targets
  