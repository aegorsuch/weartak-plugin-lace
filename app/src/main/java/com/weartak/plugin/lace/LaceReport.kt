package com.weartak.plugin.lace

enum class LaceStatus(val meaning: String) {
    GREEN("Good"),
    YELLOW("Degraded"),
    RED("Critical"),
    BLACK("Out"),
}

data class LaceReport(
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
    /** Free-text part of the message: every non-blank detail field, in L-A-C-E order. */
    fun notes(): String = listOf(
        "Liquid" to liquidNeeds,
        "Ammo" to ammoDetails,
        "Equipment" to equipmentNotes,
    ).filter { it.second.isNotBlank() }.joinToString("; ") { "${it.first}: ${it.second.trim()}" }
}

/** LACE chat text; WearTAK supplies the sender's UID and callsign. */
fun laceRemarks(report: LaceReport): String {
    val head = "LACE: L:${report.liquid} A:${report.ammo} C:${report.casualty} E:${report.equipment}"
    val counts = with(report) { if (urgentWia + priorityWia + routineWia + kia > 0) " (WIA U:$urgentWia P:$priorityWia R:$routineWia KIA:$kia)" else "" }
    val notes = report.notes()
    return if (notes.isBlank()) head + counts else "$head$counts - $notes"
}