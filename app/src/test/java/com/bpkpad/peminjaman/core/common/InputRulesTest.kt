package com.bpkpad.peminjaman.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputRulesTest {

    @Test
    fun applicantName_acceptsRelevantPunctuationAndRejectsDigits() {
        assertNull(InputRules.validateApplicantName("Siti Aminah, S.AP"))
        assertFalse(InputRules.applicantNameRegex.matches("Budi123"))
    }

    @Test
    fun phone_filterKeepsOnlyFifteenDigits() {
        assertEquals(
            "081234567890123",
            InputRules.filterPhone("0812-3456-7890-12345")
        )
        assertTrue(InputRules.phoneRegex.matches("081234567890"))
        assertFalse(InputRules.phoneRegex.matches("08123"))
    }

    @Test
    fun letterNumber_supportsCommonGovernmentLetterFormat() {
        assertNull(InputRules.validateLetterNumber("900/123/BPKPAD/VI/2026"))
    }

    @Test
    fun bypassNote_isLimitedToFiveHundredCharacters() {
        assertEquals(
            InputRules.BYPASS_NOTE_MAX,
            InputRules.filterBypassNote("a".repeat(600)).length
        )
    }

    @Test
    fun extensionReason_isLimitedToFiveHundredCharacters() {
        assertEquals(
            InputRules.EXTENSION_REASON_MAX,
            InputRules.filterExtensionReason("a".repeat(600)).length
        )
        assertNull(InputRules.validateExtensionReason("Diperlukan untuk pemeriksaan lanjutan"))
    }

    @Test
    fun search_filterRemovesUnsafeControlCharacters() {
        assertEquals(
            "SP2D-2026/001",
            InputRules.filterDocumentSearch("SP2D-2026/001\u0000")
        )
    }

    @Test
    fun agencyFields_applyContextSpecificRules() {
        assertNull(InputRules.validateAgencyName("Badan Keuangan Daerah (BPKPAD)"))
        assertEquals("BPKPAD-01", InputRules.filterAgencyCode("bpkpad-01"))
        assertNull(InputRules.validateAgencyCode("BPKPAD-01"))
        assertNull(
            InputRules.validateAgencyAddress(
                "Jl. A. Yani No. 1, Paringin, Kabupaten Balangan"
            )
        )
        assertFalse(InputRules.agencyCodeRegex.matches("BPKPAD 01"))
    }
}
