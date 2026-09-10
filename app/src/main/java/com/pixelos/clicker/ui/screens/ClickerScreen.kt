package com.pixelos.clicker.ui.screens

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
    var pixels by remember { mutableLongStateOf(0L) }
    var clickPower by remember { mutableLongStateOf(1L) }
    var autoCps by remember { mutableLongStateOf(0L) }
    var totalTaps by remember { mutableLongStateOf(0L) }
    var hapticEnabled by remember { mutableStateOf(true) }

    val haptic = LocalHapticFeedback.current

    // Upgrades list
    val upgrades = remember {
        mutableStateListOf(
            UpgradeItem("bot", "Авто-Кликер", "+1 пиксель/сек в фоне", Icons.Rounded.SmartToy, 15L, cpsBonus = 1L),
            UpgradeItem("core", "Турбо-Ядро", "+2 пикселя за каждый тап", Icons.Rounded.Speed, 50L, clickBonus = 2L),
            UpgradeItem("gpu", "Разгон Процессора", "+5 пикселей/сек", Icons.Rounded.Memory, 200L, cpsBonus = 5L),
            UpgradeItem("quantum", "Квантовый Тап", "+10 за каждый тап", Icons.Rounded.Bolt, 600L, clickBonus = 10L),
            UpgradeItem("supercluster", "PixelOS Сервер", "+25 пикселей/сек", Icons.Rounded.Dns, 2000L, cpsBonus = 25L)
        )
    }

    // Auto-clicker ticker loop
    LaunchedEffect(autoCps) {
        while (true) {
            delay(1000L)
            if (autoCps > 0) {
                pixels += autoCps
            }
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        label = "ButtonBounce"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoStories,
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
                    IconButton(onClick = { hapticEnabled = !hapticEnabled }) {
                        Icon(
                            imageVector = if (hapticEnabled) Icons.Rounded.Vibration else Icons.Rounded.VolumeMute,
                            contentDescription = "Вибрация",
                            tint = if (hapticEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
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
            Spacer(modifier = Modifier.height(12.dp))

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
                        Text("+${clickPower}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

            Spacer(modifier = Modifier.height(28.dp))

            // Main Score Display
            Text(
                text = "${pixels}",
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "пикселей накоплено",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Main Interactive Big Tap Target
            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(48.dp))
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
                        pixels += clickPower
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
                    modifier = Modifier.size(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Upgrades Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.ShoppingBag, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Магазин улучшений",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Upgrades List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
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
                                    upgrade.count += 1
                                    clickPower += upgrade.clickBonus
                                    autoCps += upgrade.cpsBonus
                                    if (hapticEnabled) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (canAfford) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = upgrade.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${upgrade.title} (${upgrade.count})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = upgrade.description,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    if (canAfford) {
                                        pixels -= currentCost
                                        upgrade.count += 1
                                        clickPower += upgrade.clickBonus
                                        autoCps += upgrade.cpsBonus
                                        if (hapticEnabled) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                },
                                enabled = canAfford,
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("${currentCost} ⚡", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
