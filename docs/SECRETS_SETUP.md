# GitHub Secrets Setup

  Before the CI/CD build can sign a release APK, configure these secrets in  
  **Settings → Secrets and variables → Actions** of the `eterna-browser` repository.

  | Secret name                  | Description                                      |
  |------------------------------|--------------------------------------------------|
  | `ETERNA_KEYSTORE_BASE64`     | Base64-encoded Android keystore (.jks/.keystore) |
  | `ETERNA_KEYSTORE_PASSWORD`   | Keystore password                                |
  | `ETERNA_KEY_ALIAS`           | Key alias inside the keystore                    |
  | `ETERNA_KEY_PASSWORD`        | Key password (if different from keystore)        |

  ## Generating a keystore

  ```bash
  keytool -genkey -v \
    -keystore eterna-release.keystore \
    -alias eterna \
    -keyalg RSA \
    -keysize 4096 \
    -validity 10000 \
    -dname "CN=AIVOS, OU=Eterna Browser, O=AIVOS, L=Unknown, S=Unknown, C=US"
  ```

  ## Encoding for GitHub Secrets

  ```bash
  base64 -w 0 eterna-release.keystore
  ```

  Paste the output as the value of `ETERNA_KEYSTORE_BASE64`.

  > **Never** commit the keystore file or its password to the repository.
  