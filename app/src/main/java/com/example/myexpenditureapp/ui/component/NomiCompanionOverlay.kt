package com.example.myexpenditureapp.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import com.example.myexpenditureapp.domain.insights.HealthStatus
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myexpenditureapp.ui.theme.ExpenseRed
import com.example.myexpenditureapp.ui.theme.IncomeGreen
import com.example.myexpenditureapp.ui.theme.MonospaceFont
import com.example.myexpenditureapp.utils.formatIndian
import java.math.BigDecimal
import java.util.Calendar

/**
 * Nomi Companion Modal Overlay.
 * Triggered on clicking the mascot, plays the choreographed boot sequence,
 * and settles into the live mascot state with intelligent financial diagnostics.
 */
@Composable
fun NomiCompanionOverlay(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    healthStatus: HealthStatus = HealthStatus.OPTIMAL,
    healthScore: Int = 85,
    safeDailyAllowance: BigDecimal = BigDecimal.ZERO,
    todayBurn: BigDecimal = BigDecimal.ZERO,
    monthSpent: BigDecimal = BigDecimal.ZERO,
    overrideMood: MascotMood? = null,
    onOpenAnalytics: (() -> Unit)? = null
) {
    if (!isOpen) return

    var isBootComplete by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Stop dismissal when clicking card body */ }
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF0F172A) // Sleek Slate 900 Obsidian
                ),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.7f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val targetMood = overrideMood ?: when {
                        Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 0..5 || Calendar.getInstance().get(Calendar.HOUR_OF_DAY) >= 23 ->
                            MascotMood.SLEEPING
                        healthStatus == HealthStatus.OPTIMAL ->
                            MascotMood.OPTIMAL
                        healthStatus == HealthStatus.CRITICAL ->
                            MascotMood.ALERT
                        else ->
                            MascotMood.NEUTRAL
                    }

                    // Header Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(0.5.dp, Color(0xFF475569))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val statusColor = when {
                                    !isBootComplete -> Color(0xFF38BDF8)
                                    targetMood == MascotMood.SLEEPING -> Color(0xFF38BDF8).copy(alpha = 0.7f)
                                    targetMood == MascotMood.ALERT -> ExpenseRed
                                    targetMood == MascotMood.OPTIMAL -> IncomeGreen
                                    else -> Color(0xFF38BDF8)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Text(
                                    text = when {
                                        !isBootComplete -> "INITIALIZING..."
                                        targetMood == MascotMood.SLEEPING -> "NOMI SLEEPING"
                                        targetMood == MascotMood.ALERT -> "ALERT MODE"
                                        targetMood == MascotMood.OPTIMAL -> "NOMI ONLINE"
                                        else -> "NOMI ONLINE"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = MonospaceFont,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = statusColor
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Mascot Centerpiece with Animated Boot Sequence
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BootAnimatedMascotBot(
                            targetMood = targetMood,
                            size = 96.dp,
                            onBootComplete = {
                                isBootComplete = true
                            }
                        )
                    }

                    // Diagnostics & Speech Console
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically { it / 2 }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Dialogue Bubble
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.8f),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Text(
                                    text = when (targetMood) {
                                        MascotMood.OPTIMAL ->
                                            "\"All systems nominal! Your expenditure is healthy and well within safe runway limits.\""
                                        MascotMood.ALERT ->
                                            "\"Attention: High burn rate detected! Leak radar is monitoring for irregular subscriptions.\""
                                        MascotMood.SLEEPING ->
                                            "\"Zzz... Nomi is in low-power resting mode. Expenses are quiet and peaceful for the night.\""
                                        MascotMood.SCANNING ->
                                            "\"Auto-parsing transaction streams and verifying expense ledger hashes...\""
                                        else ->
                                            "\"Standing by. Real-time transaction surveillance and budget envelopes active.\""
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE2E8F0),
                                    modifier = Modifier.padding(14.dp)
                                )
                            }

                            // Vitals Bento Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Health Score
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.5f),
                                    border = BorderStroke(0.5.dp, Color(0xFF334155))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            "Score",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "$healthScore / 100",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = MonospaceFont,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = if (healthScore >= 75) IncomeGreen else ExpenseRed
                                        )
                                    }
                                }

                                // Safe Allowance
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.5f),
                                    border = BorderStroke(0.5.dp, Color(0xFF334155))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            "Safe Daily",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            safeDailyAllowance.formatIndian(includeSymbol = true),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = MonospaceFont,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = IncomeGreen
                                        )
                                    }
                                }

                                // Month Spent
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF1E293B).copy(alpha = 0.5f),
                                    border = BorderStroke(0.5.dp, Color(0xFF334155))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            "Oct Spent",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF94A3B8)
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            monthSpent.formatIndian(includeSymbol = true),
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontFamily = MonospaceFont,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = Color(0xFFF1F5F9)
                                        )
                                    }
                                }
                            }

                            // Action Button
                            Button(
                                onClick = {
                                    onDismiss()
                                    onOpenAnalytics?.invoke()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4F46E5) // Electric Indigo
                                )
                            ) {
                                Icon(
                                    Icons.Default.QueryStats,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Open In-Depth Analytics",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
