package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.SegmaDemoCatalog
import com.example.model.TrendBookmark
import com.example.model.TrendingEditorialItem
import com.example.ui.components.SegmaSparkBadge
import com.example.ui.theme.SegmaBackground
import com.example.ui.theme.SegmaLime
import com.example.ui.theme.SegmaNavy
import com.example.ui.theme.SegmaSurface
import com.example.ui.theme.SegmaTextPrimary
import com.example.ui.theme.SegmaTextSecondary

@Composable
fun WhatsTrendingScreen(
    editorialId: String,
    bookmarks: List<TrendBookmark>,
    onBack: () -> Unit,
    onToggleBookmark: (TrendingEditorialItem, Boolean) -> Unit,
    onUseInAgent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val item = SegmaDemoCatalog.trendingEditorials.firstOrNull { it.id == editorialId }
        ?: SegmaDemoCatalog.trendingEditorials.first()
    val isBookmarked = bookmarks.any { it.trendId == item.id }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SegmaBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Header matching PDF Page 7
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SegmaNavy)
            }
            Text(
                text = "What's Trending",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = SegmaNavy
            )
            IconButton(
                onClick = { onToggleBookmark(item, isBookmarked) },
                modifier = Modifier.testTag("editorial_bookmark_btn")
            ) {
                Icon(
                    imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = if (isBookmarked) "Remove bookmark" else "Save bookmark",
                    tint = SegmaNavy
                )
            }
        }

        // Hero Banner matching PDF Page 7
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = SegmaNavy,
            border = BorderStroke(2.dp, SegmaLime),
            modifier = Modifier
                .fillMaxWidth()
                .height(142.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.badgeTitle,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        lineHeight = 25.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Image(
                    painter = painterResource(id = R.drawable.img_music_viral_collage),
                    contentDescription = item.headline,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(156.dp)
                        .height(106.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            item.categoryTags.forEach { tag ->
                Surface(shape = RoundedCornerShape(50), color = SegmaLime) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SegmaNavy,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = item.headline,
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            color = SegmaNavy
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${item.updatedLabel} • ${item.readTimeLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = SegmaTextSecondary
        )

        Spacer(modifier = Modifier.height(18.dp))

        // #1, #2, #3 Track Cards matching PDF Page 7
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item.tracks.forEach { track ->
                Column(modifier = Modifier.width(136.dp)) {
                    Text(
                        text = "#${track.rank}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = SegmaNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFE7EAF3),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(92.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_music_viral_collage),
                                    contentDescription = track.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.85f))
                                        .align(Alignment.Center),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = SegmaNavy
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = SegmaNavy
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.labelSmall,
                                color = SegmaTextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Why Are These Trending?",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = SegmaNavy
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.whyTrending,
            style = MaterialTheme.typography.bodyLarge,
            color = SegmaTextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Most Responsive Audience Segments",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = SegmaNavy
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.responsiveSegments,
            style = MaterialTheme.typography.bodyLarge,
            color = SegmaTextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Neon Lime Segma AI Recommendations Box matching PDF Page 7
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SegmaLime,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SegmaSparkBadge(
                        size = 32.dp,
                        containerColor = SegmaSurface,
                        iconTint = SegmaNavy
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Segma AI Recommendations",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SegmaNavy
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                item.aiRecommendations.forEach { rec ->
                    Text(
                        text = "• $rec",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SegmaNavy,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                onUseInAgent("Build a campaign concept inspired by '${item.headline}' using these insights: ${item.aiRecommendations.joinToString(" ")}")
            },
            colors = ButtonDefaults.buttonColors(containerColor = SegmaNavy, contentColor = Color.White),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Use This Insight in Segma Agent", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
