# GitHub Release Artifact Integrity 驗證規格

## 1. 功能目標

建立一個唯讀、可重複執行的 GitHub Release artifact 驗證流程，獨立確認：

1. 本機 verified release APK 的 SHA-256。
2. 實際從 GitHub Release 下載回來的 APK SHA-256。
3. GitHub Release Asset API 回傳的 APK digest。
4. 若 Release 同時存在 `.sha256` asset，該檔案宣告的 hash。

流程不得以 API 回傳 digest 取代實際下載 APK 後的重新計算。

## 2. 使用情境

維護者在本機已完成 signed release APK 建置與本機 checksum 後，指定 repository、Release tag、APK asset 名稱與本機 APK 路徑，執行驗證工具。

成功時輸出各來源 hash、比較結果與 `PASS`，並以 exit code `0` 結束。任一已取得資料的適用條件不成立時輸出 `FAIL`，以 exit code `10` 結束；GitHub API、asset 定位或實際下載無法完成時輸出 `BLOCKED`，以 exit code `20` 結束；資料取得後因格式或驗證前置條件不足而無法形成可信結論時輸出 `NOT VERIFIED`，以 exit code `21` 結束。只有 exit code `0` 代表 PASS。

## 3. 功能需求

### FR-1 本機 hash

工具 MUST 使用 SHA-256 重新讀取指定本機 APK，不得信任既有文字結果作為 `LOCAL_SHA256`。

### FR-2 遠端下載 hash

工具 MUST 從 GitHub Release asset 的實際下載 URL 下載 APK 到暫存位置，並重新計算 `DOWNLOADED_SHA256`。下載失敗、檔案不存在或無法讀取時不得 PASS。

### FR-3 GitHub API digest

工具 MUST 透過 GitHub Release Asset API，針對指定 repository + tag 列出 Release assets，再以完整且大小寫敏感的 asset name 精確比對。指定 APK asset 必須恰好解析到一個結果；零個或多個同名結果均不得 PASS。該 asset 必須是 `uploaded`，digest MUST 嚴格符合 `^sha256:[0-9a-fA-F]{64}$`，並在比對時移除 `sha256:` 後轉為小寫。

### FR-4 必要一致性條件

只有以下兩項同時成立時，APK integrity 比對才可通過：

```text
LOCAL_SHA256 == DOWNLOADED_SHA256
DOWNLOADED_SHA256 == GITHUB_ASSET_DIGEST
```

任何一項不成立，結果 MUST NOT be `PASS`。

### FR-5 可選 `.sha256` asset

工具 MUST 以同一 repository + tag 嘗試定位與 APK asset name 加上 `.sha256` 後綴的 asset，且同樣要求精確唯一解析到指定 asset。不存在時才視為不適用；多個同名、非 `uploaded` 或 API 狀態不明時不得 PASS。

若該 asset 存在並成功下載，parser 只支援 PikClick 現有產生格式：單行、64 個十六進位字元、兩個 ASCII 空白、完整 APK 檔名，允許檔案結尾為 LF 或 CRLF；不接受 BSD 格式、只含 hash 的格式、額外欄位、額外行或不同檔名。解析後必須額外要求：

```text
SHA256_FILE_VALUE == LOCAL_SHA256
```

若 `.sha256` asset 不存在，FR-5 不適用，不得因此判定失敗；但 FR-1 至 FR-4 仍必須全部成立。

若 API 宣稱存在 `.sha256` asset 但下載、解析或比對失敗，結果 MUST NOT be `PASS`。

### FR-6 唯讀與安全性

工具 MUST NOT 建立、修改、刪除或發布 GitHub Release、tag、asset、本機 APK 或既有 checksum。每次執行 MUST 建立全新的、不可預先存在的暫存目錄與檔名；不得使用或信任舊 TEMP bytes，也不得把 TEMP 路徑指向或覆寫 local verified APK。下載完成後必須從本次新下載檔重新計算 hash。驗證結束後清理暫存檔；清理失敗不得覆蓋原始驗證結果。

工具不得輸出 token、密碼、私鑰或完整 Authorization header。

## 4. 非功能需求

- 輸出必須包含 local、downloaded、GitHub API digest 及適用時的 sidecar hash。
- 結果必須可由 exit code 判斷：`0` = PASS、`10` = FAIL、`20` = BLOCKED、`21` = NOT VERIFIED；只有 `0` 可作為成功門檻。
- 驗證邏輯應可在不連線 GitHub 的情況下用 fixture 測試。
- 不修改 APK 功能、Gradle SHA-256 產生邏輯或現有 unsigned Release workflow。

## 5. 邊界條件

- 本機 APK 不存在或不可讀。
- GitHub repository、tag 或 asset 不存在。
- asset 狀態不是 `uploaded`。
- API digest 缺少、演算法不是 SHA-256 或格式錯誤。
- 遠端下載失敗、下載內容為空或 hash 不一致。
- `.sha256` asset 不存在：條件不適用，仍可在 FR-1 至 FR-4 成立時 PASS。
- `.sha256` asset 存在但內容格式錯誤、檔名錯誤或 hash 不一致：不得 PASS。
- GitHub API、下載網路或認證環境不可用：`BLOCKED` / `NOT VERIFIED`，不得 PASS。
- 重複執行不得改變 Release 或正式產物。

## 6. 驗收標準

### AC-1 完整驗證成功

Given 本機 APK、GitHub APK asset、API digest 與 `.sha256` asset 均可取得，且所有 hash 一致；
When 執行驗證工具；
Then 輸出 `PASS`、exit code 0，且明確列出四個來源值。

### AC-2 無 sidecar 成功

Given `.sha256` asset 不存在，但本機 hash、下載 hash、API digest 一致；
When 執行驗證工具；
Then sidecar 條件標記為 not applicable，仍輸出 `PASS`、exit code 0。

### AC-3 必要 hash 不一致

Given `LOCAL_SHA256 != DOWNLOADED_SHA256` 或 `DOWNLOADED_SHA256 != GITHUB_ASSET_DIGEST`；
When 執行驗證工具；
Then 不輸出 `PASS`，exit code 非零，並指出不一致來源。

### AC-4 sidecar 不一致

Given必要兩方條件成立，但 `SHA256_FILE_VALUE != LOCAL_SHA256`；
When 執行驗證工具；
Then 不輸出 `PASS`，exit code 非零。

### AC-5 遠端不可驗證

Given API 或實際 APK 下載失敗；
When 執行驗證工具；
Then 結果為 `BLOCKED` 或 `NOT VERIFIED`，exit code 非零，不得以 local hash 或 API digest 單獨判定成功。

## 7. 最小修改範圍

預計新增：

- `tools/Verify-GitHubRelease.ps1`：命令列驗證入口與遠端流程。
- `tools/tests/Verify-GitHubRelease.Tests.ps1` 或等價 fixture 測試：三方條件、sidecar 條件與阻擋情境。

預計更新：

- `docs/LOCAL_RELEASE.md`：新增實際驗證命令與結果解讀。
- `docs/RELEASE_CHECKLIST.md`：加入遠端下載與 API digest 三方驗證門檻。

不修改：

- APK production code。
- `app/build.gradle.kts` 的 SHA-256 產生邏輯。
- 現有 `.github/workflows/release.yml`，除非實作期間證明 workflow 變更是必要且不會改變既有 unsigned release 邊界。
- 既有 v2.1.2 Release assets。

## 8. Artifact Attestation / provenance 評估

本次先不加入。現有正式 APK 由本機簽章後手動上傳，而 GitHub Actions 明確只產生 unsigned APK；直接加入 GitHub attestation 無法完整證明正式 signed APK 的來源。若未來將簽章建置移入受控 workflow，再另立設計評估 attestation、provenance、權限與簽章金鑰邊界。

## 9. 未決策事項

- 是否要求驗證工具使用 `gh` CLI，或同時支援 GitHub REST API + `GITHUB_TOKEN` / anonymous API。
- 是否將驗證工具接到既有 workflow 作為手動 verification job；本次預設不接入。

## 10. 驗證限制

在實際下載指定 Release APK 並重新計算 hash 前，遠端閉環只能標示 `BLOCKED` / `NOT VERIFIED`，不得宣稱通過。

## 11. Human-readable result semantics

The verifier MUST report the three conclusions separately:

- `GitHub Release internal integrity`: downloaded APK, API digest, and applicable remote `.sha256` agree.
- `Local-to-Release artifact identity`: local APK hash equals the freshly downloaded Release APK hash.
- `Publisher provenance`: a separate conclusion based on Release/tag metadata; hash equality alone does not establish publisher provenance.

A remote PASS with local mismatch MUST report remote internal integrity `PASS`, local-to-Release identity `FAIL`, and overall exit code `10`; it MUST NOT collapse the result into an ambiguous `Release integrity FAIL` message.