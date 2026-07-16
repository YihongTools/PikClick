package com.pikclick.app;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClickPolicyTest {
    @Test public void acceptsBoundaryDelays() {
        assertTrue(ClickPolicy.INSTANCE.isValidDelay(3f));
        assertTrue(ClickPolicy.INSTANCE.isValidDelay(10f));
    }

    @Test public void rejectsOutOfRangeAndNonFiniteDelays() {
        assertFalse(ClickPolicy.INSTANCE.isValidDelay(2.9f));
        assertFalse(ClickPolicy.INSTANCE.isValidDelay(10.1f));
        assertFalse(ClickPolicy.INSTANCE.isValidDelay(Float.NaN));
    }

    @Test public void normalizesUnsafeDelays() {
        assertEquals(3f, ClickPolicy.INSTANCE.safeDelay(-1f), 0f);
        assertEquals(10f, ClickPolicy.INSTANCE.safeDelay(99f), 0f);
        assertEquals(3.5f, ClickPolicy.INSTANCE.safeDelay(Float.NaN), 0f);
    }

    @Test public void newerSequenceInvalidatesOlderCallbacks() {
        ClickSequenceGate gate = new ClickSequenceGate();
        int first = gate.begin();
        int second = gate.begin();
        assertFalse(gate.isCurrent(first));
        assertTrue(gate.isCurrent(second));
        gate.invalidate();
        assertFalse(gate.isCurrent(second));
    }
}
