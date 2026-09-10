package com.emre.gunumplanner

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class SmartPlanReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Db(this)
        FullRepository.ensureSchema(db)
        setContent { V2Theme { SmartPlanReviewScreen(this, db) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartPlanReviewScreen(activity: Activity, db: Db) {
    val plans = remember { SmartPlanner.propose(db.getOpenTasksUpTo(LocalDate.now().toString()), LocalDate.now()) }
    var selected by remember { mutableStateOf(plans.map { it.itemId }.toSet()) }
    val fmt = remember { DateTimeFormatter.ofPattern("d MMM HH:mm", Locale("tr", "TR")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text("Akıllı plan", fontWeight = FontWeight.Bold); Text("Uygulamadan önce kontrol et", style = MaterialTheme.typography.bodySmall) } },
                navigationIcon = { IconButton({ activity.finish() }) { Icon(Icons.Rounded.ArrowBack, "Geri") } }
            )
        }
    ) { p ->
        LazyColumn(
            Modifier.padding(p).fillMaxSize(),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column { Text("${plans.size} görev için öneri", fontWeight = FontWeight.Bold); Text("İstemediğin görevlerin işaretini kaldırabilirsin.", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            if (plans.isEmpty()) item { Text("Şu an yeniden planlanacak gecikmiş veya saatsiz görev yok.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)) }
            items(plans, key = { it.itemId }) { plan ->
                val checked = plan.itemId in selected
                Card(shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked, { isChecked -> selected = if (isChecked) selected + plan.itemId else selected - plan.itemId })
                        Column(Modifier.weight(1f)) {
                            Text(plan.title, fontWeight = FontWeight.SemiBold)
                            val dt = Instant.ofEpochMilli(plan.newDueAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
                            Text("${dt.format(fmt)} • ${plan.durationMinutes} dk", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (plans.isNotEmpty()) item {
                Button(
                    onClick = {
                        plans.filter { it.itemId in selected }.forEach { plan ->
                            ReminderScheduler.cancel(activity, plan.itemId)
                            db.rescheduleItem(plan.itemId, plan.newDayKey, plan.newDueAt)
                            ReminderScheduler.schedule(activity, plan.itemId, plan.newDueAt)
                        }
                        GunumWidgetProvider.refreshAll(activity)
                        activity.finish()
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    enabled = selected.isNotEmpty(),
                    shape = RoundedCornerShape(18.dp)
                ) { Icon(Icons.Rounded.Check, null); Spacer(Modifier.width(8.dp)); Text("Seçilen planı uygula", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
