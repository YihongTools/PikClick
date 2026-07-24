# Local release signing / 本機正式版簽章

PikClick keeps the Android release private key on the maintainer's computer. GitHub Actions only builds an unsigned APK and never receives the keystore or its passwords.

PikClick 將 Android 正式版私鑰保留在維護者電腦。GitHub Actions 只建置未簽章 APK，不會取得 keystore 或任何密碼。

## 1. Protect the signing material / 保護簽章資料

Keep these ignored local files in the repository root:

- `release-key.jks`
- `keystore.properties`

The properties file must define `storeFile`, `storePassword`, `keyAlias`, and `keyPassword`. Keep an encrypted offline backup. Never commit, upload, paste, or log either file or its values.

`keystore.properties` 必須包含上述四個欄位。請另做加密離線備份，且不得提交、上傳、貼出或記錄檔案及密碼內容。

## 2. Build and verify locally / 本機建置與驗證

Run from PowerShell in the repository root:

```powershell
.\gradlew.bat clean testDebugUnitTest lintRelease checksumReleaseApk --stacktrace
```

The command builds a locally signed and optimized APK, then writes these files:

- `dist/PikClick-v2.1.1-release.apk`
- `dist/PikClick-v2.1.1-release.apk.sha256`

Verify the APK signature before upload:

```powershell
$sdk = $env:LOCALAPPDATA + "\Android\Sdk"
$apksigner = Get-ChildItem "$sdk\build-tools\*\apksigner.bat" | Sort-Object FullName -Descending | Select-Object -First 1
& $apksigner.FullName verify --verbose --print-certs .\dist\PikClick-v2.1.1-release.apk
Get-FileHash .\dist\PikClick-v2.1.1-release.apk -Algorithm SHA256
```

Confirm that `Verified` is shown and that the SHA-256 output matches the `.sha256` file.

確認輸出包含 `Verified`，並核對 SHA-256 與 `.sha256` 檔案一致。

## 3. Upload manually / 手動上傳

Create or edit the GitHub Release, then upload both the signed APK and its `.sha256` file. Do not upload the unsigned CI artifact as a public release asset.

建立或編輯 GitHub Release，手動上傳已簽章 APK 與 `.sha256`。請勿將 CI 的未簽章 artifact 當成公開正式版附件。

The CI workflow explicitly passes `-PunsignedRelease=true`, so it cannot accidentally use a signing configuration even if a runner environment changes.

CI 明確傳入 `-PunsignedRelease=true`，即使 runner 環境改變，也不會意外套用簽章設定。

## Recovery rule / 復原規則

If the signing key or either password is unavailable, stop the release. Do not generate a replacement key for an update: Android will reject it as a different signer.

若私鑰或任一密碼無法取得，必須停止發布。更新既有 App 時不可改用新金鑰，否則 Android 會視為不同簽署者並拒絕更新。
