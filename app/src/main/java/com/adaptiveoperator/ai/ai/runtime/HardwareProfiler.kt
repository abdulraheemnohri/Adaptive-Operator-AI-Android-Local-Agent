package com.adaptiveoperator.ai.ai.runtime

import android.app.ActivityManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class PerformanceProfile { BATTERY_SAVER, BALANCED, PERFORMANCE }

enum class InferenceBackend { AUTOMATIC, NPU, GPU, CPU }

data class HardwareSnapshot(
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val cpuCores: Int,
    val androidSdkInt: Int,
    val isLowRamDevice: Boolean,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val thermalStatus: Int
)

/**
 * Section 10 (Hardware Detection) + Section 11 (Runtime Backend Selection).
 *
 * This only decides the *policy* -- which backend to request and which performance
 * profile to default to. The actual fallback chain (NPU -> GPU -> CPU) is enforced by
 * GemmaEngineWrapper, which must catch native init failures and step down a tier
 * rather than trust this snapshot blindly (a device can *report* NPU libraries that
 * still fail to initialize for a given model).
 */
@Singleton
class HardwareProfiler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun snapshot(): HardwareSnapshot {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val batteryPercent = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging

        val thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            pm.currentThermalStatus
        } else 0

        return HardwareSnapshot(
            totalRamBytes = memInfo.totalMem,
            availableRamBytes = memInfo.availMem,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            androidSdkInt = Build.VERSION.SDK_INT,
            isLowRamDevice = activityManager.isLowRamDevice,
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            thermalStatus = thermalStatus
        )
    }

    fun recommendedProfile(snapshot: HardwareSnapshot = snapshot()): PerformanceProfile = when {
        snapshot.isLowRamDevice || snapshot.totalRamBytes < 6L * 1024 * 1024 * 1024 -> PerformanceProfile.BATTERY_SAVER
        snapshot.batteryPercent < 20 && !snapshot.isCharging -> PerformanceProfile.BATTERY_SAVER
        snapshot.thermalStatus >= 3 /* THERMAL_STATUS_SEVERE and above */ -> PerformanceProfile.BATTERY_SAVER
        snapshot.isCharging && snapshot.totalRamBytes >= 8L * 1024 * 1024 * 1024 -> PerformanceProfile.PERFORMANCE
        else -> PerformanceProfile.BALANCED
    }
}
