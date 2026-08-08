# Exercise 03 Visual Hierarchy Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve PikClick's existing Android presentation hierarchy while preserving all functional semantics.

**Architecture:** Keep the programmatic View-based `MainActivity` and existing drawable/resource architecture. Refine presentation tokens, component spacing, typography, and interaction-state visuals in place; do not introduce Compose, new navigation, or behavior abstractions.

**Tech Stack:** Kotlin, Android Views, XML drawables/resources, JUnit JVM tests, Gradle 8.7 wrapper.

## Global Constraints

- Presentation/visual hierarchy only.
- Preserve permission, delay, overlay, accessibility-service, countdown, drag, double-click, close, and Donate semantics.
- Critical/High regressions block delivery; Medium findings are report-only.
- Do not modify `dist/`, checksums, `.agents/`, or unrelated working-tree changes.
- Do not commit or push.

---

### Task 1: Baseline and regression evidence

**Files:**
- Read: `app/src/main/java/com/pikclick/app/MainActivity.kt`
- Read: `app/src/main/java/com/pikclick/app/OverlayBubbleService.kt`
- Read: `app/src/test/java/com/pikclick/app/ClickPolicyTest.java`
- Read: `app/src/test/java/com/pikclick/app/MainUiStateTest.java`

- [ ] Record starting status, HEAD, and current diff summary.
- [ ] Run `./gradlew.bat :app:test --rerun-tasks --no-daemon --max-workers=1` and record exit code and totals.
- [ ] Confirm no production behavior changes are needed before visual edits.

### Task 2: Presentation resource refinement

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values/dimens.xml`
- Modify: `app/src/main/res/drawable/button_secondary_background.xml`
- Modify: `app/src/main/res/drawable/main_action_background.xml`
- Modify: `app/src/main/res/drawable/status_missing_background.xml`
- Modify: `app/src/main/res/drawable/status_ready_background.xml`
- Modify: `app/src/main/res/drawable/warning_background.xml`

- [ ] Reuse semantic tokens and 4/8dp spacing rhythm.
- [ ] Ensure normal, pressed, ready, missing, and warning surfaces differ through color/border/elevation without changing click behavior.
- [ ] Keep resource changes presentation-only and avoid duplicate state semantics.
- [ ] Run `git diff --check`.

### Task 3: Main screen hierarchy polish

**Files:**
- Modify: `app/src/main/java/com/pikclick/app/MainActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`

- [ ] Preserve the existing permission state machine and listeners.
- [ ] Refine permission card grouping, label/status hierarchy, delay field boundary, helper/error spacing, CTA minimum height/padding, Donate secondary weight, and focus/pressed feedback.
- [ ] Keep delay range, restoration, persistence, and service-start paths unchanged.
- [ ] Keep Donate URL and intent unchanged.
- [ ] Add or update only presentation-oriented resource strings needed for visible labels/errors.

### Task 4: Test and build verification

**Files:**
- Read: all changed files
- Test: existing JVM tests

- [ ] Run focused JVM tests for `ClickPolicyTest` and `MainUiStateTest`.
- [ ] Run full `./gradlew.bat :app:test --rerun-tasks --no-daemon --max-workers=1`.
- [ ] Run `./gradlew.bat :app:assembleDebug --no-daemon --max-workers=1`.
- [ ] Run `git diff --check`.
- [ ] If any Critical/High regression appears, stop delivery, diagnose, add a regression test where applicable, and fix before continuing.

### Task 5: Independent review and handoff

**Files:**
- Read: original request, design, plan, diff, tests
- Modify only if required: presentation files from Tasks 2-3

- [ ] Review functional preservation independently from implementation reasoning.
- [ ] Classify findings Critical/High/Medium/Low.
- [ ] Do not automatically expand Medium findings.
- [ ] Report ADB, TalkBack, and font-scale evidence separately; do not claim device coverage without execution.
- [ ] Report status, diff stat, tests, build, review, remaining risks, and final verdict.
