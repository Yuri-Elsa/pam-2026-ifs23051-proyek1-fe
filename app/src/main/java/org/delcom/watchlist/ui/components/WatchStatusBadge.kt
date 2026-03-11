package org.delcom.watchlist.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.delcom.watchlist.network.data.WatchStatus

/**
 * Small badge displaying watch status with color coding:
 *   🔵 Sedang Ditonton   (blue)
 *   🟣 Belum Ditonton    (purple)
 *   🟢 Sudah Ditonton    (green)
 */
@Composable
fun WatchStatusBadge(status: WatchStatus, modifier: Modifier = Modifier) {
    val bgColor  = Color(status.bgColorHex)
    val dotColor = Color(status.dotColorHex)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = status.label,
            color = dotColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Horizontal selector for watch status — used in Add/Edit screens.
 */
@Composable
fun WatchStatusSelector(
    selectedStatus: WatchStatus,
    onStatusSelected: (WatchStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = WatchStatus.values()
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { status ->
            val isSelected = selectedStatus == status
            val bgColor  = Color(status.bgColorHex)
            val dotColor = Color(status.dotColorHex)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) bgColor else MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = if (isSelected) 1.5.dp else 0.dp,
                        color = if (isSelected) dotColor else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onStatusSelected(status) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (isSelected) dotColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), CircleShape)
                    )
                    Text(
                        text = status.label,
                        color = if (isSelected) dotColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}
