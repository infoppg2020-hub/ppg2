package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.Content
import com.example.api.GeminiApiClient
import com.example.api.GeminiRequest
import com.example.api.Part
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class WidgetStyle {
    CIRCLE_RING,
    MATERIAL_BAR,
    RETRO_NEON,
    COMPACT_GRID
}

enum class WidgetFont {
    DEFAULT,
    CYBER_GLOW,
    MONOSPACE,
    SERIF
}

class BatteryViewModel : ViewModel() {

    private val _batteryState = MutableStateFlow(BatteryState())
    val batteryState: StateFlow<BatteryState> = _batteryState.asStateFlow()

    private val _simulatorMode = MutableStateFlow(false)
    val simulatorMode: StateFlow<Boolean> = _simulatorMode.asStateFlow()

    // Real-world state stored separately so we can restore it when toggling off simulation
    private var realBatteryState = BatteryState()

    // Widget styling options
    private val _widgetStyle = MutableStateFlow(WidgetStyle.CIRCLE_RING)
    val widgetStyle: StateFlow<WidgetStyle> = _widgetStyle.asStateFlow()

    private val _widgetColor = MutableStateFlow(0xFF4CAF50) // Emerald Green default
    val widgetColor: StateFlow<Long> = _widgetColor.asStateFlow()

    private val _widgetShowTemp = MutableStateFlow(true)
    val widgetShowTemp: StateFlow<Boolean> = _widgetShowTemp.asStateFlow()

    private val _widgetShowVoltage = MutableStateFlow(true)
    val widgetShowVoltage: StateFlow<Boolean> = _widgetShowVoltage.asStateFlow()

    private val _widgetShowHealth = MutableStateFlow(true)
    val widgetShowHealth: StateFlow<Boolean> = _widgetShowHealth.asStateFlow()

    private val _widgetTransparent = MutableStateFlow(false)
    val widgetTransparent: StateFlow<Boolean> = _widgetTransparent.asStateFlow()

    private val _widgetFont = MutableStateFlow(WidgetFont.DEFAULT)
    val widgetFont: StateFlow<WidgetFont> = _widgetFont.asStateFlow()

    // AI Diagnostics Chat system
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiChatHistory = MutableStateFlow<List<Pair<String, String>>>(emptyList()) // Pair of Role ("user" or "model") to Message
    val aiChatHistory: StateFlow<List<Pair<String, String>>> = _aiChatHistory.asStateFlow()

    private val _aiError = MutableStateFlow<String?>(null)
    val aiError: StateFlow<String?> = _aiError.asStateFlow()

    private var isReceiverRegistered = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (_simulatorMode.value) return // Ignore physical updates if in simulator mode

            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = if (level != -1 && scale != -1) {
                (level * 100 / scale.toFloat()).toInt()
            } else 100

            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                              status == BatteryManager.BATTERY_STATUS_FULL

            val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val powerSource = when (chargePlug) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
                else -> "Battery"
            }

            val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            val temperature = if (tempRaw != -1) tempRaw / 10.0f else 25.0f

            val voltageRaw = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
            val voltage = if (voltageRaw != -1) {
                // Voltage can be in millivolts or volts depending on manufacturer, normalize to volts
                if (voltageRaw > 1000) voltageRaw / 1000.0f else voltageRaw.toFloat()
            } else 4.0f

            val healthRaw = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
            val health = when (healthRaw) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
                BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over-voltage"
                BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
                else -> "Good"
            }

            val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-poly"

            realBatteryState = BatteryState(
                level = batteryPct,
                isCharging = isCharging,
                temperature = temperature,
                voltage = voltage,
                health = health,
                powerSource = powerSource,
                technology = technology,
                simulated = false
            )

            _batteryState.value = realBatteryState
            triggerNativeWidgetUpdate(context)
        }
    }

    fun registerBatteryReceiver(context: Context) {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(batteryReceiver, filter)
            isReceiverRegistered = true
        }
    }

    fun unregisterBatteryReceiver(context: Context) {
        if (isReceiverRegistered) {
            context.unregisterReceiver(batteryReceiver)
            isReceiverRegistered = false
        }
    }

    fun toggleSimulatorMode(active: Boolean, context: Context) {
        _simulatorMode.value = active
        if (active) {
            // Initialize simulator values from current state
            _batteryState.value = _batteryState.value.copy(simulated = true)
        } else {
            // Restore physical state
            _batteryState.value = realBatteryState
        }
        triggerNativeWidgetUpdate(context)
    }

    fun updateSimulatedState(
        level: Int,
        isCharging: Boolean,
        temperature: Float,
        voltage: Float,
        health: String,
        powerSource: String,
        context: Context
    ) {
        if (!_simulatorMode.value) return
        _batteryState.value = BatteryState(
            level = level,
            isCharging = isCharging,
            temperature = temperature,
            voltage = voltage,
            health = health,
            powerSource = powerSource,
            technology = "Li-poly (Simulated)",
            simulated = true
        )
        triggerNativeWidgetUpdate(context)
    }

    // Widget customizer setters
    fun setWidgetStyle(style: WidgetStyle) { _widgetStyle.value = style }
    fun setWidgetColor(color: Long) { _widgetColor.value = color }
    fun setWidgetShowTemp(show: Boolean) { _widgetShowTemp.value = show }
    fun setWidgetShowVoltage(show: Boolean) { _widgetShowVoltage.value = show }
    fun setWidgetShowHealth(show: Boolean) { _widgetShowHealth.value = show }
    fun setWidgetTransparent(transparent: Boolean) { _widgetTransparent.value = transparent }
    fun setWidgetFont(font: WidgetFont) { _widgetFont.value = font }

    private fun triggerNativeWidgetUpdate(context: Context) {
        // Broadcast to update our real homescreen appwidget provider
        val intent = Intent(context, BatteryAppWidgetProvider::class.java).apply {
            action = "com.example.BATTERY_UPDATE_ACTION"
            putExtra("level", _batteryState.value.level)
            putExtra("isCharging", _batteryState.value.isCharging)
            putExtra("temperature", _batteryState.value.temperature)
            putExtra("voltage", _batteryState.value.voltage)
            putExtra("health", _batteryState.value.health)
            putExtra("powerSource", _batteryState.value.powerSource)
            putExtra("widgetStyle", _widgetStyle.value.name)
            putExtra("widgetColor", _widgetColor.value)
        }
        context.sendBroadcast(intent)
    }

    // Gemini API battery diagnostics chat
    fun askAIBatteryAdvisor(userQuestion: String, apiKey: String) {
        if (userQuestion.isBlank() || apiKey.isBlank()) return

        // Add user message to history
        val updatedHistory = _aiChatHistory.value.toMutableList().apply {
            add(Pair("user", userQuestion))
        }
        _aiChatHistory.value = updatedHistory
        _isAiLoading.value = true
        _aiError.value = null

        viewModelScope.launch {
            try {
                val state = _batteryState.value
                val systemContext = """
                    คุณคือ "ผู้เชี่ยวชาญด้านสุขภาพแบตเตอรี่อัจฉริยะ" บนสมาร์ทโฟนแอนดรอยด์
                    นี่คือข้อมูลแบตเตอรี่ในปัจจุบันของผู้ใช้ (ไม่ว่าจะเป็นค่าจริงหรือจากการจำลอง):
                    - ระดับแบตเตอรี่: ${state.level}%
                    - สถานะการชาร์จ: ${if (state.isCharging) "กำลังชาร์จ (${state.powerSource})" else "ไม่ได้ชาร์จ"}
                    - อุณหภูมิแบตเตอรี่: ${state.temperature}°C
                    - แรงดันไฟฟ้า: ${state.voltage}V
                    - สุขภาพแบตเตอรี่: ${state.healthThai} (${state.health})
                    - แหล่งพลังงานหลัก: ${state.powerSourceThai}
                    
                    คำชี้แจงในการตอบ:
                    1. ตอบเป็นภาษาไทยด้วยความสุภาพ เป็นมิตร และมีความเชี่ยวชาญสูงแบบวิศวกรเทคโนโลยีมือถือ
                    2. วิเคราะห์สภาพแบตเตอรี่ตามข้อมูลจริงข้างต้นอย่างแม่นยำ เช่น หากอุณหภูมิเกิน 45°C ให้เตือนทันทีและแนะนำวิธีแก้ไขเพื่อความปลอดภัย, หากแบตเตอรี่ต่ำกว่า 20% ให้เตือนเรื่องรอบการเสื่อมสภาพ
                    3. ให้คำแนะนำที่ลงลึก จับต้องได้ นำไปปฏิบัติตามได้จริงเพื่อยืดอายุการใช้งานแบตเตอรี่
                    4. หลีกเลี่ยงศัพท์เทคนิคที่ยากเกินเข้าใจ หากต้องใช้ให้อธิบายอย่างกระชับ
                    5. สำหรับคำถามอื่นๆ ที่ไม่เกี่ยวกับแบตเตอรี่ พยายามเชื่อมโยงให้คำตอบส่งเสริมสุขภาพแบตเตอรี่หรือให้สาระความรู้เกี่ยวกับโทรศัพท์มือถือเสมอ
                """.trimIndent()

                // Compile history into Gemini's expected Content format
                val contentsList = mutableListOf<Content>()
                
                // Add system context first
                contentsList.add(Content(listOf(Part(systemContext))))
                
                // Add historical turns
                _aiChatHistory.value.forEach { (role, msg) ->
                    val promptText = if (role == "user") "คำถามจากผู้ใช้: $msg" else "คำตอบจากคุณ: $msg"
                    contentsList.add(Content(listOf(Part(promptText))))
                }

                val request = GeminiRequest(contents = contentsList)
                val response = GeminiApiClient.service.generateContent(apiKey, request)
                
                val answer = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                    ?: "ขออภัยด้วยครับ ไม่สามารถประมวลผลคำแนะนำได้ในขณะนี้ กรุณาลองใหม่อีกครั้ง"
                
                _aiChatHistory.value = _aiChatHistory.value.toMutableList().apply {
                    add(Pair("model", answer))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _aiError.value = "ล้มเหลวในการเชื่อมต่อกับ Gemini AI: ${e.localizedMessage}"
                _aiChatHistory.value = _aiChatHistory.value.toMutableList().apply {
                    add(Pair("model", "ขออภัยครับ เกิดข้อผิดพลาดในการติดต่อระบบปัญญาประดิษฐ์ กรุณาตรวจสอบคีย์ API หรือการเชื่อมต่ออินเทอร์เน็ต"))
                }
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun clearChatHistory() {
        _aiChatHistory.value = emptyList()
    }

    fun triggerInitialDiagnosis(apiKey: String) {
        if (_aiChatHistory.value.isNotEmpty()) return
        val greetingText = when {
            _batteryState.value.temperature >= 42.0f -> {
                "วิเคราะห์สถานะสุขภาพแบตเตอรี่ของฉันตอนนี้ด่วนเลย อุณหภูมิค่อนข้างสูง!"
            }
            _batteryState.value.level <= 20 -> {
                "แบตเตอรี่ของฉันต่ำกว่า 20% แล้ว ช่วยวิเคราะห์และแนะนำการถนอมแบตเตอรี่ให้หน่อย"
            }
            else -> {
                "สวัสดีครับ ช่วยวิเคราะห์สภาพแบตเตอรี่ปัจจุบันของฉัน และให้คำแนะนำพื้นฐานในการประหยัดพลังงานและการถนอมแบตเตอรี่ให้ใช้งานได้ยาวนานที่สุดหน่อยครับ"
            }
        }
        askAIBatteryAdvisor(greetingText, apiKey)
    }
}
