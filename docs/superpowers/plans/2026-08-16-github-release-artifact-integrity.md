# GitHub Release Artifact Integrity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Add a read-only verifier that proves local APK, freshly downloaded GitHub Release APK, GitHub Asset API digest, and applicable `.sha256` sidecar agree.

**Architecture:** Keep Gradle and existing release workflow unchanged. Add a focused PowerShell verifier with pure validation helpers and an injectable transport boundary so fixture tests can prove exit semantics without network access; the real command uses GitHub API metadata and fresh temporary downloads.

**Tech Stack:** PowerShell, GitHub REST API, `Get-FileHash`, existing PikClick Gradle/PowerShell environment.

## Global Constraints

- `0` = PASS; `10` = FAIL; `20` = BLOCKED; `21` = NOT VERIFIED.
- PASS requires `LOCAL_SHA256 == DOWNLOADED_SHA256` and `DOWNLOADED_SHA256 == GITHUB_ASSET_DIGEST`.
- If the exact `.sha256` asset exists, also require `SHA256_FILE_VALUE == LOCAL_SHA256`.
- API digest must strictly match `sha256:<64 hex>`.
- APK and sidecar asset lookup must use repository + tag + exact case-sensitive asset name and resolve exactly one uploaded asset.
- Every run must use a new temporary directory and never overwrite or reuse local APK or stale temporary bytes.
- No APK production changes, Release mutation, v2.1.2 asset changes, commit, push, or Release publication.

### Task 1: Add RED fixture tests for validation contracts

**Files:**
- Create: `tools/tests/Verify-GitHubRelease.Tests.ps1`
- Test target: `tools/Verify-GitHubRelease.ps1`

- [ ] Write tests for strict digest parsing, exact PikClick sidecar format, unique uploaded asset selection, and result exit codes.
- [ ] Include passing fixture with all three hashes equal and sidecar equal.
- [ ] Include passing fixture with no sidecar.
- [ ] Include FAIL fixtures for each required comparison mismatch and sidecar mismatch.
- [ ] Include BLOCKED/NOT VERIFIED fixtures for missing asset, duplicate asset, non-uploaded asset, malformed digest, download failure, and stale temp prevention.
- [ ] Run the test file before the implementation; expected failure is missing verifier/helper, not a test infrastructure error.

### Task 2: Implement minimal pure validation helpers

**Files:**
- Create: `tools/Verify-GitHubRelease.ps1`

- [ ] Implement strict `sha256:<64 hex>` normalization.
- [ ] Implement exact parser for `<64 hex><two ASCII spaces><APK basename>` with LF/CRLF only.
- [ ] Implement exact unique asset selection requiring name equality and `state = uploaded`.
- [ ] Implement comparison result mapping to PASS/FAIL/BLOCKED/NOT VERIFIED and exit codes 0/10/20/21.
- [ ] Run focused fixture tests and confirm they pass.

### Task 3: Implement read-only GitHub and fresh-download boundary

**Files:**
- Modify: `tools/Verify-GitHubRelease.ps1`

- [ ] Accept repository, tag, asset name, local APK path, and optional token input without printing secrets.
- [ ] Query the Release assets for the requested repository and tag.
- [ ] Resolve APK and optional sidecar assets exactly once.
- [ ] Create a new unique temporary directory per invocation; fail closed if creation or download fails.
- [ ] Download the APK and optional sidecar into that directory, calculate hashes from those new files, and never write to the local APK.
- [ ] Emit machine-readable enough labels for LOCAL_SHA256, DOWNLOADED_SHA256, GITHUB_ASSET_DIGEST, optional SHA256_FILE_VALUE, status, and reason.
- [ ] Run focused tests again.

### Task 4: Documentation and release checklist

**Files:**
- Modify: `docs/LOCAL_RELEASE.md`
- Modify: `docs/RELEASE_CHECKLIST.md`

- [ ] Document the exact command for a signed local APK and Release tag.
- [ ] Document that API digest alone is insufficient and that the APK must be freshly downloaded.
- [ ] Document exit codes and BLOCKED/NOT VERIFIED handling.
- [ ] Document that `.sha256` is conditional and must use the existing two-space PikClick format.

### Task 5: Integration verification and independent review

**Files:**
- Inspect: all changed files and `.github/workflows/release.yml`

- [ ] Run PowerShell parser/lint validation and focused fixture tests.
- [ ] Run existing Gradle checksum-related verification without changing release artifacts.
- [ ] Run the real GitHub Release verifier for the specified Release only if network/API access is available; otherwise report BLOCKED/NOT VERIFIED.
- [ ] Record LOCAL_SHA256, DOWNLOADED_SHA256, and GITHUB_ASSET_DIGEST separately; never infer unavailable values.
- [ ] Run `git diff --check` and inspect the complete diff from starting HEAD.
- [ ] Confirm no APK functionality, signing material, workflow, Release asset, or unrelated dirty file changed.
- [ ] Report whether workflow modification or re-release is required; default is no.

## Completion evidence

The work is complete only when focused tests pass, integration results are reported with actual exit codes, the real remote comparison is either PASS with all three values or explicitly BLOCKED/NOT VERIFIED, and independent diff review finds no Critical/High issue.