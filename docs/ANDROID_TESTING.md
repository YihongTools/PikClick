# Android 整合測試與實機矩陣

## 測試層級

1. JVM：`./gradlew testDebugUnitTest`，驗證延遲邊界、序列 token 與第二次採最新位置。
2. Emulator instrumentation：`./gradlew connectedDebugAndroidTest`，驗證繁中／英文資源由系統 Locale 載入。
3. 實機：驗證懸浮窗、Accessibility gesture、旋轉、撤權、程序終止與 OEM 省電；這些無法由一般 emulator instrumentation 完整模擬。

## GitHub Actions Emulator Matrix

在 repository 的 **Actions → Android Integration Matrix → Run workflow** 手動執行。矩陣使用 API 26、30、35，避免每次 push 都耗用三台 Emulator 的 CI 時間。

## 本機 Emulator／裝置

```bash
adb devices
./gradlew connectedDebugAndroidTest
```

## 實機矩陣

| 類別 | 最低要求 |
|---|---|
| Android 版本 | Android 8、11、13、15 |
| 螢幕 | 小螢幕、一般直向、橫向、瀏海／挖孔 |
| OEM | Pixel／AOSP、Samsung、至少一個積極省電品牌 |
| 語系 | 繁體中文、英文 |

每台裝置執行：

1. 首次安裝，確認無障礙顯著揭露先於系統設定頁。
2. 授權懸浮窗與無障礙服務，執行第一組點擊。
3. 第一次點擊後移動圓點，確認第二次使用新中心。
4. 第一次點擊後旋轉，確認圓點仍在可視範圍，第二次使用旋轉後中心。
5. 第一次點擊後撤銷懸浮窗，再撤銷無障礙服務；兩者都不得送出第二次點擊。
6. 倒數時執行 `adb shell am force-stop com.pikclick.app`；重開後不得補送過期點擊。
7. 快速開始／取消／再開始 20 組，確認舊 callback 不影響新序列。
8. 切換系統語系後重開 App，確認所有操作文字與 Toast 正確本地化。

每次 Release 應在 `docs/RELEASE_CHECKLIST.md` 記錄裝置型號、Android 版本、測試日期、結果與失敗影片／截圖連結。未執行的裝置不得標記通過。
