# Eterna Browser — Architecture

  ## Overview

  Eterna Browser is a fork of [Kiwi Browser](https://github.com/kiwibrowser/src.next),
  which is itself a fork of Chromium for Android. All Eterna-specific code lives in the
  `eterna/` directory at the repository root.

  ## Directory Structure

  ```
  eterna/
  ├── assets/                     # Logo SVGs (full-color, fg, mono, notification, splash)
  ├── BUILD.gn                    # GN build targets for Eterna Java + resources
  ├── config/
  │   ├── eterna_build_config.gni # Shared GN args
  │   ├── gclient_config.py       # gclient .gclient configuration template
  │   └── args_arm64_release.gn   # Release build args for out/eterna_arm64/args.gn
  ├── java/com/aivos/eterna/
  │   ├── EternaApplication.java  # App entry — initialises all subsystems at startup
  │   ├── download/
  │   │   ├── EternaDownloadManager.java    # Crash-safe download queue
  │   │   └── DownloadBootReceiver.java     # Resumes downloads on reboot
  │   ├── immortal/
  │   │   └── ImmortalModeService.java      # Foreground keep-alive service
  │   ├── isolated/
  │   │   └── IsolatedTabManager.java       # Per-tab OTR profile management
  │   ├── session/
  │   │   ├── SessionSnapshotManager.java   # JSON persistence of tab state
  │   │   ├── SessionProtectionManager.java # High-level protection coordinator
  │   │   ├── SessionRecoveryCoordinator.java # Startup recovery
  │   │   └── BootReceiver.java             # Schedules recovery on reboot
  │   ├── settings/
  │   │   └── EternaControlCenterActivity.java  # Settings UI
  │   ├── shizuku/
  │   │   └── ShizukuIntegration.java       # Optional OEM restriction detection
  │   └── tabs/
  │       └── TabCreationMenuDelegate.java  # + button and tab context menus
  ├── manifests/
  │   └── AndroidManifest.xml     # Overlay manifest (services, receivers, permissions)
  ├── native/
  │   ├── eterna_jni.cc           # C++ JNI bridge to Chromium engine
  │   └── BUILD.gn
  ├── res/
  │   ├── drawable/               # ic_launcher_background.xml
  │   ├── layout/                 # activity_eterna_control_center.xml
  │   ├── mipmap-anydpi-v26/      # Adaptive icon XMLs
  │   ├── values/                 # strings.xml, channel_constants.xml, ids.xml, arrays.xml
  │   └── xml/                    # eterna_control_center_prefs.xml
  └── scripts/
      ├── apply_branding.py       # Replaces Kiwi → Eterna across codebase
      ├── check_branding.py       # Verifies required Eterna keywords are present
      ├── check_no_kiwi.py        # Ensures no Kiwi remnants in overlays
      ├── validate_resources.py   # Validates all XML files are well-formed
      └── validate_gn_args.py     # Validates eterna_build_config.gni
  ```

  ## Feature Architecture

  ### Isolated Tabs
  Each isolated tab is backed by a dedicated Chromium off-the-record Profile  
  (identified by a UUID key). The Profile is created on tab creation and destroyed  
  when the tab is closed. No data flows between OTR profiles.

  ### Immortal Mode
  A foreground Service (`ImmortalModeService`) is started when the first tab is  
  marked Keep Alive. It holds a partial WakeLock (10 s timeout, renewed as needed)  
  and schedules periodic session snapshots every 30 s (configurable). On  
  `onTaskRemoved`, it performs a synchronous emergency snapshot.

  ### Session Protection
  Protected tabs are snapshotted to `SharedPreferences` as JSON. On startup,  
  `SessionRecoveryCoordinator` reads the snapshots and requests the Chromium  
  `TabCreator` to recreate each tab at the saved URL with scroll position.

  ### Download Manager
  `EternaDownloadManager` persists the download queue to `SharedPreferences`.  
  Up to 3 downloads run in parallel. On reboot, `DownloadBootReceiver` calls  
  `recoverInterruptedDownloads()` to re-queue in-flight items.

  ## Build Pipeline

  1. Fork sync: `gclient sync`
  2. Branding patch: `python3 eterna/scripts/apply_branding.py`
  3. GN generate: `gn gen out/eterna_arm64 --args=...`
  4. Build: `autoninja -C out/eterna_arm64 chrome_public_apk`
  5. APK: `out/eterna_arm64/apks/ChromePublic.apk` → renamed `eterna-browser-arm64-v8a.apk`

  ## Target

  - ABI: arm64-v8a exclusively
  - Min SDK: 23 (Android 6.0)
  - Target SDK: 34 (Android 14)
  - Package: com.aivos.eterna
  