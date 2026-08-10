# PikClick Hybrid H Main Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the approved Hybrid H main-screen grouping while preserving PikClick's existing behavior.

**Architecture:** Keep the programmatic View hierarchy in `MainActivity.kt`. Recompose existing permission, delay, primary-action, warning, and Donate builders into the approved vertical hierarchy, adding only narrowly named presentation resources required by the new grouping.

**Tech Stack:** Kotlin 2.0.20, Android Views, XML resources and drawables, JUnit, Gradle 8.7.

## Global Constraints

- Presentation and visual hierarchy only.
- Preserve all permission, delay, CTA, Donate, overlay, accessibility-service, countdown, drag, double-click, and close semantics.
- Keep all interactive targets at least 48dp and preserve pressed/focus feedback.
- Do not modify `dist/`, checksums, `.agents/`, version metadata, signing, or release configuration.
- Do not introduce Compose, XML layout migration, dependencies, navigation, toggles, or unrelated refactoring.
- Do not commit or push unless the user separately authorizes it.

---

### Task 1: Baseline behavior and layout map

**Files:**
- Read: `app/src/main/java/com/pikclick/app/MainActivity.kt`
- Read: `app/src/main/java/com/pikclick/app/MainUiState.kt`
- Read: `app/src/test/java/com/pikclick/app/MainUiStateTest.java`

**Interfaces:**
- Consumes: `derivePrimaryActionState(boolean, boolean)` and existing permission/delay click handlers.
- Produces: a verified list of behavior paths that the presentation edit must preserve.

- [ ] Record `git status --short`, `git diff --stat`, and the current HEAD.
- [ ] Read `MainActivity.kt` completely and map each builder to the approved visual group.
- [ ] Run `./gradlew.bat :app:test --rerun-tasks --no-daemon --max-workers=1` and record Debug, Release, total, and exit code.
- [ ] Stop before editing if the baseline has a source/test failure.

### Task 2: Permission missing-state copy

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`
- Modify: `app/src/main/java/com/pikclick/app/MainActivity.kt`
- Test: `app/src/test/java/com/pikclick/app/MainUiStateTest.java`

**Interfaces:**
- Consumes: `updatePermissionCell(...)` and existing boolean permission readiness values.
- Produces: ready labels (`permission_granted`, `service_enabled`) and missing labels (`grant_permission`, `enable_service`) selected without changing click destinations.

- [ ] Add localized missing-state strings with exact meanings `去授權` / `Grant permission` and `去啟用` / `Enable service`.
- [ ] Keep the existing ready labels unchanged.
- [ ] Update only status presentation selection in `updatePermissionCell`; do not change permission checks or click listeners.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests com.pikclick.app.MainUiStateTest --rerun-tasks --no-daemon --max-workers=1`.

### Task 3: Hybrid H view hierarchy

**Files:**
- Modify: `app/src/main/java/com/pikclick/app/MainActivity.kt`
- Modify: `app/src/main/res/values/dimens.xml`
- Modify if needed: `app/src/main/res/drawable/button_secondary_background.xml`

**Interfaces:**
- Consumes: existing `createPermissionCell`, `createDelayCell`, `mainActionButton`, `createDonateSection`, and current handlers.
- Produces: permission-row, delay-plus-action card, external warning, and footer Donate groups.

- [ ] Replace the current delay/CTA side-by-side row with a vertical control card.
- [ ] Keep the delay label, editable `EditText`, and inline `delayError` in the same logical field group.
- [ ] Place `primaryActionButton` below the delay field using match-width layout params and the existing minimum height.
- [ ] Preserve permission cells in a two-column row and keep each card's listener/content description.
- [ ] Move the warning block immediately below the main control card.
- [ ] Restyle Donate as a bottom low-emphasis text action while preserving its listener and content description.
- [ ] Add only dimension names that express stable Hybrid H roles; reuse the 4/8/12/16/24dp scale for spacing.
- [ ] Run `git diff --check`.

### Task 4: Functional-preservation review

**Files:**
- Review: `app/src/main/java/com/pikclick/app/MainActivity.kt`
- Review: `app/src/main/java/com/pikclick/app/MainUiState.kt`
- Review: `app/src/main/java/com/pikclick/app/OverlayBubbleService.kt`

**Interfaces:**
- Consumes: the completed presentation diff.
- Produces: evidence that no functional path changed.

- [ ] Compare the modified CTA listener with HEAD and verify all three action branches are unchanged.
- [ ] Compare delay validation, persistence, and error restoration paths with HEAD.
- [ ] Verify permission settings destinations and Donate URI are unchanged.
- [ ] Verify there is no diff in `OverlayBubbleService.kt` or accessibility-service production code.
- [ ] Verify no absolute paths, debug code, generated mockups, or temporary probes entered production files.

### Task 5: Automated verification

**Files:**
- Test: existing JVM suites
- Build: existing debug variant

**Interfaces:**
- Consumes: the complete scoped diff.
- Produces: test, build, and whitespace evidence.

- [ ] Run `./gradlew.bat :app:test --rerun-tasks --no-daemon --max-workers=1`; record Debug, Release, total, failures, errors, skipped, and exit code.
- [ ] Run `./gradlew.bat :app:assembleDebug --no-daemon --max-workers=1`; record exit code.
- [ ] Run `git diff --check`.
- [ ] Confirm `git status --short` contains no new changes outside approved source/resource/spec/plan files.

### Task 6: Device and independent review

**Files:**
- Review: approved spec, plan, production diff, and test evidence

**Interfaces:**
- Consumes: automated verification and an available connected device.
- Produces: final severity-ranked delivery verdict.

- [ ] If a device is available, install only an appropriate debug build without clearing app data and perform a targeted main-screen smoke.
- [ ] Verify permission ready/missing presentation, delay normal/error layout, all three reachable CTA labels, Donate hierarchy, clipping, and pressed feedback.
- [ ] Report TalkBack and larger-font checks as PASS, FAIL, or NOT RUN based only on actual execution.
- [ ] Classify findings Critical, High, Medium, and Low; fix only confirmed Critical/High regressions introduced by this change unless the user expands scope.
- [ ] Report modified files, test/build evidence, device evidence, remaining verification gaps, and final verdict.
