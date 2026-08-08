# PikClick Exercise 03 Visual Hierarchy Design

## Goal

Polish the existing Android main screen and overlay presentation so permission status, delay input, primary action, Donate, typography, spacing, surfaces, focus, pressed, and inline-error states form a clear visual hierarchy without changing functional semantics.

## Constraints

- Preserve permission detection, destinations, state transitions, delay validation and persistence, overlay launch, drag, countdown, double-click, close, accessibility-service behavior, and Donate action.
- Do not add Compose or Material dependencies.
- Do not modify release artifacts, checksums, `.agents/`, or unrelated user changes.
- No commit or push in this exercise.
- Critical or High regressions block delivery. Medium findings are reported only unless explicitly authorized.

## Design

Use the current programmatic Android View layout and existing drawable resources. Centralize only presentation tokens already introduced by Exercise 02: screen insets, spacing, semantic colors, surface/border states, and pressed-state resources. Keep existing click listeners and state derivation intact.

The permission cards remain individually actionable, but their label/status/icon grouping becomes more consistent. The primary action remains a single action whose label follows the existing prerequisite state. The delay field keeps the same accepted range and persistence; its error moves from Toast-only feedback to an inline visual message while restoring the saved value as before.

The Donate area remains secondary and retains its existing URL/action. Primary buttons use minimum height and padding instead of relying only on fixed text height. Focus and pressed feedback must be visible without layout shifts. Overlay accessibility affordances already present in the working tree are preserved and only visually adjusted if required by the audit.

## Acceptance criteria

- Given missing overlay permission, the primary action still opens overlay settings.
- Given overlay permission but missing accessibility permission, the primary action still opens the accessibility disclosure/destination.
- Given both permissions, the primary action still validates delay and starts the overlay service exactly as before.
- Given an invalid delay, the same validation range and persistence behavior remain unchanged; the user sees an inline error and the saved value is restored.
- Permission cards, delay control, primary CTA, warning, and Donate have consistent spacing, semantic colors, borders, and readable hierarchy.
- Primary, permission, delay, and Donate controls expose visible pressed/focus feedback without changing layout bounds.
- Existing overlay launch, drag, countdown, double-click, and close behavior remains regression-free.
- JVM tests, debug build, and `git diff --check` pass.
- No Critical or High review findings remain; Medium findings are documented only.

## Verification limits

ADB/device visual smoke, TalkBack, and largest font-scale verification are separate evidence categories. If unavailable, report them as not run or pending rather than inferring success.
