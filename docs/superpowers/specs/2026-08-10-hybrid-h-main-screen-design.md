# PikClick Hybrid H Main Screen Design

## Goal

Restructure the existing PikClick main screen around the approved Hybrid H concept while preserving all functional semantics. The resulting scan order is app identity, permission status, delay and primary action, safety guidance, then Donate.

## Scope

- Keep the programmatic Android View implementation in `MainActivity.kt`.
- Keep the two permission controls side by side at the top of the setup content.
- Combine the delay input and state-aware primary CTA in one large control card.
- Move the safety message below the main control card.
- Place Donate at the bottom as a low-emphasis text action.
- Reuse the existing semantic colors, spacing scale, pressed/focus resources, icons, and accessibility semantics where practical.

## Permission states

Each permission card remains independently clickable and keeps its current destination.

- Granted overlay permission: green check treatment and `已授權` / `Granted` status.
- Enabled accessibility service: green check treatment and `已啟用` / `Enabled` status.
- Missing overlay permission: neutral gray treatment and `去授權` / `Grant permission` status.
- Missing accessibility service: neutral gray treatment and `去啟用` / `Enable service` status.

Ready and missing states must use both text and visual treatment; color alone is insufficient.

## Delay and primary action card

- The delay label remains visible above the input.
- The numeric value receives the strongest visual emphasis within the card, with `秒` / `seconds` presented as its unit.
- The existing editable numeric input, accepted range, persistence, keyboard behavior, and restoration behavior remain unchanged.
- Invalid input continues to show an inline error adjacent to the field and restores the saved value according to current behavior.
- The full-width primary CTA sits at the bottom of the same card.
- CTA labels and actions remain state-aware: enable overlay permission, enable accessibility service, or show the floating button.

## Safety and Donate

- The existing safety disclosure remains visible below the main control card and retains its wording.
- Donate remains below the safety disclosure as a secondary text action.
- Donate retains its existing content description, URL, click behavior, and minimum touch target.

## Accessibility and layout

- Preserve logical focus order matching visual order.
- Keep every interactive target at least 48dp.
- Preserve visible pressed and focus feedback without layout movement.
- Use wrapping and vertical expansion instead of clipping for translated text and larger font scales.
- Keep the screen scrollable on small devices and when the keyboard is visible.

## Functional invariants

The change must not alter permission detection, permission destinations, primary CTA state derivation, delay validation or persistence, overlay service launch, floating bubble behavior, accessibility-service behavior, Donate destination, or any release artifact.

## Acceptance criteria

- Given either permission is missing, its card uses neutral missing-state treatment and an actionable missing-state label.
- Given a permission is ready, its card uses the existing green ready-state treatment and ready-state label.
- Given each primary CTA state, its label and click action remain mapped to the existing `PrimaryActionState` behavior.
- Given a valid delay, submitting it persists the same value and clears any inline error.
- Given an invalid delay, the existing range remains enforced and the inline error remains visible near the input.
- The main screen visually matches Hybrid H's grouping: two permission cards, one delay-plus-CTA card, external safety guidance, bottom Donate action.
- Existing JVM tests pass, debug assembly succeeds, and `git diff --check` passes.
- Device visual smoke, TalkBack, and larger-font verification are reported separately and never inferred.

## Exclusions

- No Compose or XML layout migration.
- No new dependency, navigation, feature, toggle, or delay control behavior.
- No changes to overlay geometry or service behavior.
- No changes to `dist/`, checksum files, `.agents/`, version metadata, signing, or release configuration.
- No unrelated typography or design-system cleanup.
