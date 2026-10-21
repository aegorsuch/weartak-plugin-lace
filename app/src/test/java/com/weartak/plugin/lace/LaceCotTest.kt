package com.weartak.plugin.lace

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class LaceCotTest {
    private val now = 1_760_000_000_000L // 2025-10-09T08:53:20Z
    private val fix = LaceFix(38.8895, -77.0353, LaceFixSource.HOST_PLI)

    private fun parse(xml: String): Element =
        DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xml.byteInputStream()).documentElement

    @Test
    fun buildsEventWithTypeHowPointAndTimes() {
        val event = parse(buildLaceCotEvent(LaceReport(callsign = "ALPHA-1"), fix, "ANDROID-abc", "WATCH-1", "msg-1", nowMs = now))
        assertEquals("event", event.tagName)
        assertEquals("b-t-f", event.getAttribute("type"))
        assertEquals("m-g", event.getAttribute("how"))
        assertEquals("GeoChat.ANDROID-abc.All Chat Rooms.msg-1", event.getAttribute("uid"))
        assertEquals("2025-10-09T08:53:20.000Z", event.getAttribute("time"))
        assertEquals("2025-10-09T08:53:20.000Z", event.getAttribute("start"))
        assertEquals("2025-10-10T08:53:20.000Z", event.getAttribute("stale"))
        val chat = event.getElementsByTagName("__chat").item(0) as Element
        assertEquals("All Chat Rooms", chat.getAttribute("chatroom"))
        assertEquals("WATCH-1", chat.getAttribute("senderCallsign"))
        val grp = event.getElementsByTagName("chatgrp").item(0) as Element
        assertEquals("ANDROID-abc", grp.getAttribute("uid0"))
        val link = event.getElementsByTagName("link").item(0) as Element
        assertEquals("ANDROID-abc", link.getAttribute("uid"))
        val remarks = event.getElementsByTagName("remarks").item(0) as Element
        assertEquals("BAO.F.ATAK.ANDROID-abc", remarks.getAttribute("source"))
        assertEquals("All Chat Rooms", remarks.getAttribute("to"))
        val point = event.getElementsByTagName("point").item(0) as Element
        assertEquals(38.8895, point.getAttribute("lat").toDouble(), 0.0)
        assertEquals(-77.0353, point.getAttribute("lon").toDouble(), 0.0)
        assertEquals(COT_UNKNOWN, point.getAttribute("hae").toDouble(), 0.0)
    }

    @Test
    fun laceDetailCarriesStatusesAndCounts() {
        val report = LaceReport(
            callsign = "ALPHA-1",
            liquid = LaceStatus.YELLOW, ammo = LaceStatus.RED, casualty = LaceStatus.RED, equipment = LaceStatus.BLACK,
            urgentWia = 2, priorityWia = 1, routineWia = 3, kia = 0,
        )
        val event = parse(buildLaceCotEvent(report, fix, "ANDROID-abc", "WATCH-1", "msg-1", nowMs = now))
        val lace = event.getElementsByTagName("lace").item(0) as Element
        assertEquals("YELLOW", lace.getAttribute("liquid"))
        assertEquals("RED", lace.getAttribute("ammo"))
        assertEquals("RED", lace.getAttribute("casualty"))
        assertEquals("BLACK", lace.getAttribute("equipment"))
        assertEquals("2", lace.getAttribute("urgent_wia"))
        assertEquals("1", lace.getAttribute("priority_wia"))
        assertEquals("3", lace.getAttribute("routine_wia"))
        assertEquals("0", lace.getAttribute("kia"))
        assertTrue(event.getElementsByTagName("remarks").item(0).textContent.startsWith("ALPHA-1 LACE:"))
    }

    @Test
    fun remarksFollowLaceFormat() {
        val report = LaceReport(
            callsign = "ALPHA-1", liquid = LaceStatus.YELLOW,
            liquidNeeds = "Water 2L", ammoDetails = "5.56 - 4 mags", equipmentNotes = "Radio down",
        )
        assertEquals(
            "ALPHA-1 LACE: L:YELLOW A:GREEN C:GREEN E:GREEN - Liquid: Water 2L; Ammo: 5.56 - 4 mags; Equipment: Radio down",
            laceRemarks(report),
        )
        assertEquals("ALPHA-1 LACE: L:GREEN A:GREEN C:GREEN E:GREEN", laceRemarks(LaceReport(callsign = "ALPHA-1")))
        assertEquals(
            "ALPHA-1 LACE: L:GREEN A:GREEN C:RED E:GREEN (WIA U:1 P:0 R:2 KIA:0)",
            laceRemarks(LaceReport(callsign = "ALPHA-1", casualty = LaceStatus.RED, urgentWia = 1, routineWia = 2)),
        )
    }

    @Test
    fun escapesUserText() {
        val report = LaceReport(callsign = "A&B \"1\"", equipmentNotes = "<b>broken</b> & 'lost'")
        val xml = buildLaceCotEvent(report, fix, "ANDROID-abc", "WATCH-1", "msg-1", nowMs = now)
        val event = parse(xml)
        val remarks = event.getElementsByTagName("remarks").item(0).textContent
        assertEquals("A&B \"1\" LACE: L:GREEN A:GREEN C:GREEN E:GREEN - Equipment: <b>broken</b> & 'lost'", remarks)
        assertFalse(xml.contains("<b>"))
    }

    @Test
    fun rejectsMissingOrNullIslandFix() {
        assertFalse(LaceFix(0.0, 0.0, LaceFixSource.DEVICE_GPS).isValid)
        assertFalse(LaceFix(Double.NaN, 1.0, LaceFixSource.DEVICE_GPS).isValid)
        assertFalse(LaceFix(91.0, 1.0, LaceFixSource.DEVICE_GPS).isValid)
        assertTrue(fix.isValid)
        val thrown = runCatching { buildLaceCotEvent(LaceReport(), LaceFix(0.0, 0.0, LaceFixSource.HOST_PLI), "u", "c", "m") }
        assertTrue(thrown.exceptionOrNull() is IllegalArgumentException)
    }
}
