package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SegmaBorder
import com.example.ui.theme.SegmaLime
import com.example.ui.theme.SegmaLimeSoft
import com.example.ui.theme.SegmaNavy
import com.example.ui.theme.SegmaNavyDark
import com.example.ui.theme.SegmaSurface
import com.example.ui.theme.SegmaTextPrimary
import com.example.ui.theme.SegmaTextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SegmaLogoHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(color = SegmaNavy, radius = r)
            // Geometric network lines & lime nodes matching PDF logo
            for (i in 0 until 6) {
                val angle = (i * 60.0) * PI / 180.0
                val pt = Offset(
                    center.x + (r * 0.62f * cos(angle)).toFloat(),
                    center.y + (r * 0.62f * sin(angle)).toFloat()
                )
                drawLine(
                    color = Color.White,
                    start = center,
                    end = pt,
                    strokeWidth = 2f
                )
                drawCircle(
                    color = if (i % 2 == 0) SegmaLime else Color.White,
                    radius = 3.2f,
                    center = pt
                )
            }
            drawCircle(color = SegmaLime, radius = 4.2f, center = center)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "SEGMA.AI",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            ),
            color = SegmaNavy
        )
    }
}

@Composable
fun SegmaSparkBadge(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    containerColor: Color = SegmaLime,
    iconTint: Color = SegmaNavy
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = "Segma AI",
            tint = iconTint,
            modifier = Modifier.size(size * 0.56f)
        )
    }
}

enum class SegmaTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
) {
    HOME("home", "Home", Icons.Filled.Home, "nav_home"),
    AGENT("agent", "Segma Agent", Icons.Filled.AutoAwesome, "nav_agent"),
    INSIGHTS("insights", "Insights", Icons.AutoMirrored.Filled.ShowChart, "nav_insights"),
    CAMPAIGNS("campaigns", "Campaign", Icons.Filled.Flag, "nav_campaigns"),
    PROFILE("profile", "Profile", Icons.Filled.Person, "nav_profile")
}

@Composable
fun SegmaBottomNavBar(
    currentRoute: String,
    onTabSelected: (SegmaTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SegmaNavy,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmaTab.entries.forEach { tab ->
                val isSelected = currentRoute == tab.route
                Box(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) SegmaLime else Color.Transparent)
                        .clickable { onTabSelected(tab) }
                        .testTag(tab.testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = if (isSelected) SegmaNavy else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SegmaOutlinedCard(
    modifier: Modifier = Modifier,
    containerColor: Color = SegmaSurface,
    borderColor: Color = SegmaBorder,
    cornerRadius: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun TrendLineChartCanvas(
    points: List<Float>,
    endValueLabel: String,
    modifier: Modifier = Modifier,
    accentColor: Color = SegmaNavy
) {
    val safePoints = if (points.size >= 2) points else listOf(0.2f, 0.4f, 0.55f, 0.72f, 0.88f)
    Box(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .padding(top = 18.dp, end = 14.dp, start = 8.dp, bottom = 10.dp)
        ) {
            val w = size.width
            val h = size.height
            val origin = Offset(0f, h)

            // Axes
            drawLine(
                color = Color(0xFF1D2454),
                start = Offset(0f, 0f),
                end = origin,
                strokeWidth = 3f
            )
            drawLine(
                color = Color(0xFF1D2454),
                start = origin,
                end = Offset(w, h),
                strokeWidth = 3f
            )

            val stepX = w / safePoints.size.toFloat()
            val coords = safePoints.mapIndexed { idx, v ->
                val x = (idx + 0.8f) * stepX
                val clamped = v.coerceIn(0.08f, 0.98f)
                val y = h - (clamped * (h * 0.88f))
                Offset(x.coerceAtMost(w - 8f), y)
            }

            // Smooth line path
            val path = Path().apply {
                moveTo(0f, h)
                coords.forEach { pt -> lineTo(pt.x, pt.y) }
            }
            drawPath(
                path = path,
                color = accentColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 4.5f,
                    cap = StrokeCap.Round
                )
            )

            // Vertical stems & dots (middle dashed stem matches PDF Pages 4 & 5!)
            val midIdx = coords.size / 2
            coords.forEachIndexed { idx, pt ->
                drawLine(
                    color = Color(0xFF3E456B),
                    start = Offset(pt.x, h),
                    end = pt,
                    strokeWidth = 2.5f,
                    pathEffect = if (idx == midIdx) {
                        PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    } else null
                )
                drawCircle(
                    color = SegmaNavyDark,
                    radius = 7f,
                    center = pt
                )
            }
        }
        Text(
            text = endValueLabel,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF4BB543),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 6.dp)
        )
    }
}

@Composable
fun MetricGradientCard(
    title: String,
    value: String,
    subtitle: String? = null,
    isLimeStyle: Boolean,
    modifier: Modifier = Modifier
) {
    val brush = if (isLimeStyle) {
        Brush.verticalGradient(
            colors = listOf(SegmaSurface, SegmaLimeSoft, SegmaLime)
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF8C92AE), Color(0xFF28326E), SegmaNavy)
        )
    }
    val textColor = if (isLimeStyle) SegmaNavy else Color.White
    val subtitleColor = if (isLimeStyle) SegmaTextSecondary else Color(0xFFD5DAF0)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(brush)
            .border(1.2.dp, SegmaBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = textColor
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = subtitleColor
                )
            }
        }
    }
}

@Composable
fun DemoDataBadge(
    label: String = "Demo Data",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFFEEF1FF),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, Color(0xFFC5CCE8))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SegmaNavy,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    SegmaOutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SegmaSparkBadge(size = 44.dp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = SegmaTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = SegmaTextSecondary,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SegmaNavy,
                    modifier = Modifier.clickable(onClick = onAction)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EngagementFormulaDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SegmaSurface,
        icon = {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = "Formula",
                tint = SegmaNavy
            )
        },
        title = {
            Text(
                text = "Engagement Rate (ER) Formula",
                style = MaterialTheme.typography.titleLarge,
                color = SegmaNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ER (%) = ((Likes + Comments + Shares + Saves) / Reach) × 100",
                    style = MaterialTheme.typography.labelLarge,
                    color = SegmaNavy
                )
                Text(
                    text = "This standardized reach-based Engagement Rate measures total meaningful interactions relative to unique accounts reached. When Reach is 0, ER defaults to 0.0% until campaign metrics are recorded.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SegmaTextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Got it", color = SegmaNavy, fontWeight = FontWeight.Bold)
            }
        }
    )
}
