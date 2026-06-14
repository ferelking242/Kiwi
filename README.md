# Eterna Browser

  ![Build](https://github.com/ferelking242/eterna-browser/actions/workflows/build.yml/badge.svg)
  ![Lint](https://github.com/ferelking242/eterna-browser/actions/workflows/lint.yml/badge.svg)
  ![Platform](https://img.shields.io/badge/platform-Android%20arm64--v8a-blue)
  ![License](https://img.shields.io/badge/license-BSD--3--Clause-green)

  > **Never Sleep. Never Forget.**

  Eterna Browser is a Chromium-based Android browser by **AIVOS**, forked from Kiwi Browser.  
  It is engineered for session persistence, isolated identities, Chrome extension support, and maximum background reliability.

  ---

  ## Features

  | Feature | Description |
  |---------|-------------|
  | 🔒 **Isolated Tabs** | Each tab has its own cookies, storage, service workers and login session |
  | ♾️ **Immortal Mode** | Keep-alive foreground service with smart WakeLocks and session snapshots |
  | 🛡️ **Session Protection** | Auto-save URL, scroll, forms, cookies — auto-recover on crash/reboot |
  | 👤 **Multi Profiles** | Fully separated browser profiles |
  | 🗂️ **Tab Groups** | Grouped and isolated tab groups |
  | 🧩 **Extensions** | Chrome extensions, Manifest V2 + V3 |
  | ⬇️ **Download Manager** | Pause, resume, retry, parallel, crash-safe |
  | 📡 **Shizuku Support** | Optional — detect OEM battery killers |

  ## Build

  ### Requirements
  - Android SDK / NDK
  - depot_tools (Chromium)
  - JDK 17
  - Python 3.11+
  - Ubuntu 22.04+ build host

  ### Quick Start
  ```bash
  gclient config --name src --unmanaged https://github.com/ferelking242/eterna-browser.git
  gclient sync --nohooks --no-history -D
  gclient runhooks
  python3 eterna/scripts/apply_branding.py
  gn gen out/eterna_arm64 --args="target_os=\"android\" target_cpu=\"arm64\" is_official_build=true"
  autoninja -C out/eterna_arm64 chrome_public_apk
  ```

  ### CI/CD
  Every push triggers an automated build.  
  APK artifacts are available under **Actions → Build → Artifacts**.

  ## Architecture

  - **Target ABI:** arm64-v8a only
  - **Engine:** Chromium (latest Kiwi base)
  - **Min SDK:** 23 (Android 6.0)
  - **Target SDK:** 34 (Android 14)
  - **Package:** com.aivos.eterna

  ## Package

  `com.aivos.eterna`

  ## Company

  **AIVOS**

  ## License

  BSD 3-Clause — see [LICENSE](LICENSE)
  