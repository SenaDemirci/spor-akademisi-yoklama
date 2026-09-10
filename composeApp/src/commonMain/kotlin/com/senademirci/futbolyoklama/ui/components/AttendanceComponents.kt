package com.senademirci.futbolyoklama.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.senademirci.futbolyoklama.data.model.AttendanceStatus
import com.senademirci.futbolyoklama.ui.theme.StatusAbsent
import com.senademirci.futbolyoklama.ui.theme.StatusExcused
import com.senademirci.futbolyoklama.ui.theme.StatusLate
import com.senademirci.futbolyoklama.ui.theme.StatusPresent

/** Her durumun kendi rengi var — listeye bakınca renkten okunabilsin. */
fun AttendanceStatus.color(): Color = when (this) {
    AttendanceStatus.PRESENT -> StatusPresent
    AttendanceStatus.ABSENT -> StatusAbsent
    AttendanceStatus.EXCUSED -> StatusExcused
    AttendanceStatus.LATE -> StatusLate
}

/** Öğrenci fotoğrafı yoksa baş harfleri gösteren daire. */
@Composable
fun PlayerAvatar(
    initials: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    jerseyNumber: Int? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = jerseyNumber?.toString() ?: initials,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.36f).sp,
        )
    }
}

/** Geçmiş kayıtlarda tek harflik renkli durum rozeti. */
@Composable
fun StatusBadge(status: AttendanceStatus, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(status.color()),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = status.shortLabel,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

/**
 * Yoklama satırındaki dört durumlu seçici. Material3'ün segmented button'ı yerine
 * elle yazıldı: dokunma alanları daha geniş ve her durum kendi rengini alıyor.
 */
@Composable
fun AttendanceStatusSelector(
    selected: AttendanceStatus,
    onSelect: (AttendanceStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AttendanceStatus.entries.forEach { status ->
            val isSelected = status == selected
            val statusColor = status.color()
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) statusColor else Color.Transparent)
                    .border(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = if (isSelected) Color.Transparent
                        else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(10.dp),
                    )
                    .clickable { onSelect(status) }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = status.label,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}
