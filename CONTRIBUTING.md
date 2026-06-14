# Contributing to Eterna Browser

  ## Commit Convention

  All commits must follow [Conventional Commits](https://www.conventionalcommits.org/):

  | Prefix     | When to use                              |
  |------------|------------------------------------------|
  | `feat:`    | New feature                              |
  | `fix:`     | Bug fix                                  |
  | `refactor:`| Code restructure, no behaviour change    |
  | `perf:`    | Performance improvement                  |
  | `build:`   | Build system / dependency changes        |
  | `ci:`      | CI/CD configuration                      |
  | `docs:`    | Documentation only                       |
  | `test:`    | Tests                                    |

  ## Branch Naming

  ```
  feat/<short-description>
  fix/<short-description>
  refactor/<short-description>
  ```

  ## Pull Requests

  - Target the `kiwi` branch.
  - All CI checks must pass before merge.
  - No TODO comments in production code.
  - No placeholder or mock implementations.

  ## Architecture

  See `eterna/` for all Eterna-specific source:

  ```
  eterna/
  ├── assets/          # Logo SVGs and raw icons
  ├── config/          # GN build config, gclient config
  ├── java/            # Java source (features)
  │   └── com/aivos/eterna/
  │       ├── download/        # EternaDownloadManager
  │       ├── immortal/        # ImmortalModeService
  │       ├── isolated/        # IsolatedTabManager
  │       ├── session/         # Snapshot, Protection, Recovery
  │       ├── settings/        # EternaControlCenterActivity
  │       ├── shizuku/         # ShizukuIntegration
  │       └── tabs/            # TabCreationMenuDelegate
  ├── manifests/       # AndroidManifest overlay
  ├── res/             # Android resource overlays
  └── scripts/         # Build and CI helper scripts
  ```

  ## Building

  See `README.md` for full build instructions.
  