package com.jumbo.trus.service.appnotice;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppVersionComparatorTest {

    @Test
    void matchesInclusiveVersionRange() {
        assertTrue(AppVersionComparator.isWithinRange("9.0.1", "9.0.1", "9.0.1"));
        assertTrue(AppVersionComparator.isWithinRange("9.2", "9.0.1", "10.0.0"));
        assertTrue(AppVersionComparator.isWithinRange("v9.0.1+53", "9", null));
    }

    @Test
    void rejectsVersionOutsideRangeOrMalformedVersion() {
        assertFalse(AppVersionComparator.isWithinRange("8.9.9", "9.0.0", null));
        assertFalse(AppVersionComparator.isWithinRange("10.0.1", null, "10.0.0"));
        assertFalse(AppVersionComparator.isWithinRange("unknown", null, null));
        assertFalse(AppVersionComparator.isWithinRange("9.0.1", "latest", null));
    }

    @Test
    void comparesPreReleaseAccordingToSemanticVersionRules() {
        assertTrue(AppVersionComparator.isWithinRange("9.0.1-beta.2", "9.0.1-beta.1", "9.0.1"));
        assertFalse(AppVersionComparator.isWithinRange("9.0.1-beta.1", "9.0.1", null));
    }
}
