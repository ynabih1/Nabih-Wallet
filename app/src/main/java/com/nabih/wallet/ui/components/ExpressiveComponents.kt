package com.nabih.wallet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.nabih.wallet.ui.theme.CategoryColors
import com.nabih.wallet.util.Formatters

fun segmentedCardShape(index: Int, count: Int): RoundedCornerShape {
    val large = 24.dp
    val small = 6.dp
    return when {
        count <= 1 -> RoundedCornerShape(large)
        index == 0 -> RoundedCornerShape(
            topStart = large,
            topEnd = large,
            bottomStart = small,
            bottomEnd = small
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = small,
            topEnd = small,
            bottomStart = large,
            bottomEnd = large
        )
        else -> RoundedCornerShape(small)
    }
}

@Composable
fun ExpressiveSegmentedCardItem(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    content: @Composable () -> Unit
) {
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val resolvedColor = containerColor ?: if (isDarkSurface) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    val shape = segmentedCardShape(index, count)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = shape,
        color = resolvedColor
    ) {
        content()
    }
}

@Composable
fun ExpressiveLinearProgressIndicator(
    progress: Float,
    activeColor: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    height: Dp = 8.dp
) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val clamped = progress.coerceIn(0f, 1f)
    val visualProgress = when {
        clamped <= 0f -> 0f
        clamped < 0.04f -> 0.04f
        else -> clamped
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val h = size.height
        val w = size.width
        if (w <= 0f || h <= 0f) return@Canvas
        val radius = CornerRadius(h / 2f, h / 2f)
        val gapPx = 4.dp.toPx()
        val stopDotRadius = (h * 0.25f).coerceAtLeast(2.dp.toPx())

        if (visualProgress <= 0f) {
            drawRoundRect(
                color = trackColor,
                topLeft = Offset.Zero,
                size = Size(w, h),
                cornerRadius = radius
            )
            val dotX = if (isRtl) h / 2f else w - h / 2f
            drawCircle(
                color = activeColor,
                radius = stopDotRadius,
                center = Offset(dotX, h / 2f)
            )
            return@Canvas
        }

        if (visualProgress >= 0.999f) {
            drawRoundRect(
                color = activeColor,
                topLeft = Offset.Zero,
                size = Size(w, h),
                cornerRadius = radius
            )
            return@Canvas
        }

        val activeWidth = (w * visualProgress).coerceAtLeast(h)
        val trackWidth = (w - activeWidth - gapPx).coerceAtLeast(0f)

        if (isRtl) {
            // Active segment starts on the right, track fills the left side
            val activeLeft = w - activeWidth
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(activeLeft, 0f),
                size = Size(activeWidth, h),
                cornerRadius = radius
            )
            if (trackWidth > h * 0.6f) {
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(0f, 0f),
                    size = Size(trackWidth, h),
                    cornerRadius = radius
                )
                drawCircle(
                    color = activeColor,
                    radius = stopDotRadius,
                    center = Offset(h / 2f, h / 2f)
                )
            }
        } else {
            drawRoundRect(
                color = activeColor,
                topLeft = Offset.Zero,
                size = Size(activeWidth, h),
                cornerRadius = radius
            )
            if (trackWidth > h * 0.6f) {
                val trackLeft = activeWidth + gapPx
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(trackLeft, 0f),
                    size = Size(trackWidth, h),
                    cornerRadius = radius
                )
                drawCircle(
                    color = activeColor,
                    radius = stopDotRadius,
                    center = Offset(w - h / 2f, h / 2f)
                )
            }
        }
    }
}

@Composable
fun ExpressiveCircularProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    strokeWidth: Dp = 6.dp,
    activeColor: Color = MaterialTheme.colorScheme.tertiary,
    trackColor: Color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.14f),
    labelColor: Color = MaterialTheme.colorScheme.onTertiaryContainer
) {
    val clamped = progress.coerceIn(0f, 1f)
    val percent = (clamped * 100f).toInt()

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)
            val strokeStyle = Stroke(width = strokePx, cap = StrokeCap.Round)

            if (clamped <= 0f) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = strokeStyle
                )
            } else if (clamped >= 0.999f) {
                drawArc(
                    color = activeColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = strokeStyle
                )
            } else {
                val gapDegrees = 18f
                val activeSweep = (clamped * 360f).coerceAtLeast(10f)
                val trackSweep = (360f - activeSweep - gapDegrees * 2f).coerceAtLeast(0f)

                if (trackSweep > 2f) {
                    drawArc(
                        color = trackColor,
                        startAngle = -90f + activeSweep + gapDegrees,
                        sweepAngle = trackSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = strokeStyle
                    )
                }
                drawArc(
                    color = activeColor,
                    startAngle = -90f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = strokeStyle
                )
            }
        }
        Text(
            text = Formatters.formatPercentInt(percent),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = labelColor
        )
    }
}

@Composable
fun TonalCategoryIcon(
    iconKey: String,
    colorKey: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    boxSize: Dp = 42.dp,
    iconSize: Dp = 22.dp
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f || isSystemInDarkTheme() && MaterialTheme.colorScheme.background.luminance() < 0.5f
    val accent = CategoryColors.accentColor(colorKey, isDark)
    val container = CategoryColors.containerColor(colorKey, isDark)

    Surface(
        modifier = modifier.size(boxSize),
        shape = RoundedCornerShape(14.dp),
        color = container
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = IconLibrary.iconFor(iconKey),
                contentDescription = contentDescription,
                tint = accent,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun TonalSquircleIcon(
    icon: ImageVector,
    contentDescription: String?,
    containerColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    boxSize: Dp = 42.dp,
    iconSize: Dp = 22.dp
) {
    Surface(
        modifier = modifier.size(boxSize),
        shape = RoundedCornerShape(14.dp),
        color = containerColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
fun SectionHeaderRow(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        trailing?.invoke()
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val container = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = container
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TonalSquircleIcon(
                icon = icon,
                contentDescription = null,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                boxSize = 56.dp,
                iconSize = 28.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NabihTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "وقت التذكير اليومي",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(state = timePickerState)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
