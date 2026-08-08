package com.pikclick.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MainUiStateTest {
    @Test public void primaryActionGuidesOverlayPermissionFirst() {
        assertEquals(PrimaryActionState.ENABLE_OVERLAY,
            PrimaryActionState.from(false, false));
    }

    @Test public void primaryActionGuidesAccessibilityAfterOverlay() {
        assertEquals(PrimaryActionState.ENABLE_ACCESSIBILITY,
            PrimaryActionState.from(true, false));
    }

    @Test public void primaryActionIsReadyWhenBothPrerequisitesAreEnabled() {
        assertEquals(PrimaryActionState.SHOW_BUTTON,
            PrimaryActionState.from(true, true));
    }
}
