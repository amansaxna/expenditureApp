package com.example.myexpenditureapp.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myexpenditureapp.domain.insights.InsightType
import com.example.myexpenditureapp.domain.insights.SmartInsight
import com.example.myexpenditureapp.ui.theme.ExpenseRed
import com.example.myexpenditureapp.ui.theme.IncomeGreen
import com.example.myexpenditureapp.ui.theme.IndigoPrimary
import com.example.myexpenditureapp.ui.theme.AmberWarning

@Composable
fun FinancialDigestSection(
    insights: List<SmartInsight>,
    modifier: Modifier = Modifier,
    onInsightClick: ((SmartInsight) -> Unit)? = null
) {
    if (insights.isEmpty()) return

    var activeStoryIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SMART FINANCIAL DIGEST",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = { activeStoryIndex = 0 },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(6.dp)
                ) {}
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Play Highlights",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // WhatsApp / Instagram Status-Style Story Circles
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
        ) {
            items(insights.size) { index ->
                val insight = insights[index]
                StatusStoryBubble(
                    insight = insight,
                    onClick = { activeStoryIndex = index }
                )
            }
        }
    }

    if (activeStoryIndex != null) {
        FinancialDigestStoryViewer(
            insights = insights,
            initialIndex = activeStoryIndex ?: 0,
            onDismiss = { activeStoryIndex = null },
            onInsightAction = onInsightClick
        )
    }
}

@Composable
fun StatusStoryBubble(
    insight: SmartInsight,
    onClick: () -> Unit
) {
    val accentColor = when (insight.type) {
        InsightType.POSITIVE -> IncomeGreen
        InsightType.WARNING -> ExpenseRed
        InsightType.TIP -> IndigoPrimary
        InsightType.NEUTRAL -> AmberWarning
    }

    val displayLabel = when {
        insight.metric != null -> insight.metric
        insight.title.contains(":") -> insight.title.substringBefore(":")
        else -> insight.title.take(14)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick)
    ) {
        // Story ring container
        Surface(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            color = Color.Transparent,
            border = BorderStroke(2.dp, accentColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                InsightVectorIcon(
                    iconKey = insight.icon,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = displayLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
