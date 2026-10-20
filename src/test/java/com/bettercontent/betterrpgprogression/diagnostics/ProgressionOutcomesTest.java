package com.bettercontent.betterrpgprogression.diagnostics;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

final class ProgressionOutcomesTest {
    @Test void rejectedPolicyAndNoDeltaAreNotReportedAsSpent() {
        assertEquals("rejected", ProgressionOutcomes.manual(false, 4));
        assertEquals("unchanged", ProgressionOutcomes.manual(true, 0));
        assertEquals("committed", ProgressionOutcomes.manual(true, 2));
        assertEquals("unknown", ProgressionOutcomes.manual(true, -1));
    }
    @Test void enqueueBooleanIsNotItemDeliveryOrASecondSuccessfulGrant() {
        assertEquals("enqueued", ProgressionOutcomes.entitlement(true));
        assertEquals("unchanged", ProgressionOutcomes.entitlement(false));
        assertNotEquals("delivered", ProgressionOutcomes.entitlement(true));
    }
}
