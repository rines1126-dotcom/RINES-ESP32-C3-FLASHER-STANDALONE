
package com.rines.esp32c3flasher.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rines.esp32c3flasher.firmware.FlashLog

@Composable
fun FlashLogsCard(
    logs: List<FlashLog>,
    onClearLogs: () -> Unit,
    onExportLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D141D)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "FLASH LOGS",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF00D9FF)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onClearLogs) {
                    Text("CLEAR")
                }

                TextButton(onClick = onExportLogs) {
                    Text("EXPORT")
                }
            }

            if (logs.isEmpty()) {
                Text(
                    text = "No logs available.",
                    color = Color(0xFF8193A5),
                    fontSize = 12.sp
                )
            } else {
                logs.forEach { log ->
                    val level = log.level.toString().uppercase()

                    val logColor = when {
                        level.contains("ERROR") ->
                            Color(0xFFFF6262)

                        level.contains("WARN") ->
                            Color(0xFFFFC857)

                        level.contains("SUCCESS") ->
                            Color(0xFF00E5A8)

                        else ->
                            Color(0xFFB8D7EB)
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "[${log.timestamp}] [$level]",
                            color = logColor,
                            fontSize = 11.sp
                        )

                        Text(
                            text = log.message,
                            color = Color(0xFFE6F1FA),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
