package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    private val viewModel: BatteryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.registerBatteryReceiver(this)
    }

    override fun onStop() {
        super.onStop()
        viewModel.unregisterBatteryReceiver(this)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(viewModel: BatteryViewModel) {
    val context = LocalContext.current
    val batteryState by viewModel.batteryState.collectAsState()
    val simulatorMode by viewModel.simulatorMode.collectAsState()

    var activeTab by remember { mutableStateOf("dashboard") } // dashboard, creator, homescreen

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == "dashboard",
                    onClick = { activeTab = "dashboard" },
                    icon = { Icon(Icons.Rounded.Dashboard, contentDescription = "แดชบอร์ด") },
                    label = { Text("สถานะแบต", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == "creator",
                    onClick = { activeTab = "creator" },
                    icon = { Icon(Icons.Rounded.Palette, contentDescription = "สตูดิโอวิดเจ็ต") },
                    label = { Text("ออกแบบวิดเจ็ต", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = activeTab == "homescreen",
                    onClick = { activeTab = "homescreen" },
                    icon = { Icon(Icons.Rounded.PhoneAndroid, contentDescription = "หน้าจอจำลอง") },
                    label = { Text("จำลองหน้าจอ", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            when (activeTab) {
                "dashboard" -> DashboardScreen(viewModel)
                "creator" -> WidgetCreatorScreen(viewModel)
                "homescreen" -> SimulatedHomeScreen(viewModel, onNavigateToApp = { activeTab = "dashboard" })
            }
        }
    }
}

// ==========================================
// SCREEN 1: DASHBOARD
// ==========================================
@Composable
fun DashboardScreen(viewModel: BatteryViewModel) {
    val batteryState by viewModel.batteryState.collectAsState()
    val simulatorMode by viewModel.simulatorMode.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "VOLT",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        ),
                        color = Color.White
                    )
                    Text(
                        text = ".",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "BATTERY INTELLIGENCE SYSTEM",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Real vs Simulator pill indicator
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "DEVICE: ACTIVE HARDWARE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, if (simulatorMode) MaterialTheme.colorScheme.error.copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.clickable {
                        viewModel.toggleSimulatorMode(!simulatorMode, context)
                    }
                ) {
                    Text(
                        text = if (simulatorMode) "SIMULATOR_ACTIVE" else "SYSTEM_OPTIMAL",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (simulatorMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gorgeous Wave Battery Liquid Indicator Card
        BatteryWaveLiquidCard(batteryState = batteryState)

        Spacer(modifier = Modifier.height(24.dp))

        // Bento Grid of Battery Stats
        Text(
            text = "ข้อมูลสุขภาพแบตเตอรี่แบบละเอียด",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = TextAlign.Start
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard(
                    title = "อุณหภูมิ",
                    value = "${batteryState.temperature}°C",
                    subtitle = when {
                        batteryState.temperature >= 45.0f -> "ร้อนผิดปกติ!"
                        batteryState.temperature >= 38.0f -> "อุ่นเล็กน้อย"
                        else -> "อุณหภูมิปกติ"
                    },
                    icon = Icons.Rounded.WbSunny,
                    tintColor = when {
                        batteryState.temperature >= 45.0f -> Color(0xFFE53935)
                        batteryState.temperature >= 38.0f -> Color(0xFFFFB300)
                        else -> Color(0xFF4CAF50)
                    }
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard(
                    title = "แรงดันไฟฟ้า",
                    value = String.format("%.2f V", batteryState.voltage),
                    subtitle = "แรงดันเสถียร",
                    icon = Icons.Rounded.FlashOn,
                    tintColor = Color(0xFF00ACC1)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard(
                    title = "สุขภาพแบต",
                    value = batteryState.healthThai,
                    subtitle = "อ้างอิงจากระบบ OS",
                    icon = Icons.Rounded.CheckCircle,
                    tintColor = if (batteryState.health == "Good") Color(0xFF4CAF50) else Color(0xFFE53935)
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard(
                    title = "แหล่งพลังงาน",
                    value = batteryState.powerSourceThai,
                    subtitle = "เทคโนโลยี: ${batteryState.technology}",
                    icon = Icons.Rounded.Settings,
                    tintColor = Color(0xFF5E35B1)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Expanded Simulated Mode Controller Panel
        if (simulatorMode) {
            SimulatorControllerPanel(viewModel, batteryState)
            Spacer(modifier = Modifier.height(24.dp))
        }

        // AI Battery Advisor Chat Section
        AIBatteryAdvisorSection(viewModel)
    }
}

@Composable
fun BatteryWaveLiquidCard(batteryState: BatteryState) {
    val animatedPercent = animateFloatAsState(
        targetValue = batteryState.level.toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )

    val infiniteTransition = rememberInfiniteTransition()
    val wavePhase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Simulated Phone Battery Tank
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Battery Container outline drawing
                val strokeColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val isCharging = batteryState.isCharging

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Drawing battery body rounded container
                    val batteryWidth = width * 0.7f
                    val batteryHeight = height * 0.9f
                    val xOffset = (width - batteryWidth) / 2
                    val yOffset = (height - batteryHeight) / 2

                    // Battery head tip at top
                    val headWidth = batteryWidth * 0.35f
                    val headHeight = height * 0.05f
                    val headX = (width - headWidth) / 2
                    val headY = yOffset - headHeight

                    drawRoundRect(
                        color = strokeColor,
                        topLeft = Offset(headX, headY),
                        size = Size(headWidth, headHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw outer capsule
                    val pathBattery = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                rect = androidx.compose.ui.geometry.Rect(
                                    xOffset, yOffset, xOffset + batteryWidth, yOffset + batteryHeight
                                ),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
                            )
                        )
                    }

                    drawPath(
                        path = pathBattery,
                        color = strokeColor,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw inner wave filled liquid
                    val fillHeightRatio = animatedPercent.value / 100f
                    val fillHeight = batteryHeight * fillHeightRatio
                    val fillTopY = yOffset + batteryHeight - fillHeight

                    clipPath(pathBattery) {
                        // Color theme of liquid changes on level/charging state
                        val liquidColors = when {
                            isCharging -> listOf(Color(0xFF66BB6A), Color(0xFF2E7D32)) // Green glow
                            batteryState.level <= 15 -> listOf(Color(0xFFEF5350), Color(0xFFC62828)) // Red danger
                            batteryState.level <= 35 -> listOf(Color(0xFFFFB74D), Color(0xFFEF6C00)) // Amber low
                            else -> listOf(primaryColor, secondaryColor)
                        }

                        // Drawing waving wave using Sine function
                        val wavePath = Path()
                        val amplitude = 8.dp.toPx()
                        val wavelength = batteryWidth

                        wavePath.moveTo(xOffset, yOffset + batteryHeight)
                        wavePath.lineTo(xOffset, fillTopY)

                        for (x in 0..batteryWidth.toInt()) {
                            val relativeX = x.toFloat()
                            val absX = xOffset + relativeX
                            val sineY = fillTopY + sin((relativeX / wavelength) * 2 * Math.PI.toFloat() + wavePhase.value) * amplitude
                            wavePath.lineTo(absX, sineY)
                        }

                        wavePath.lineTo(xOffset + batteryWidth, yOffset + batteryHeight)
                        wavePath.close()

                        drawPath(
                            path = wavePath,
                            brush = Brush.verticalGradient(
                                colors = liquidColors,
                                startY = fillTopY - amplitude,
                                endY = yOffset + batteryHeight
                            )
                        )

                        // If charging, draw a glowing thunder icon in middle
                        if (isCharging) {
                            // Simple lightning icon layout points relative to canvas
                            val lWidth = batteryWidth * 0.35f
                            val lHeight = batteryHeight * 0.4f
                            val lX = xOffset + (batteryWidth - lWidth) / 2
                            val lY = yOffset + (batteryHeight - lHeight) / 2

                            val boltPath = Path().apply {
                                moveTo(lX + lWidth * 0.6f, lY)
                                lineTo(lX, lY + lHeight * 0.55f)
                                lineTo(lX + lWidth * 0.45f, lY + lHeight * 0.55f)
                                lineTo(lX + lWidth * 0.35f, lY + lHeight)
                                lineTo(lX + lWidth, lY + lHeight * 0.45f)
                                lineTo(lX + lWidth * 0.55f, lY + lHeight * 0.45f)
                                close()
                            }
                            drawPath(boltPath, Color.White.copy(alpha = 0.85f))
                        }
                    }
                }
            }

            // Numeric Stats
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${batteryState.level}%",
                    fontSize = 52.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = when {
                        batteryState.isCharging -> Color(0xFF2E7D32)
                        batteryState.level <= 15 -> Color(0xFFC62828)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Icon(
                        imageVector = if (batteryState.isCharging) Icons.Rounded.FlashOn else Icons.Rounded.BatteryChargingFull,
                        contentDescription = null,
                        tint = if (batteryState.isCharging) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (batteryState.isCharging) "กำลังชาร์จด่วน" else "กำลังดึงพลังงาน",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Estimated Time
                val textTime = if (batteryState.isCharging) {
                    if (batteryState.level >= 100) "ชาร์จเต็มแล้ว" else "อีกประมาณ 54 นาทีเต็ม"
                } else {
                    "ใช้งานได้อีกประมาณ 1 วัน 4 ชม."
                }
                Text(
                    text = textTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Light
                )
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, subtitle: String, icon: ImageVector, tintColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SimulatorControllerPanel(viewModel: BatteryViewModel, state: BatteryState) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Build, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "แผงควบคุมการจำลอง (Simulator Settings)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "ลากปรับค่าด้านล่างเพื่อทดสอบปฏิกิริยาวิดเจ็ตและข้อแนะนำ AI",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

            // Slider 1: Battery Level
            Text(text = "ระดับแบตเตอรี่: ${state.level}%", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = state.level.toFloat(),
                onValueChange = {
                    viewModel.updateSimulatedState(
                        level = it.toInt(),
                        isCharging = state.isCharging,
                        temperature = state.temperature,
                        voltage = state.voltage,
                        health = state.health,
                        powerSource = state.powerSource,
                        context = context
                    )
                },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.error,
                    activeTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            )

            // Slider 2: Battery Temperature
            Text(text = "อุณหภูมิ: ${String.format("%.1f", state.temperature)}°C", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = state.temperature,
                onValueChange = {
                    viewModel.updateSimulatedState(
                        level = state.level,
                        isCharging = state.isCharging,
                        temperature = it,
                        voltage = state.voltage,
                        health = if (it >= 45.0f) "Overheat" else "Good",
                        powerSource = state.powerSource,
                        context = context
                    )
                },
                valueRange = 0f..60f,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.error,
                    activeTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            )

            // Row 3: Charging source and Charging toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "สถานะการชาร์จไฟ:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Switch(
                    checked = state.isCharging,
                    onCheckedChange = {
                        viewModel.updateSimulatedState(
                            level = state.level,
                            isCharging = it,
                            temperature = state.temperature,
                            voltage = state.voltage,
                            health = state.health,
                            powerSource = if (it) "AC" else "Battery",
                            context = context
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Power Sources row selector
            if (state.isCharging) {
                Text(text = "แหล่งพลังงานที่เชื่อมต่อ:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sources = listOf("AC", "USB", "Wireless")
                    sources.forEach { source ->
                        val selected = state.powerSource == source
                        ElevatedFilterChip(
                            selected = selected,
                            onClick = {
                                viewModel.updateSimulatedState(
                                    level = state.level,
                                    isCharging = state.isCharging,
                                    temperature = state.temperature,
                                    voltage = state.voltage,
                                    health = state.health,
                                    powerSource = source,
                                    context = context
                                )
                            },
                            label = { Text(source) }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// AI ADV-SECTION (GEMINI CLIENT API CHAT)
// ==========================================
@Composable
fun AIBatteryAdvisorSection(viewModel: BatteryViewModel) {
    val context = LocalContext.current
    val aiLoading by viewModel.isAiLoading.collectAsState()
    val chatHistory by viewModel.aiChatHistory.collectAsState()
    val aiError by viewModel.aiError.collectAsState()

    // Read the secret key from BuildConfig
    val apiKey = BuildConfig.GEMINI_API_KEY
    val isKeyConfigured = apiKey.isNotEmpty() && apiKey != "your_api_key_here" && apiKey != "MY_GEMINI_API_KEY"

    var chatMessageText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Help,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ผู้ช่วยวิเคราะห์แบตอัจฉริยะ AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (chatHistory.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearChatHistory() }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "ล้างแชท", tint = Color.Red.copy(alpha = 0.7f))
                    }
                }
            }

            Text(
                text = "ใช้โมเดล Gemini AI วิเคราะห์อุณหภูมิ แรงดันไฟ และระดับประจุ เพื่อวินิจฉัยสุขภาพแบตเตอรี่ของคุณพร้อมเคล็ดลับส่วนบุคคล",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (!isKeyConfigured) {
                // Key configuration guide card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Yellow.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.Yellow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "💡 ยังไม่ได้ตั้งค่าคีย์ Gemini API",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF6D4C41)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "คุณสามารถตั้งค่าคีย์ API ได้ที่เมนูแผงควบคุมด้านข้างของระบบ AI Studio เพื่อเรียกใช้งานแชทผู้ช่วยวิเคราะห์แบตเตอรี่อัจฉริยะแบบเรียลไทม์",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Initial prompt trigger button
                if (chatHistory.isEmpty()) {
                    Button(
                        onClick = { viewModel.triggerInitialDiagnosis(apiKey) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.WbSunny, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("เริ่มการวินิจฉัยสุขภาพด่วนด้วย AI")
                    }
                } else {
                    // Conversation layout
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 6.dp)
                    ) {
                        chatHistory.forEach { (role, message) ->
                            val isUser = role == "user"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isUser) 16.dp else 4.dp,
                                                bottomEnd = if (isUser) 4.dp else 16.dp
                                            )
                                        )
                                        .background(
                                            if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .padding(12.dp)
                                        .widthIn(max = 260.dp)
                                ) {
                                    Text(
                                        text = message,
                                        fontSize = 13.sp,
                                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }

                        if (aiLoading) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI กำลังวิเคราะห์สัญญาณความร้อนและประจุ...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (aiError != null) {
                            Text(
                                text = aiError ?: "",
                                color = Color.Red,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input form
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatMessageText,
                            onValueChange = { chatMessageText = it },
                            placeholder = { Text("ถามคำถามเพิ่มเติมเกี่ยวกับแบตเตอรี่...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (chatMessageText.isNotBlank()) {
                                    viewModel.askAIBatteryAdvisor(chatMessageText, apiKey)
                                    chatMessageText = ""
                                }
                            })
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (chatMessageText.isNotBlank()) {
                                    viewModel.askAIBatteryAdvisor(chatMessageText, apiKey)
                                    chatMessageText = ""
                                }
                            },
                            enabled = chatMessageText.isNotBlank() && !aiLoading,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Icon(Icons.Rounded.Send, contentDescription = "ส่ง")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: WIDGET CREATOR
// ==========================================
@Composable
fun WidgetCreatorScreen(viewModel: BatteryViewModel) {
    val widgetStyle by viewModel.widgetStyle.collectAsState()
    val widgetColor by viewModel.widgetColor.collectAsState()
    val showTemp by viewModel.widgetShowTemp.collectAsState()
    val showVoltage by viewModel.widgetShowVoltage.collectAsState()
    val showHealth by viewModel.widgetShowHealth.collectAsState()
    val isTransparent by viewModel.widgetTransparent.collectAsState()
    val fontSelection by viewModel.widgetFont.collectAsState()
    val batteryState by viewModel.batteryState.collectAsState()

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "DESIGNER",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = Color.White
                )
                Text(
                    text = ".",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "WIDGET CONFIGURATION LAB",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // LIVE PREVIEW BLOCK
        Text(
            text = "ตัวอย่างวิดเจ็ตสด (Live Preview)",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Widget Canvas Rendering
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF27272A), Color(0xFF050505))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            WidgetRenderer(
                style = widgetStyle,
                color = Color(widgetColor),
                showTemp = showTemp,
                showVoltage = showVoltage,
                showHealth = showHealth,
                transparent = isTransparent,
                font = fontSelection,
                batteryState = batteryState
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CONTROLLERS SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 1. STYLE SELECTOR
                Text(text = "เลือกรูปแบบวิดเจ็ต", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val styles = listOf(
                        WidgetStyle.CIRCLE_RING to "วงกลม",
                        WidgetStyle.MATERIAL_BAR to "แถบยาว",
                        WidgetStyle.RETRO_NEON to "เรโทร",
                        WidgetStyle.COMPACT_GRID to "ตาราง"
                    )
                    styles.forEach { (style, label) ->
                        val selected = widgetStyle == style
                        OutlinedButton(
                            onClick = { viewModel.setWidgetStyle(style) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. ACCENT COLOR PICKER
                Text(text = "เลือกโทนสีสะท้อนแสงวิดเจ็ต", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colors = listOf(
                        0xFF4CAF50 to "เขียว",
                        0xFF00BCD4 to "ฟ้า",
                        0xFFE91E63 to "ชมพู",
                        0xFF9C27B0 to "ม่วง",
                        0xFFFF9800 to "ส้ม",
                        0xFF00796B to "ทีล"
                    )
                    colors.forEach { (colorVal, name) ->
                        val selected = widgetColor == colorVal.toLong()
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal.toLong()))
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.setWidgetColor(colorVal.toLong()) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. BACKGROUND MODE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "พื้นหลังโปร่งใส (Glassmorphism)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = "จำลองการแสดงผลโปร่งใสมองทะลุจอได้", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isTransparent, onCheckedChange = { viewModel.setWidgetTransparent(it) })
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. FONT STYLE
                Text(text = "รูปแบบตัวอักษรของตัวเลข", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fonts = listOf(
                        WidgetFont.DEFAULT to "ปกติ",
                        WidgetFont.CYBER_GLOW to "นีออน",
                        WidgetFont.MONOSPACE to "โค้ดดิ้ง",
                        WidgetFont.SERIF to "โมเดิร์น"
                    )
                    fonts.forEach { (font, label) ->
                        val selected = fontSelection == font
                        ElevatedFilterChip(
                            selected = selected,
                            onClick = { viewModel.setWidgetFont(font) },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5. DETAIL SWITCHES
                Text(text = "ข้อมูลที่จะแสดงบนวิดเจ็ต", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "แสดงอุณหภูมิแบตเตอรี่", fontSize = 13.sp)
                    Checkbox(checked = showTemp, onCheckedChange = { viewModel.setWidgetShowTemp(it) })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "แสดงแรงดันกระแสไฟ (V)", fontSize = 13.sp)
                    Checkbox(checked = showVoltage, onCheckedChange = { viewModel.setWidgetShowVoltage(it) })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "แสดงสถานะสุขภาพของประจุ", fontSize = 13.sp)
                    Checkbox(checked = showHealth, onCheckedChange = { viewModel.setWidgetShowHealth(it) })
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Native Widget placement guide
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "📲 วิธีการนำไปใช้งานบนจอหลักของโทรศัพท์จริง",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "1. ย่อแอพนี้ลงแล้วไปที่หน้าจอหลัก (Home Screen)\n" +
                           "2. แตะค้างบนพื้นที่ว่างบนหน้าจอ\n" +
                           "3. เลือกเมนู 'วิดเจ็ต' (Widgets)\n" +
                           "4. ค้นหาชื่อ 'Battery Monitor Widget' แล้วแตะค้างนำไปวางบนหน้าจอได้ทันที!\n\n" +
                           "ระดับประจุแบตเตอรี่และอุณหภูมิของจริงจะซิงค์ตรงกับวิดเจ็ตบนระบบมือถือของคุณตลอด 24 ชั่วโมง",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun WidgetRenderer(
    style: WidgetStyle,
    color: Color,
    showTemp: Boolean,
    showVoltage: Boolean,
    showHealth: Boolean,
    transparent: Boolean,
    font: WidgetFont,
    batteryState: BatteryState
) {
    val containerBg = if (transparent) Color.Black.copy(alpha = 0.4f) else Color(0xFF1E293B)
    val textStyleFont = when (font) {
        WidgetFont.CYBER_GLOW -> FontFamily.SansSerif
        WidgetFont.MONOSPACE -> FontFamily.Monospace
        WidgetFont.SERIF -> FontFamily.Serif
        else -> FontFamily.Default
    }
    val fontWeightText = if (font == WidgetFont.CYBER_GLOW) FontWeight.ExtraBold else FontWeight.Bold

    Box(
        modifier = Modifier
            .size(width = 280.dp, height = 120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(containerBg)
            .padding(12.dp)
    ) {
        when (style) {
            WidgetStyle.CIRCLE_RING -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Circle Canvas progress bar
                    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Draw background circle track
                            drawArc(
                                color = Color.White.copy(alpha = 0.1f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx())
                            )
                            // Draw active level progress arc
                            drawArc(
                                color = color,
                                startAngle = -90f,
                                sweepAngle = batteryState.level * 3.6f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx())
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${batteryState.level}%",
                                fontSize = 18.sp,
                                fontWeight = fontWeightText,
                                color = Color.White,
                                fontFamily = textStyleFont
                            )
                            if (batteryState.isCharging) {
                                Icon(Icons.Rounded.FlashOn, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
                            }
                        }
                    }

                    // Metric text lines
                    Column(verticalArrangement = Arrangement.Center) {
                        Text(
                            text = if (batteryState.isCharging) "⚡ กำลังชาร์จ..." else "🔋 แบตเตอรี่ดี",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (showTemp) Text(text = "🌡️ อุณหภูมิ: ${batteryState.temperature}°C", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        if (showVoltage) Text(text = "⚡ แรงดัน: ${String.format("%.2f", batteryState.voltage)}V", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        if (showHealth) Text(text = "💖 สุขภาพ: ${batteryState.healthThai}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            WidgetStyle.MATERIAL_BAR -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.BatteryChargingFull, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "ระดับประจุไฟ", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "${batteryState.level}%",
                            fontSize = 16.sp,
                            fontWeight = fontWeightText,
                            color = color,
                            fontFamily = textStyleFont
                        )
                    }

                    // Horizontal Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(batteryState.level / 100f)
                                .fillMaxHeight()
                                .background(color)
                        )
                    }

                    // Subtext info Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val items = mutableListOf<String>()
                        if (showTemp) items.add("${batteryState.temperature}°C")
                        if (showVoltage) items.add("${String.format("%.2f", batteryState.voltage)}V")
                        if (showHealth) items.add(batteryState.healthThai)
                        
                        Text(
                            text = if (items.isNotEmpty()) items.joinToString(" • ") else "พร้อมใช้งาน",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )

                        Text(
                            text = if (batteryState.isCharging) "กำลังเชื่อมต่อไฟ" else "โหมดพลังงานปกติ",
                            fontSize = 10.sp,
                            color = color
                        )
                    }
                }
            }

            WidgetStyle.RETRO_NEON -> {
                // Cyber Retro grid design with neon lines
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            // Draw cyan neon grid lines in background
                            val gridSize = 14.dp.toPx()
                            for (x in 0..size.width.toInt() step gridSize.toInt()) {
                                drawLine(
                                    color = Color(0xFF00E5FF).copy(alpha = 0.08f),
                                    start = Offset(x.toFloat(), 0f),
                                    end = Offset(x.toFloat(), size.height),
                                    strokeWidth = 1f
                                )
                            }
                            for (y in 0..size.height.toInt() step gridSize.toInt()) {
                                drawLine(
                                    color = Color(0xFF00E5FF).copy(alpha = 0.08f),
                                    start = Offset(0f, y.toFloat()),
                                    end = Offset(size.width, y.toFloat()),
                                    strokeWidth = 1f
                                )
                            }
                        }
                        .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SYSTEM BATTERY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = color,
                            fontFamily = FontFamily.Monospace
                        )
                        Box(
                            modifier = Modifier
                                .background(if (batteryState.isCharging) Color.Green.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f))
                                .border(1.dp, if (batteryState.isCharging) Color.Green else Color.Red, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (batteryState.isCharging) "CHARGING" else "DISCHARGE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (batteryState.isCharging) Color.Green else Color.Red,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⚡${batteryState.level} PCT",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = color,
                            fontFamily = textStyleFont
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            if (showTemp) Text(text = "TEMP: ${batteryState.temperature} C", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace)
                            if (showVoltage) Text(text = "VOLT: ${String.format("%.2f", batteryState.voltage)} V", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace)
                            if (showHealth) Text(text = "HLTH: ${batteryState.health.uppercase()}", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }

            WidgetStyle.COMPACT_GRID -> {
                // Modular block grid
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left 40% Block
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${batteryState.level}%",
                            fontSize = 22.sp,
                            fontWeight = fontWeightText,
                            color = color,
                            fontFamily = textStyleFont
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (batteryState.isCharging) "ชาร์จไฟ" else "แบตเตอรี่",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right 60% Block
                    Column(
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceAround
                    ) {
                        if (showTemp) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("อุณหภูมิ", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                                    Text("${batteryState.temperature}°C", fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (showVoltage) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("แรงดันไฟฟ้า", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                                    Text("${String.format("%.2f", batteryState.voltage)}V", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (showHealth) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("ระบบเคมี", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                                    Text(batteryState.healthThai, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: SIMULATED ANDROID HOME SCREEN
// ==========================================
data class DesktopWidget(
    val id: String,
    val style: WidgetStyle,
    val color: Long,
    val showTemp: Boolean,
    val showVoltage: Boolean,
    val showHealth: Boolean,
    val transparent: Boolean,
    val font: WidgetFont
)

@Composable
fun SimulatedHomeScreen(viewModel: BatteryViewModel, onNavigateToApp: () -> Unit) {
    val batteryState by viewModel.batteryState.collectAsState()
    val widgetStyle by viewModel.widgetStyle.collectAsState()
    val widgetColor by viewModel.widgetColor.collectAsState()
    val showTemp by viewModel.widgetShowTemp.collectAsState()
    val showVoltage by viewModel.widgetShowVoltage.collectAsState()
    val showHealth by viewModel.widgetShowHealth.collectAsState()
    val isTransparent by viewModel.widgetTransparent.collectAsState()
    val fontSelection by viewModel.widgetFont.collectAsState()

    // Wallpaper selections
    val wallpapers = listOf(
        0 to "ไล่โทนออโรร่า",
        1 to "คอสมิคเข้ม",
        2 to "ทิวเขาดวงดาว",
        3 to "ตารางไซเบอร์"
    )
    var selectedWallpaperIdx by remember { mutableStateOf(0) }

    val wallpaperBackground = when (selectedWallpaperIdx) {
        1 -> Brush.verticalGradient(listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)))
        2 -> Brush.verticalGradient(listOf(Color(0xFF141E30), Color(0xFF243B55)))
        3 -> Brush.sweepGradient(listOf(Color(0xFF0D0221), Color(0xFF0F084B), Color(0xFF0D0221)))
        else -> Brush.verticalGradient(listOf(Color(0xFF6A11CB), Color(0xFF2575FC)))
    }

    // Active placed widgets list
    var placedWidgets = remember { mutableStateListOf<DesktopWidget>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "SANDBOX",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp
                    ),
                    color = Color.White
                )
                Text(
                    text = ".",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = "INTERACTIVE HOME ENVIRONMENT",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Wallpaper selector row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            wallpapers.forEach { (idx, label) ->
                ElevatedFilterChip(
                    selected = selectedWallpaperIdx == idx,
                    onClick = { selectedWallpaperIdx = idx },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action controls to spawn/remove
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    placedWidgets.add(
                        DesktopWidget(
                            id = System.currentTimeMillis().toString(),
                            style = widgetStyle,
                            color = widgetColor,
                            showTemp = showTemp,
                            showVoltage = showVoltage,
                            showHealth = showHealth,
                            transparent = isTransparent,
                            font = fontSelection
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("แปะวิดเจ็ตลงหน้าจอ")
            }

            if (placedWidgets.isNotEmpty()) {
                TextButton(onClick = { placedWidgets.clear() }) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ล้างหน้าจอ", color = Color.Red)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // MOBILE SCREEN PREVIEW BOX
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(wallpaperBackground)
                .border(6.dp, Color.Black, RoundedCornerShape(32.dp))
                .padding(20.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mock Status Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("10:42 AM", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${batteryState.level}%", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (batteryState.isCharging) Icons.Rounded.FlashOn else Icons.Rounded.BatteryChargingFull,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Place Battery Widget
                if (placedWidgets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "แตะปุ่ม 'แปะวิดเจ็ตลงหน้าจอ' ด้านบน\nเพื่อทดลองวางวิดเจ็ตที่คุณออกแบบไว้",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        placedWidgets.forEach { widget ->
                            Box(
                                modifier = Modifier
                                    .clickable { onNavigateToApp() } // Clicking simulated widget opens the app!
                                    .animateContentSize()
                            ) {
                                WidgetRenderer(
                                    style = widget.style,
                                    color = Color(widget.color),
                                    showTemp = widget.showTemp,
                                    showVoltage = widget.showVoltage,
                                    showHealth = widget.showHealth,
                                    transparent = widget.transparent,
                                    font = widget.font,
                                    batteryState = batteryState
                                )

                                // Mini X button on simulated widget to delete it
                                IconButton(
                                    onClick = { placedWidgets.remove(widget) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                        .size(24.dp)
                                        .background(Color.Red, CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }

                // Bottom App Dock
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val apps = listOf(
                        Icons.Rounded.PhoneAndroid to "สายโทร",
                        Icons.Rounded.Settings to "ตั้งค่า",
                        Icons.Rounded.Dashboard to "ตัวประจุ"
                    )
                    apps.forEach { (icon, name) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { if (name == "ตัวประจุ") onNavigateToApp() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = name, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(name, fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
