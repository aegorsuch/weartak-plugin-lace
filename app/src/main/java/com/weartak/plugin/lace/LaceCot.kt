package com.weartak.plugin.lace

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class LaceStatus(val meaning: String) {
    GREEN("Good"),
    YELLOW("Degraded"),
    RED("Critical"),
    BLACK("Out"),
}

data class LaceReport(
    val callsign: String = "",
    val liquid: LaceStatus = LaceStatus.GREEN,
    val liquidNeeds: String = "",
    val ammo: LaceStatus = LaceStatus.GREEN,
    val ammoDetails: String = "",
    val casualty: LaceStatus = LaceStatus.GREEN,
    val urgentWia: Int = 0,
    val priorityWia: Int = 0,
    val routineWia: Int = 0,
    val kia: Int = 0,
    val equipment: LaceStatus = LaceStatus.GREEN,
    val equipmentNotes: String = "",
) {
    /** Free-text part of the remarks: every non-blank detail field, in L-A-C-E order. */
    fun notes(): String = listOf(
        "Liquid" to liquidNeeds,
        "Ammo" to ammoDetails,
        "Equipment" to equipmentNotes,
    ).filter { it.second.isNotBlank() }.joinToString("; ") { "${it.first}: ${it.second.trim()}" }
}

enum class LaceFixSource { HOST_PLI, DEVICE_GPS }

/** Report position. HAE/CE/LE default to the CoT "unknown" value. */
data class LaceFix(
    val lat: Double,
    val lon: Double,
    val source: LaceFixSource,
    val hae: Double = COT_UNKNOWN,
    val ce: Double = COT_UNKNOWN,
    val le: Double = COT_UNKNOWN,
) {
    val isValid: Boolean
        get() = lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0 &&
            !(lat == 0.0 && lon == 0.0)
}

const val COT_UNKNOWN = 9999999.0
const val LACE_COT_TYPE = "b-t-f"
const val LACE_COT_HOW = "m-g"
const val LACE_CHATROOM = "All Chat Rooms"
const val LACE_STALE_MS = 24 * 60 * 60 * 1000L

fun laceRemarks(report: LaceReport): String {
    val head = "${report.callsign.trim()} LACE: L:${report.liquid} A:${report.ammo} C:${report.casualty} E:${report.equipment}"
    val counts = with(report) { if (urgentWia + priorityWia + routineWia + kia > 0) " (WIA U:$urgentWia P:$priorityWia R:$routineWia KIA:$kia)" else "" }
    val notes = report.notes()
    return if (notes.isBlank()) head + counts else "$head$counts - $notes"
}

/**
 * Builds the LACE report as a GeoChat message to All Chat Rooms (readable on every TAK client),
 * with the structured `<lace/>` detail attached for tools that parse it. Handed to WearTAK core for dispatch.
 */
fun buildLaceCotEvent(
    report: LaceReport,
    fix: LaceFix,
    senderUid: String,
    senderCallsign: String,
    messageId: String,
    nowMs: Long = System.currentTimeMillis(),
    staleMs: Long = LACE_STALE_MS,
): String {
    require(fix.isValid) { "LACE report needs a valid position" }
    val time = cotTime(nowMs)
    val callsign = esc(senderCallsign.trim().ifBlank { report.callsign.trim() })
    val sender = esc(senderUid)
    val room = LACE_CHATROOM
    return buildString {
        append("<event version=\"2.0\" uid=\"GeoChat.").append(sender).append('.').append(room).append('.').append(esc(messageId))
        append("\" type=\"").append(LACE_COT_TYPE)
        append("\" how=\"").append(LACE_COT_HOW)
        append("\" time=\"").append(time).append("\" start=\"").append(time)
        append("\" stale=\"").append(cotTime(nowMs + staleMs)).append("\">")
        append("<point lat=\"").append(fix.lat).append("\" lon=\"").append(fix.lon)
        append("\" hae=\"").append(fix.hae).append("\" ce=\"").append(fix.ce)
        append("\" le=\"").append(fix.le).append("\"/>")
        append("<detail>")
        append("<__chat parent=\"RootContactGroup\" groupOwner=\"false\" messageId=\"").append(esc(messageId))
        append("\" chatroom=\"").append(room).append("\" id=\"").append(room)
        append("\" senderCallsign=\"").append(callsign).append("\">")
        append("<chatgrp uid0=\"").append(sender).append("\" uid1=\"").append(room).append("\" id=\"").append(room).append("\"/>")
        append("</__chat>")
        append("<link uid=\"").append(sender).append("\" type=\"a-f-G-U-C\" relation=\"p-p\"/>")
        append("<remarks source=\"BAO.F.ATAK.").append(sender).append("\" to=\"").append(room)
        append("\" time=\"").append(time).append("\">").append(esc(laceRemarks(report))).append("</remarks>")
        append("<lace liquid=\"").append(report.liquid)
        append("\" ammo=\"").append(report.ammo)
        append("\" casualty=\"").append(report.casualty)
        append("\" equipment=\"").append(report.equipment)
        append("\" urgent_wia=\"").append(report.urgentWia.coerceAtLeast(0))
        append("\" priority_wia=\"").append(report.priorityWia.coerceAtLeast(0))
        append("\" routine_wia=\"").append(report.routineWia.coerceAtLeast(0))
        append("\" kia=\"").append(report.kia.coerceAtLeast(0)).append("\"/>")
        append("</detail>")
        append("</event>")
    }
}
private fun cotTime(ms: Long): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(ms))

private fun esc(s: String): String = buildString(s.length) {
    for (c in s) when (c) {
        '&' -> append("&amp;")
        '<' -> append("&lt;")
        '>' -> append("&gt;")
        '"' -> append("&quot;")
        '\'' -> append("&apos;")
        else -> if (c >= ' ' || c == '\t') append(c) else append(' ')
    }
}
