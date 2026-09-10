package com.pixelos.clicker.ui.screens

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class UpgradeItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val baseCost: Long,
    val clickBonus: Long = 0,
    val cpsBonus: Long = 0,
    var count: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClickerScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("pixel_clicker_save", Context.MODE_PRIVATE) }
    val haptic = LocalHapticFeedback.current

    // Upgrades definition
    val upgrades = remember {
        mutableStateListOf(
            UpgradeItem("bot", "Авто-Кликер", "+1 пиксель/сек в фоне", Icons.Rounded.SmartToy, 15L, cpsBonus = 1L),
            UpgradeItem("core", "Турбо-Ядро", "+2 пикселя за каждый тап", Icons.Rounded.Speed, 50L, clickBonus = 2L),
            UpgradeItem("gpu", "Разгон Процессора", "+5 пикселей/сек", Icons.Rounded.Memory, 200L, cpsBonus = 5L),
            UpgradeItem("quantum", "Квантовый Тап", "+10 за каждый тап", Icons.Rounded.Bolt, 600L, clickBonus = 10L),
            UpgradeItem("supercluster", "PixelOS Сервер", "+25 пикселей/сек", Icons.Rounded.Dns, 2000L, cpsBonus = 25L)
        )
    }

    // Load initial values from SharedPreferences
    var pixels by remember { mutableLongStateOf(prefs.getLong("saved_pixels", 0L)) }
    var clickPower by remember { mutableLongStateOf(prefs.getLong("saved_click_power", 1L)) }
    var autoCps by remember { mutableLongStateOf(prefs.getLong("saved_auto_cps", 0L)) }
    var totalTaps by remember { mutableLongStateOf(prefs.getLong("saved_total_taps", 0L)) }
    var hapticEnabled by remember { mutableStateOf(prefs.getBoolean("saved_haptic", true)) }

    // Load upgrade counts
    LaunchedEffect(Unit) {
        upgrades.forEachIndexed { index, item ->
            val savedCount = prefs.getInt("upgrade_${item.id}_count", 0)
            upgrades[index] = item.copy(count = savedCount)
        }
    }

    // Offline progress check
    var offlineEarned by remember { mutableLongStateOf(0L) }
    var showOfflineDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val lastTime = prefs.getLong("last_save_time", 0L)
        val now = System.currentTimeMillis()
        if (lastTime > 0L && autoCps > 0L) {
            val secondsAway = ((now - lastTime) / 1000L).coerceIn(0L, 86400L) // Max 24 hours
            if (secondsAway >= 5L) {
                val earned = secondsAway * autoCps
                if (earned > 0L) {
                    offlineEarned = earned
                    pixels += earned
                    showOfflineDialog = true
                }
            }
        }
    }

    // Helper save function
    fun saveGameState() {
        prefs.edit().apply {
            putLong("saved_pixels", pixels)
            putLong("saved_click_power", clickPower)
            putLong("saved_auto_cps", autoCps)
            putLong("saved_total_taps", totalTaps)
            putBoolean("saved_haptic", hapticEnabled)
            putLong("last_save_time", System.currentTimeMillis())
            upgrades.forEach {
                putInt("upgrade_${it.id}_count", it.count)
            }
            apply()
        }
    }

    // Auto-save periodically every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000L)
            saveGameState()
        }
    }

    // Auto-clicker ticker loop
    LaunchedEffect(autoCps) {
        while (true) {
            delay(1000L)
            if (autoCps > 0L) {
                pixels += autoCps
            }
        }
    }

    // Boost timer (x2 multiplier)
    var boostSecondsLeft by remember { mutableIntStateOf(0) }
    LaunchedEffect(boostSecondsLeft) {
        if (boostSecondsLeft > 0) {
            delay(1000L)
            boostSecondsLeft -= 1
        }
    }

    // Reset Dialog state
    var showResetDialog by remember { mutableStateOf(false) }

    val effectiveClickPower = if (boostSecondsLeft > 0) clickPower * 2 else clickPower

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        label = "ButtonBounce"
    )

    // Offline Dialog
    if (showOfflineDialog) {
        AlertDialog(
            onDismissRequest = { showOfflineDialog = false },
            icon = { Icon(Icons.Rounded.RocketLaunch, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Добро пожаловать обратно!") },
            text = { Text("Пока тебя не было, твои авто-кликеры нафармили +${offlineEarned} пикселей в фоне!") },
            confirmButton = {
                Button(onClick = { showOfflineDialog = false }) {
                    Text("Забрать")
                }
            }
        )
    }

    // Reset Progress Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Сбросить прогресс?") },
            text = { Text("Все накопленные пиксели и купленные улучшения будут сброшены до нуля.") },
            confirmButton = {
                Button(
                    onClick = {
                        pixels = 0L
                        clickPower = 1L
                        autoCps = 0L
                        totalTaps = 0L
                        upgrades.forEachIndexed { idx, it -> upgrades[idx] = it.copy(count = 0) }
                        prefs.edit().clear().apply()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Да, сбросить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = "Logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Pixel Clicker",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        hapticEnabled = !hapticEnabled
                        saveGameState()
                    }) {
                        Icon(
                            imageVector = if (hapticEnabled) Icons.Rounded.Vibration else Icons.Rounded.VolumeMute,
                            contentDescription = "Вибрация",
                            tint = if (hapticEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Сброс",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // Stat Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Сила клика", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("+${effectiveClickPower}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            if (boostSecondsLeft > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("x2 🔥", fontSize = 14.sp, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Авто-доход", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${autoCps}/сек", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }

            // Boost active indicator bar
            if (boostSecondsLeft > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.ElectricBolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Двойной буст активен: ${boostSecondsLeft / 60}:%02d".format(boostSecondsLeft % 60),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Main Score Display
            Text(
                text = "${pixels}",
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "пикселей накоплено",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Interactive Big Tap Target
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(44.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary
                            )
                        )
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) {
                        pixels += effectiveClickPower
                        totalTaps += 1
                        if (hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.TouchApp,
                    contentDescription = "Клик",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(76.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Boost Action Card (Bonus Reward)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        // Activate 3-minute 2x Boost + 300 free pixels
                        boostSecondsLeft = 180
                        pixels += 300L
                        if (hapticEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        saveGameState()
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Бонусный Буст x2", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text("+300 пикселей и клики x2 на 3 мин", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                    }
                    Button(
                        onClick = {
                            boostSecondsLeft = 180
                            pixels += 300L
                            if (hapticEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            saveGameState()
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text("Врубить ⚡", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Upgrades Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Магазин улучшений",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Upgrades List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(upgrades) { upgrade ->
                    val currentCost = (upgrade.baseCost * Math.pow(1.15, upgrade.count.toDouble())).toLong()
                    val canAfford = pixels >= currentCost

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = canAfford) {
                                if (canAfford) {
                                    pixels -= currentCost
                                    val newCount = upgrade.count + 1
                                    val idx = upgrades.indexOf(upgrade)
                                    if (idx != -1) {
                                        upgrades[idx] = upgrade.copy(count = newCount)
                                    }
                                    clickPower += upgrade.clickBonus
                                    autoCps += upgrade.cpsBonus
                                    if (hapticEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                    saveGameState()
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (canAfford) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = upgrade.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${upgrade.title} (${upgrade.count})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = upgrade.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    if (canAfford) {
                                        pixels -= currentCost
                                        val newCount = upgrade.count + 1
                                        val idx = upgrades.indexOf(upgrade)
                                        if (idx != -1) {
                                            upgrades[idx] = upgrade.copy(count = newCount)
                                        }
                                        clickPower += upgrade.clickBonus
                                        autoCps += upgrade.cpsBonus
                                        if (hapticEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                        saveGameState()
                                    }
                                },
                                enabled = canAfford,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("${currentCost} ⚡", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
