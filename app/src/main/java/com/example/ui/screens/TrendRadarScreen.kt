package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.SegmaDemoCatalog
import com.example.model.TrendBookmark
import com.example.model.TrendCategory
import com.example.model.TrendInsight
import com.example.ui.components.DemoDataBadge
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.SegmaOutlinedCard
import com.example.ui.components.SegmaSparkBadge
import com.example.ui.components.TrendLineChartCanvas
import com.example.ui.theme.SegmaBackground
import com.example.ui.theme.SegmaBorder
import com.example.ui.theme.SegmaLime
import com.example.ui.theme.SegmaNavy
import com.example.ui.theme.SegmaSurface
import com.example.ui.theme.SegmaTextPrimary
import com.example.ui.theme.SegmaTextSecondary

@Composable
fun TrendRadarScreen(
    bookmarks: List<TrendBookmark>,
    onBack: () -> Unit,
    onCreateStrategyFromTrend: (TrendInsight) -> Unit,
    onToggleBookmark: (TrendInsight, Boolean) -> Unit,
    onOpenWhatsTrending: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TrendCategory.ALL) }

    val filteredTrends = remember(searchQuery, selectedCategory) {
        SegmaDemoCatalog.trendInsights.filter { trend ->
            val matchesCat = selectedCategory == TrendCategory.ALL || trend.category == selectedCategory
            val q = searchQuery.trim()
            val matchesQuery = q.isEmpty() ||
                trend.title.contains(q, ignoreCase = true) ||
                trend.opportunityBody.contains(q, ignoreCase = true) ||
                trend.topHashtags.any { it.first.contains(q, ignoreCase = true) }
            matchesCat && matchesQuery
        }
    }

    val activeTrend = filteredTrends.firstOrNull()
    val isBookmarked = activeTrend != null && bookmarks.any { it.trendId == activeTrend.id }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SegmaBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Header matching PDF Page 4
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = SegmaNavy
                )
            }
            Text(
                text = "Trend Radar & Insights",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SegmaNavy
            )
            IconButton(
                onClick = {
                    if (activeTrend != null) {
                        onToggleBookmark(activeTrend, isBookmarked)
                    } else {
                        onOpenWhatsTrending("editorial_top3_audio")
                    }
                }
            ) {
                Icon(
                    imageVector = if (activeTrend != null) {
                        if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder
                    } else Icons.Filled.MoreHoriz,
                    contentDescription = "Bookmark trend",
                    tint = SegmaNavy
                )
            }
        }

        // Search + Category Filter Card matching PDF Page 4
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SegmaSurface,
            border = BorderStroke(1.2.dp, SegmaBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SegmaLime),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Search trends", tint = SegmaNavy)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search campus trends, eco-friendly...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SegmaTextSecondary
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = SegmaTextPrimary),
                            singleLine = true,
                            cursorBrush = SolidColor(SegmaNavy),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("trend_search_input")
                        )
                    }
                    IconButton(
                        onClick = { onOpenWhatsTrending("editorial_top3_audio") },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = "Editorial Trending", tint = SegmaNavy)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrendCategory.entries.forEach { cat ->
                        val selected = selectedCategory == cat
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) SegmaLime else SegmaSurface,
                            border = BorderStroke(1.dp, SegmaBorder),
                            modifier = Modifier
                                .clickable { selectedCategory = cat }
                                .testTag("trend_filter_${cat.name}")
                        ) {
                            Text(
                                text = cat.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = SegmaNavy,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Trend Surge This Week",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SegmaNavy
            )
            DemoDataBadge("Demo Data")
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeTrend == null) {
            EmptyStateCard(
                title = "No matching trends found",
                message = "Try clearing your search or switching back to All categories.",
                actionLabel = "Reset Filters",
                onAction = {
                    searchQuery = ""
                    selectedCategory = TrendCategory.ALL
                }
            )
        } else {
            // Main Trend Surge Card with Chart & Note Callout matching PDF Page 4
            SegmaOutlinedCard {
                Text(
                    text = "Featured Topic • ${activeTrend.category.label}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SegmaTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activeTrend.title,
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = SegmaNavy
                )
                Spacer(modifier = Modifier.height(6.dp))

                TrendLineChartCanvas(
                    points = activeTrend.chartPoints,
                    endValueLabel = activeTrend.growthLabel
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SegmaLime,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SegmaSparkBadge(
                            size = 32.dp,
                            containerColor = SegmaSurface,
                            iconTint = SegmaNavy
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Peak Timing Note",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = SegmaNavy
                            )
                            Text(
                                text = activeTrend.peakTimeNote,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SegmaNavy
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dark Navy Rising Hashtags Pager Card matching PDF Page 4
            val pagerState = rememberPagerState(pageCount = { filteredTrends.size })
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .height(124.dp)
                ) { page ->
                    val trendItem = filteredTrends[page]
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SegmaNavy,
                        border = BorderStroke(1.5.dp, SegmaLime),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Top Rising Hashtags (Demo):",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            trendItem.topHashtags.forEach { (tag, stats) ->
                                Text(
                                    text = "• $tag — $stats",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE7ECFF)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(filteredTrends.size) { idx ->
                        Box(
                            modifier = Modifier
                                .size(if (idx == pagerState.currentPage) 9.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx == pagerState.currentPage) SegmaNavy else Color(0xFFBDC2D6)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Neon Lime Segma AI Opportunity Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = SegmaLime,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    SegmaSparkBadge(
                        size = 34.dp,
                        containerColor = SegmaSurface,
                        iconTint = SegmaNavy
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = activeTrend.opportunityTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = SegmaNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeTrend.opportunityBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SegmaNavy
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { onCreateStrategyFromTrend(activeTrend) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SegmaNavy,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("create_strategy_from_trend_btn")
            ) {
                Text(
                    text = "Create Strategy from This Trend",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
