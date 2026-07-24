# GitHub 首頁實作任務

| Task | 目標與影響範圍 | 相依 | 驗證與完成條件 | 風險 | 狀態 |
|---|---|---|---|---|---|
| T1 | 盤點現有功能、版本、文件與分發檔 | 無 | 全庫搜尋版本與 README 現況 | 將推論當成功能 | 完成 |
| T2 | 建立 v2.1.1 README 正式規格 | T1 | 規格涵蓋目標、邊界、驗收與風險 | 遺漏遠端 Release 狀態 | 完成 |
| T3 | 製作並保存專業 Banner | T2 | 圖檔位於 `docs/images/`，文字與品牌正確 | 生成文字錯誤、外部依賴 | 完成 |
| T4 | 重寫中英 README | T2、T3 | 雙語、功能、畫面、安裝、Roadmap 全部存在 | 虛構截圖或授權 | 完成 |
| T5 | 升級 Android 與分發版本至 2.1.1 | T2 | `versionCode=22`、`versionName=2.1.1`、檔名一致 | 升級簽章或版本碼錯誤 | 完成 |
| T6 | 擴充 `.gitignore` | T1 | 本機環境與簽章材料不會被加入 Git | 誤忽略發布產物 | 完成 |
| T7 | 乾淨建置、測試與 checksum | T5 | Gradle 成功且 SHA-256 相符 | 本機 JDK／SDK 環境 | 完成 |
| T8 | 獨立審查與全庫架構掃描 | T4–T7 | 報告含分級發現、證據與限制 | 動態行為未實機覆蓋 | 完成 |
| T9 | 新位置語意、撤權／旋轉安全與中英本地化 | T8 | JVM、lint、AndroidTest APK 與行為規格 | Android 生命週期競態 | 完成 |
| T10 | Dependabot、CodeQL、Release CI、Emulator Matrix 與 branch protection | T9 | main 的 Android CI／CodeQL 成功，Release v2.1.1 已驗證，Dependabot 工具鏈升級已忽略 | Secrets 與 CI 權限 | 完成（實體矩陣仍待驗收） |
