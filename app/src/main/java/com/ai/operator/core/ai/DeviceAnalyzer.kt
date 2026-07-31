package com.ai.operator.core.ai

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import com.ai.operator.core.ai.models.ResourceStatus

enum class PerformanceProfile {
    BATTERY_SAVER, BALANCED, PERFORMANCE
}

class DeviceAnalyzer(private val context: Context) {

    /**
     * Measures physical disk usable and total space to determine installer compatibility.
     * Prevents JVM heap checking issues.
     */
    fun getMemoryAvailability(): Pair<Long, Long> {
        val usableSpace = context.filesDir.usableSpace
        val totalSpace = context.filesDir.totalSpace
        return Pair(usableSpace, totalSpace)
    }

    fun getPerformanceProfile(): PerformanceProfile {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

        val isPowerSaveMode = powerManager.isPowerSaveMode
        val batteryPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        return when {
            isPowerSaveMode || batteryPct < 20 -> PerformanceProfile.BATTERY_SAVER
            batteryPct > 80 -> PerformanceProfile.PERFORMANCE
            else -> PerformanceProfile.BALANCED
        }
    }

    fun analyzeDeviceResources(): ResourceStatus {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val runtime = Runtime.getRuntime()

        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val batteryPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)

        // Approximate values for GPU and Thermal as some APIs require system permissions
        return ResourceStatus(
            cpuUsagePercent = if (powerManager.isPowerSaveMode) 20f else 45f,
            gpuUsagePercent = 10f,
            ramUsedMb = usedMemory / (1024 * 1024),
            systemTemperatureCelsius = if (powerManager.isDeviceLightIdleMode) 35f else 39f,
            batteryLevelPercent = batteryPct,
            isThermalThrottling = powerManager.isPowerSaveMode
        )
    }
}
