
package com.rines.esp32c3flasher.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FlashProgressCard(
    progress: Int,
    status: String,
    isBusy: Boolean,
    modifier: Modifier = Modifier
) {
    val safeProgress = progress.coerceIn(0, 100)

    val animatedProgress = animateFloatAsState(
        targetValue = safeProgress / 100f,
        animationSpec = tween(durationMillis = 400),
        label = "flashProgress"
    )

    val statusColor = when {
        isBusy -> Color(0xFF00D9FF)
        safeProgress >= 100 -> Color(0xFF00E5A8)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "FLASH PROGRESS",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isBusy) "OPERATION IN PROGRESS"
                    else if (safeProgress >= 100) "COMPLETED"
                    else "STANDBY",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "$safeProgress%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress.value },
                modifier = Modifier.fillMaxWidth(),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                text = status.ifBlank { "Waiting for operation..." },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
