package com.example.myexpenditureapp.ui.screen

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myexpenditureapp.R
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.data.backup.BackupManager
import com.example.myexpenditureapp.ui.theme.*
import com.example.myexpenditureapp.ui.viewmodel.ThemeViewModel
import com.example.myexpenditureapp.ui.component.ForexChartAnimation
import com.example.myexpenditureapp.ui.component.GeometricMascotBot
import com.example.myexpenditureapp.ui.component.MascotMood
import com.example.myexpenditureapp.ui.component.NomiCompanionOverlay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ThemeViewModel = viewModel(),
    onNavigateToRules: () -> Unit = {},
    onNavigateToGoals: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {}
) {
    val currentMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val aboutIconRotation = remember { Animatable(0f) }
    val aboutIconScale = remember { Animatable(1f) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showNomiOverlay by remember { mutableStateOf(false) }
    var previewMood by remember { mutableStateOf(MascotMood.NEUTRAL) }
    var isNomiExtended by remember { mutableStateOf(false) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isNotificationAccessGranted by remember {
        mutableStateOf(com.example.myexpenditureapp.notifications.NotificationHelper.isNotificationListenerAccessGranted(context))
    }
    var isLiveStatusNotificationEnabled by remember {
        mutableStateOf(com.example.myexpenditureapp.notifications.NotificationHelper.isLiveStatusEnabled(context))
    }
    var isOverlayPermissionGranted by remember {
        mutableStateOf(com.example.myexpenditureapp.overlay.OverlayHelper.canDrawOverlays(context))
    }
    var isInstantOverlayEnabled by remember {
        mutableStateOf(com.example.myexpenditureapp.overlay.OverlayHelper.isInstantOverlayEnabled(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isNotificationAccessGranted = com.example.myexpenditureapp.notifications.NotificationHelper.isNotificationListenerAccessGranted(context)
                isOverlayPermissionGranted = com.example.myexpenditureapp.overlay.OverlayHelper.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showRestoreConfirmDialog by remember { mutableStateOf<android.net.Uri?>(null) }
    var isOperatingBackup by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isOperatingBackup = true
            scope.launch {
                val result = BackupManager.exportBackup(context, Graph.database, uri)
                isOperatingBackup = false
                if (result.isSuccess) {
                    Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Export failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            showRestoreConfirmDialog = uri
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // THEME / APPEARANCE
            item {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeTile(
                        label = "System",
                        icon = Icons.Default.BrightnessAuto,
                        isSelected = currentMode == ThemeMode.SYSTEM,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setThemeMode(ThemeMode.SYSTEM)
                        }
                    )
                    ThemeTile(
                        label = "Light",
                        icon = Icons.Default.LightMode,
                        isSelected = currentMode == ThemeMode.LIGHT,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setThemeMode(ThemeMode.LIGHT)
                        }
                    )
                    ThemeTile(
                        label = "Dark",
                        icon = Icons.Default.DarkMode,
                        isSelected = currentMode == ThemeMode.DARK,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setThemeMode(ThemeMode.DARK)
                        }
                    )
                }
            }

            // AUTOMATION
            item {
                Text(
                    text = "Automation & Rules",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        SettingsNavigationRow(
                            title = "Notification Listener Access",
                            subtitle = if (isNotificationAccessGranted) "Active • Background UPI auto-detection" else "Enable permission for automatic transaction logging",
                            icon = Icons.Default.NotificationsActive,
                            trailingContent = {
                                if (isNotificationAccessGranted) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(8.dp),
                                            shape = CircleShape,
                                            color = IncomeGreen
                                        ) {}
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Active",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = IncomeGreen,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                } else {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )

                        SettingsNavigationRow(
                            title = "Always-On Live Status",
                            subtitle = "Ambient daily & monthly spend status with shortcuts in notification tray",
                            icon = Icons.Default.Notifications,
                            trailingContent = {
                                Switch(
                                    checked = isLiveStatusNotificationEnabled,
                                    onCheckedChange = { isChecked ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isLiveStatusNotificationEnabled = isChecked
                                        com.example.myexpenditureapp.notifications.NotificationHelper.setLiveStatusEnabled(context, isChecked)
                                        if (isChecked) {
                                            com.example.myexpenditureapp.notifications.LiveStatusNotificationManager.refresh(context)
                                        } else {
                                            com.example.myexpenditureapp.notifications.NotificationHelper.cancelLiveStatusNotification(context)
                                        }
                                    }
                                )
                            },
                            onClick = {
                                val next = !isLiveStatusNotificationEnabled
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isLiveStatusNotificationEnabled = next
                                com.example.myexpenditureapp.notifications.NotificationHelper.setLiveStatusEnabled(context, next)
                                if (next) {
                                    com.example.myexpenditureapp.notifications.LiveStatusNotificationManager.refresh(context)
                                } else {
                                    com.example.myexpenditureapp.notifications.NotificationHelper.cancelLiveStatusNotification(context)
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )

                        SettingsNavigationRow(
                            title = "Instant Floating Pop-up",
                            subtitle = if (!isOverlayPermissionGranted) "Permission required • Tap to enable 'Display over other apps'" else "Truecaller-style interactive pop-up when payment is detected",
                            icon = Icons.Default.Bolt,
                            trailingContent = {
                                Switch(
                                    checked = isInstantOverlayEnabled && isOverlayPermissionGranted,
                                    onCheckedChange = { isChecked ->
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (!isOverlayPermissionGranted && isChecked) {
                                            com.example.myexpenditureapp.overlay.OverlayHelper.openOverlayPermissionSettings(context)
                                        } else {
                                            isInstantOverlayEnabled = isChecked
                                            com.example.myexpenditureapp.overlay.OverlayHelper.setInstantOverlayEnabled(context, isChecked)
                                        }
                                    }
                                )
                            },
                            onClick = {
                                if (!isOverlayPermissionGranted) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    com.example.myexpenditureapp.overlay.OverlayHelper.openOverlayPermissionSettings(context)
                                } else {
                                    val next = !isInstantOverlayEnabled
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isInstantOverlayEnabled = next
                                    com.example.myexpenditureapp.overlay.OverlayHelper.setInstantOverlayEnabled(context, next)
                                }
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )

                        SettingsNavigationRow(
                            title = "Smart Auto-Categorize Rules",
                            subtitle = "Map merchant keywords to categories",
                            icon = Icons.Default.AutoFixHigh,
                            onClick = onNavigateToRules
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )

                        SettingsNavigationRow(
                            title = "Bills & Subscriptions Radar",
                            subtitle = "Predictive recurring bills, EMIs & due dates",
                            icon = Icons.Default.EventRepeat,
                            onClick = onNavigateToSubscriptions
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )

                        SettingsNavigationRow(
                            title = "Categories & Hierarchy",
                            subtitle = "Manage expense and income categories",
                            icon = Icons.Default.Category,
                            onClick = onNavigateToCategories
                        )
                    }
                }
            }

            // TARGETS & GOALS
            item {
                Text(
                    text = "Goals & Savings",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        SettingsNavigationRow(
                            title = "Savings Goals & Pockets",
                            subtitle = "Track savings progress and target reserves",
                            icon = Icons.Default.Savings,
                            onClick = onNavigateToGoals
                        )
                    }
                }
            }

            // DATA BACKUP & RESTORE
            item {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(4.dp)) {
                        SettingsNavigationRow(
                            title = "Export Backup (JSON)",
                            subtitle = "Download all accounts, transactions, and budgets",
                            icon = Icons.Default.FileDownload,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                                exportLauncher.launch("my_expenditure_backup_$timestamp.json")
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )

                        SettingsNavigationRow(
                            title = "Restore Backup (JSON)",
                            subtitle = "Import data from a local JSON backup archive",
                            icon = Icons.Default.FileUpload,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                importLauncher.launch(arrayOf("application/json"))
                            }
                        )
                    }
                }
            }

            // ABOUT & PRIVACY
            item {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                )

                val triggerAboutAnimation: () -> Unit = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAboutDialog = true
                    scope.launch {
                        launch {
                            aboutIconScale.snapTo(0.82f)
                            aboutIconScale.animateTo(
                                targetValue = 1.28f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                            aboutIconScale.animateTo(
                                targetValue = 1f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                        launch {
                            aboutIconRotation.snapTo(0f)
                            aboutIconRotation.animateTo(
                                targetValue = 360f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                            aboutIconRotation.snapTo(0f)
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { triggerAboutAnimation() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .graphicsLayer {
                                    scaleX = aboutIconScale.value
                                    scaleY = aboutIconScale.value
                                    rotationZ = aboutIconRotation.value
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0B1019),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                    contentDescription = "SpendZen App Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SpendZen v2.0",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "100% On-Device • Encrypted & Offline Private",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Nomi Companion Sub-section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Hero Identity Row with Expand/Collapse Dropdown
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isNomiExtended = !isNomiExtended
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .combinedClickable(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isNomiExtended = !isNomiExtended
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showNomiOverlay = true
                                        }
                                    ),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0D131F),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    GeometricMascotBot(
                                        mood = previewMood,
                                        size = 32.dp,
                                        animated = true
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Nomi AI Companion",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "SpendZen's discreet geometric AI companion that ambiently mirrors your financial vitals in real time.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val dropdownRotation by androidx.compose.animation.core.animateFloatAsState(
                                targetValue = if (isNomiExtended) 180f else 0f,
                                animationSpec = tween(durationMillis = 250),
                                label = "nomi_dropdown_rotation"
                            )

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isNomiExtended = !isNomiExtended
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isNomiExtended) "Collapse Nomi Behaviors" else "Expand Nomi Behaviors",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer { rotationZ = dropdownRotation }
                                )
                            }
                        }

                        // Extended content showing all behaviors when toggled
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isNomiExtended,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(14.dp))

                                // Subtitle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "BEHAVIORAL STATES",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = "TAP: SELECT • HOLD: PREVIEW",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Behaviors List - tap to select, hold to preview in console!
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    NomiBehaviorItem(
                                        mood = MascotMood.NEUTRAL,
                                        title = "Neutral (| |)",
                                        tag = "Balanced Pace",
                                        tagColor = Color(0xFF38BDF8),
                                        description = "Cyan pill LEDs with organic eyelid blinks. Reflects steady, balanced expenditure within baseline targets.",
                                        isSelected = previewMood == MascotMood.NEUTRAL,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            previewMood = MascotMood.NEUTRAL
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            previewMood = MascotMood.NEUTRAL
                                            showNomiOverlay = true
                                        }
                                    )

                                    NomiBehaviorItem(
                                        mood = MascotMood.OPTIMAL,
                                        title = "Thriving / Optimal (^ ^)",
                                        tag = "Healthy Surplus",
                                        tagColor = IncomeGreen,
                                        description = "Mint smiling arcs with celebratory vertical bobbing. Signals budget surplus and spending safely below daily allowance.",
                                        isSelected = previewMood == MascotMood.OPTIMAL,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            previewMood = MascotMood.OPTIMAL
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            previewMood = MascotMood.OPTIMAL
                                            showNomiOverlay = true
                                        }
                                    )

                                    NomiBehaviorItem(
                                        mood = MascotMood.ALERT,
                                        title = "Alert (\\ /)",
                                        tag = "Budget Warning",
                                        tagColor = ExpenseRed,
                                        description = "Coral crimson sharp slits with fast jitter shake pulse. Flags anomalous spending velocity or budget threshold breach.",
                                        isSelected = previewMood == MascotMood.ALERT,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            previewMood = MascotMood.ALERT
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            previewMood = MascotMood.ALERT
                                            showNomiOverlay = true
                                        }
                                    )

                                    NomiBehaviorItem(
                                        mood = MascotMood.SLEEPING,
                                        title = "Sleeping (- -)",
                                        tag = "Ambient Rest",
                                        tagColor = Color(0xFF818CF8),
                                        description = "Dim resting bars with slow breathing float and floating 'z'. Activates between 23:00 - 06:00 or during long periods of quiet.",
                                        isSelected = previewMood == MascotMood.SLEEPING,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            previewMood = MascotMood.SLEEPING
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            previewMood = MascotMood.SLEEPING
                                            showNomiOverlay = true
                                        }
                                    )

                                    NomiBehaviorItem(
                                        mood = MascotMood.SCANNING,
                                        title = "Scanning (—·—)",
                                        tag = "Auto Radar",
                                        tagColor = Color(0xFFA855F7),
                                        description = "High-contrast reticle with sweeping radar scanlines. Active during SMS/UPI parsing and complex analytics queries.",
                                        isSelected = previewMood == MascotMood.SCANNING,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            previewMood = MascotMood.SCANNING
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            previewMood = MascotMood.SCANNING
                                            showNomiOverlay = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAboutDialog) {
            AlertDialog(
                onDismissRequest = { showAboutDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0B1019)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                        Text("About SpendZen", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ForexChartAnimation(
                            size = 170.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SpendZen v2.0",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "100% On-Device Financial Intelligence\nEncrypted Local Vaults • Zero Cloud Tracking",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                },
                confirmButton = {
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        onClick = { showAboutDialog = false }
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }

        showRestoreConfirmDialog?.let { uri ->
            AlertDialog(
                onDismissRequest = { showRestoreConfirmDialog = null },
                title = { Text("Restore Data from Backup?", fontWeight = FontWeight.Bold) },
                text = { Text("This will import accounts, transactions, budgets, rules, and goals from the JSON file into your database.") },
                confirmButton = {
                    Button(
                        shape = RoundedCornerShape(12.dp),
                        onClick = {
                            isOperatingBackup = true
                            scope.launch {
                                val res = BackupManager.importBackup(context, Graph.database, uri)
                                isOperatingBackup = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Data restored successfully!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Restore error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                                showRestoreConfirmDialog = null
                            }
                        }
                    ) {
                        Text("Restore", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreConfirmDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        NomiCompanionOverlay(
            isOpen = showNomiOverlay,
            overrideMood = previewMood,
            onDismiss = { showNomiOverlay = false }
        )
    }
}

@Composable
private fun ThemeTile(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (trailingContent != null) {
                trailingContent()
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NomiBehaviorItem(
    mood: MascotMood,
    title: String,
    tag: String,
    tagColor: Color,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 0.5.dp,
            color = if (isSelected) {
                tagColor.copy(alpha = 0.75f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF0B1019),
                border = BorderStroke(0.5.dp, tagColor.copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    GeometricMascotBot(
                        mood = mood,
                        size = 32.dp,
                        animated = true,
                        onClick = onClick
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = tagColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = tagColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    fontSize = 12.sp
                )
            }
        }
    }
}

