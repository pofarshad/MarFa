package net.marfanet.android.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Compose Overlay UI with RTT + Speed Chips
 * RSO-003: Real-time stats overlay that displays while VPN is active
 */
@Composable
fun StatsOverlayContent(
    overlayState: OverlayUiState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (overlayState.isVisible) {
        Box(
            modifier = modifier
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
                .clickable { onDismiss() }
                .padding(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // RTT Chip
                StatsChip(
                    label = "RTT",
                    value = overlayState.formatRtt(),
                    color = MaterialTheme.colorScheme.primary
                )
                
                // Upload Speed Chip
                StatsChip(
                    label = "↑",
                    value = overlayState.formatUploadSpeed(),
                    color = MaterialTheme.colorScheme.secondary
                )
                
                // Download Speed Chip
                StatsChip(
                    label = "↓",
                    value = overlayState.formatDownloadSpeed(),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun StatsChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.2f),
        contentColor = color
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}