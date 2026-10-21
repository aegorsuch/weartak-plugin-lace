package com.weartak.plugin.lace

import org.junit.Assert.assertEquals
import org.junit.Test

class LaceReportTest {
    @Test
    fun remarksFollowLaceFormat() {
        val report = LaceReport(
            liquid = LaceStatus.YELLOW,
            liquidNeeds = "Water 2L", ammoDetails = "5.56 - 4 mags", equipmentNotes = "Radio down",
        )
        assertEquals(
            "LACE: L:YELLOW A:GREEN C:GREEN E:GREEN - Liquid: Water 2L; Ammo: 5.56 - 4 mags; Equipment: Radio down",
            laceRemarks(report),
        )
        assertEquals("LACE: L:GREEN A:GREEN C:GREEN E:GREEN", laceRemarks(LaceReport()))
        assertEquals(
            "LACE: L:GREEN A:GREEN C:RED E:GREEN (WIA U:1 P:0 R:2 KIA:0)",
            laceRemarks(LaceReport(casualty = LaceStatus.RED, urgentWia = 1, routineWia = 2)),
        )
    }

    @Test
    fun blankNotesAreSkippedAndTrimmed() {
        val report = LaceReport(ammoDetails = "  ", equipmentNotes = "  NVG lost  ")
        assertEquals("LACE: L:GREEN A:GREEN C:GREEN E:GREEN - Equipment: NVG lost", laceRemarks(report))
    }
}