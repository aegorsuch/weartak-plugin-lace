package com.weartak.plugin.lace

import android.app.RemoteInput
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.input.RemoteInputIntentHelper
import com.weartak.plugin.lace.host.LacePluginRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val INPUT_KEY = "lace_input"
private const val ACTION_TRANSMIT = "lace.transmit"

private val Muted = Color(0xFFAAAAAA)
private val Panel = Color(0xFF202124)

private fun LaceStatus.color(): Color = when (this) {
    LaceStatus.GREEN -> Color(0xFF2E7D32)
    LaceStatus.YELLOW -> Color(0xFFF9A825)
    LaceStatus.RED -> Color(0xFFC62828)
    LaceStatus.BLACK -> Color(0xFF000000)
}

private val LIQUID_LABELS = mapOf(
    LaceStatus.GREEN to ">75%", LaceStatus.YELLOW to "25-75%", LaceStatus.RED to "<25%", LaceStatus.BLACK to "Out",
)
private val AMMO_LABELS = LIQUID_LABELS
private val CASUALTY_LABELS = mapOf(
    LaceStatus.GREEN to "0 WIA / KIA", LaceStatus.YELLOW to "Minor WIA", LaceStatus.RED to "Urgent WIA", LaceStatus.BLACK to "KIA",
)
private val EQUIPMENT_LABELS = mapOf(
    LaceStatus.GREEN to "100% Ready", LaceStatus.YELLOW to "Degraded", LaceStatus.RED to "Critical Gear Down", LaceStatus.BLACK to "Mission Failure",
)

private sealed interface TxState {
    data object Idle : TxState
    data object Sending : TxState
    data class Done(val ok: Boolean, val message: String) : TxState
}

/** LACE report form (Wear OS). The report is sent as WearTAK chat through WearTAK core. */
@Composable
fun LACEFragment() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf(LaceReport()) }
    var callsignEdited by remember { mutableStateOf(false) }
    var tx by remember { mutableStateOf<TxState>(TxState.Idle) }
    var confirmReset by remember { mutableStateOf(false) }

    var inputCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val input = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val text = r.data?.let { RemoteInput.getResultsFromIntent(it) }?.getCharSequence(INPUT_KEY)?.toString()
        if (text != null) inputCallback?.invoke(text.trim())
        inputCallback = null
    }
    val edit: (String, (String) -> Unit) -> Unit = { prompt, cb ->
        inputCallback = cb
        try {
            val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
            RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(RemoteInput.Builder(INPUT_KEY).setLabel(prompt).build()))
            input.launch(intent)
        } catch (_: ActivityNotFoundException) {
            inputCallback = null
        }
    }

    // Default the callsign to the WearTAK self callsign until the user edits it.
    LaunchedEffect(Unit) {
        val cs = withContext(Dispatchers.IO) { resolveCallsign() }
        if (!callsignEdited && cs != null) report = report.copy(callsign = cs)
    }

    fun update(r: LaceReport) {
        report = r
        if (tx is TxState.Done) tx = TxState.Idle
        confirmReset = false
    }

    val listState = rememberScalingLazyListState()
    Scaffold(positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScalingLazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().background(Color.Black),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item { WearTakHomeButton() }
            item {
                Text("LACE REPORT", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            item {
                FieldChip("Callsign", report.callsign.ifBlank { "Tap to set" }) {
                    edit("Callsign") { v -> if (v.isNotBlank()) { callsignEdited = true; update(report.copy(callsign = v)) } }
                }
            }

            item { SectionHeader("L", "Liquid", report.liquid, LIQUID_LABELS) }
            item { StatusSelector(report.liquid) { update(report.copy(liquid = it)) } }
            item {
                FieldChip("Needs", report.liquidNeeds.ifBlank { "e.g. Water 2L" }, placeholder = report.liquidNeeds.isBlank()) {
                    edit("Liquid needs") { update(report.copy(liquidNeeds = it)) }
                }
            }

            item { SectionHeader("A", "Ammunition", report.ammo, AMMO_LABELS) }
            item { StatusSelector(report.ammo) { update(report.copy(ammo = it)) } }
            item {
                FieldChip("Type / count", report.ammoDetails.ifBlank { "e.g. 5.56 - 4 mags" }, placeholder = report.ammoDetails.isBlank()) {
                    edit("Ammo type / count") { update(report.copy(ammoDetails = it)) }
                }
            }

            item { SectionHeader("C", "Casualty", report.casualty, CASUALTY_LABELS) }
            item { StatusSelector(report.casualty) { update(report.copy(casualty = it)) } }
            item { Stepper("Urgent WIA", report.urgentWia) { update(report.copy(urgentWia = it)) } }
            item { Stepper("Priority WIA", report.priorityWia) { update(report.copy(priorityWia = it)) } }
            item { Stepper("Routine WIA", report.routineWia) { update(report.copy(routineWia = it)) } }
            item { Stepper("KIA", report.kia) { update(report.copy(kia = it)) } }

            item { SectionHeader("E", "Equipment", report.equipment, EQUIPMENT_LABELS) }
            item { StatusSelector(report.equipment) { update(report.copy(equipment = it)) } }
            item {
                FieldChip("Remarks", report.equipmentNotes.ifBlank { "Add notes" }, placeholder = report.equipmentNotes.isBlank()) {
                    edit("Equipment remarks") { update(report.copy(equipmentNotes = it)) }
                }
            }

            item {
                Chip(
                    onClick = {
                        if (tx == TxState.Sending) return@Chip
                        if (report.callsign.isBlank()) {
                            tx = TxState.Done(false, "Set a callsign first")
                            return@Chip
                        }
                        tx = TxState.Sending
                        val snapshot = report
                        scope.launch {
                            tx = withContext(Dispatchers.IO) { transmit(snapshot) }
                        }
                    },
                    label = {
                        Text(if (tx == TxState.Sending) "SENDING…" else "TRANSMIT REPORT",
                            fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth())
                    },
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF1565C0), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(top = 6.dp),
                )
            }
            (tx as? TxState.Done)?.let { done ->
                item {
                    Text(done.message, fontSize = 12.sp, textAlign = TextAlign.Center,
                        color = if (done.ok) Color(0xFF81C784) else Color(0xFFEF9A9A), modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CompactChip(
                        onClick = {
                            if (confirmReset) {
                                report = LaceReport(callsign = report.callsign)
                                tx = TxState.Idle
                                confirmReset = false
                            } else {
                                confirmReset = true
                            }
                        },
                        label = { Text(if (confirmReset) "Tap again to reset" else "RESET") },
                        colors = ChipDefaults.chipColors(
                            backgroundColor = if (confirmReset) Color(0xFF8E2A2A) else Color(0xFF33363B),
                            contentColor = Color.White,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(letter: String, name: String, status: LaceStatus, labels: Map<LaceStatus, String>) {
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$letter · ${name.uppercase()}", fontSize = 12.sp, color = Muted, fontWeight = FontWeight.Bold)
        Text("  ${labels.getValue(status)}", fontSize = 12.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun StatusSelector(selected: LaceStatus, onSelect: (LaceStatus) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        LaceStatus.entries.forEach { s ->
            val isSel = s == selected
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(s.color())
                    .border(if (isSel) 3.dp else 1.dp, if (isSel) Color.White else Color(0xFF5F6368), CircleShape)
                    .clickable { onSelect(s) }
                    .semantics { contentDescription = s.name; this.selected = isSel },
                contentAlignment = Alignment.Center,
            ) {
                Text(s.name.first().toString(), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = if (s == LaceStatus.YELLOW) Color.Black else Color.White)
            }
        }
    }
}

@Composable
private fun Stepper(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(Panel).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", enabled = value > 0) { onChange((value - 1).coerceAtLeast(0)) }
        Text(label, fontSize = 13.sp, color = Color.White, modifier = Modifier.weight(1f).padding(start = 6.dp))
        Text("$value", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White,
            textAlign = TextAlign.Center, modifier = Modifier.width(28.dp))
        StepButton("+", enabled = value < 99) { onChange((value + 1).coerceAtMost(99)) }
    }
}

@Composable
private fun StepButton(text: String, enabled: Boolean, onClick: () -> Unit) = Button(
    onClick = onClick,
    enabled = enabled,
    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3C4043)),
    modifier = Modifier.size(32.dp),
) { Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold) }

@Composable
private fun FieldChip(label: String, value: String, placeholder: Boolean = false, onClick: () -> Unit) = Chip(
    onClick = onClick,
    label = { Text(label, fontSize = 11.sp, color = Muted) },
    secondaryLabel = {
        Text(value, fontSize = 14.sp, color = if (placeholder) Color(0xFF777777) else Color.White,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    },
    colors = ChipDefaults.chipColors(backgroundColor = Panel),
    modifier = Modifier.fillMaxWidth(),
)

@Composable
private fun WearTakHomeButton() {
    val ctx = LocalContext.current
    val host = remember {
        (listOfNotNull(HostTarget.hostPackageName) + listOf("com.aitech.weartak_civ", "com.aitech.weartak_civ.dev"))
            .firstOrNull { ctx.packageManager.getLaunchIntentForPackage(it) != null }
    } ?: return
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Button(
            onClick = {
                ctx.packageManager.getLaunchIntentForPackage(host)?.let {
                    ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                }
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF33363B)),
            modifier = Modifier.size(28.dp),
        ) {
            Icon(painterResource(R.drawable.lace_back), contentDescription = "Back to WearTAK", tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

private fun resolveCallsign(): String? =
    LacePluginRuntime.hostCallsign() ?: LacePluginRuntime.readHostTelemetry()?.callsign

/** Blocking; run off the main thread. */
private fun transmit(report: LaceReport): TxState {
    if (!LacePluginRuntime.isHostAttached()) {
        return TxState.Done(false, "WearTAK not connected. Open WearTAK and enable LACE.")
    }
    if (!LacePluginRuntime.hostSupportsChat()) {
        return TxState.Done(false, "Update WearTAK to send LACE chat. Not sent.")
    }
    val outcome = LacePluginRuntime.dispatchChat(ACTION_TRANSMIT, laceRemarks(report))
    return if (outcome.success) {
        TxState.Done(true, "Sent to All Chat Rooms")
    } else {
        TxState.Done(false, "Send failed: ${outcome.message.removePrefix("NOT_ATTACHED: ")}")
    }
}