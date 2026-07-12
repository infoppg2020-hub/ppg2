package com.example

data class BatteryState(
    val level: Int = 100,             // 0 to 100
    val isCharging: Boolean = false,
    val temperature: Float = 25.0f,    // In Celsius
    val voltage: Float = 4.2f,        // In Volts
    val health: String = "Good",       // Good, Overheat, Dead, Over-voltage, Cold, Unknown
    val powerSource: String = "Battery",// AC, USB, Wireless, Battery
    val technology: String = "Li-poly",
    val simulated: Boolean = false
) {
    val healthThai: String
        get() = when (health) {
            "Good" -> "ดีมาก"
            "Overheat" -> "ร้อนจัด (ร้อนเกินไป)"
            "Dead" -> "เสื่อมสภาพ"
            "Over-voltage" -> "แรงดันไฟสูงเกินไป"
            "Cold" -> "เย็นจัด"
            else -> "ปกติ"
        }

    val powerSourceThai: String
        get() = when (powerSource) {
            "AC" -> "สายชาร์จบ้าน (AC)"
            "USB" -> "สายชาร์จ USB"
            "Wireless" -> "ชาร์จไร้สาย (Wireless)"
            else -> "กำลังใช้แบตเตอรี่"
        }
}
