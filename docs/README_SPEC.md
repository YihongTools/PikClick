# GitHub 首頁規格（v2.1.0）

## 功能目標

為 `YihongTools/PikClick` 提供可直接作為 GitHub 首頁的中英文 README，清楚傳達用途、限制、安裝、開發與 Roadmap，並將 Android Release 版本統一為 v2.1.0。

## 使用情境與功能需求

- 訪客進入 repository 時，應先看到品牌 Banner、用途、安全界線與最新版本。
- 使用者應能從 README 前往 v2.1.0 Release，辨認 APK 與 checksum 檔名並完成安裝。
- 開發者應能辨認 JDK、SDK、Gradle 版本及測試／建置命令。
- 中文與英文內容必須包含同等核心資訊，不將未驗證的設計稿稱為實機截圖。
- Roadmap 必須區分已完成與待完成事項。

## 非功能需求

- GitHub Markdown 可直接呈現；行動裝置仍可閱讀。
- 不暴露 keystore、密碼、token 或本機設定。
- Banner 存於 repository，不依賴生成服務才能顯示。
- Release 名稱、Gradle 版本與文件引用一致。

## 邊界條件

- Release 尚未上傳 GitHub 時，連結可能暫時顯示 404；README 不宣稱已發布成功。
- 缺少實機截圖時必須明示待補，不得用生成圖冒充。
- 遠端既有 MIT LICENSE 必須保留，README 的授權說明須與其一致。
- Release build 缺少本機簽章材料時應失敗，不得把金鑰提交到 Git。

## 驗收標準

- Given repository 首頁，When GitHub 呈現 README，Then Banner、中文、英文、功能、畫面、安裝與 Roadmap 章節皆存在。
- Given v2.1.0 建置，When 執行 `checksumReleaseApk`，Then 產出同版本 APK 與可驗證的 SHA-256。
- Given Git 暫存內容，When 搜尋敏感檔名，Then 不包含 `keystore.properties`、`.jks`、`.keystore` 或 `.env`。
- Given 缺少真機截圖，When 閱讀 README，Then 限制被明確揭露；授權則連結既有 MIT LICENSE。

## 風險與降低方式

| 風險 | 影響 | 可能性 | 降低方式 |
|---|---|---|---|
| GitHub v2.1.0 Release 尚不存在 | 中 | 高 | 只建立本機產物與連結，不宣稱遠端已發布 |
| 無障礙用途遭誤解 | 高 | 中 | README 與隱私文件清楚揭露使用方式及限制 |
| 真機畫面與描述不一致 | 中 | 中 | 不使用生成 UI 假裝截圖，列為 Roadmap |
| 版本引用遺漏 | 中 | 低 | 全庫搜尋舊版本並重新建置 |
| 簽章資訊外洩 | 高 | 低 | 擴充 `.gitignore` 並在 staged diff 進行敏感檔檢查 |

## 未決策事項

- 已確認遠端 repository 採用 MIT License。
- 尚未確認 v2.1.0 GitHub Release 是否由維護者建立。
- 尚未取得 v2.1.0 真機截圖。
