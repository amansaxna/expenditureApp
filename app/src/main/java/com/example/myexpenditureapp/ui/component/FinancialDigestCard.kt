package com.example.myexpenditureapp.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myexpenditureapp.domain.insights.InsightType
import com.example.myexpenditureapp.domain.insights.SmartInsight
import com.example.myexpenditureapp.ui.theme.ExpenseRed
import com.example.myexpenditureapp.ui.theme.IncomeGreen
import com.example.myexpenditureapp.ui.theme.IndigoPrimary
import com.example.myexpenditureapp.ui.theme.AmberWarning
import com.example.myexpenditureapp.ui.theme.MonospaceFont

@Composable
fun FinancialDigestSection(
    insights: List<SmartInsight>,
    modifier: Modifier = Modifier,
    onInsightClick: ((SmartInsight) -> Unit)? = null
) {
    if (insights.isEmpty()) return

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SMART FINANCIAL DIGEST",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
        ) {
            items(insights, key = { it.id }) { insight ->
                FinancialInsightCard(
                    insight = insight,
                    onClick = if (onInsightClick != null) { { onInsightClick(insight) } } else null
                )
            }
        }
    }
}

@Composable
fun FinancialInsightCard(
    insight: SmartInsight,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (accentColor, bgGradient) = when (insight.type) {
        InsightType.POSITIVE -> IncomeGreen to Brush.linearGradient(
            colors = listOf(IncomeGreen.copy(alpha = 0.12f), Color.Transparent)
        )
        InsightType.WARNING -> ExpenseRed to Brush.linearGradient(
            colors = listOf(ExpenseRed.copy(alpha = 0.12f), Color.Transparent)
        )
        InsightType.TIP -> IndigoPrimary to Brush.linearGradient(
            colors = listOf(IndigoPrimary.copy(alpha = 0.12f), Color.Transparent)
        )
        InsightType.NEUTRAL -> AmberWarning to Brush.linearGradient(
            colors = listOf(AmberWarning.copy(alpha = 0.10f), Color.Transparent)
        )
    }

    Card(
        modifier = modifier
            .width(280.dp)
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgGradient)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(insight.icon, style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onClick != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Inspect",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = accentColor
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (insight.metric != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = insight.metric,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = MonospaceFont,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
