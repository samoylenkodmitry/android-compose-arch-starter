package com.archstarter.core.designsystem

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Stable
internal data class LiquidGlassTilt(
    val angle: Float,
    val pitch: Float,
) {
    companion object {
        val Zero = LiquidGlassTilt(angle = 0f, pitch = 0f)
    }
}

@Composable
internal fun rememberLiquidGlassTilt(enabled: Boolean): LiquidGlassTilt {
    val view = LocalView.current
    val context = LocalContext.current
    val tiltProcessor = remember { LiquidGlassTiltProcessor() }
    var tilt by remember { mutableStateOf(LiquidGlassTilt.Zero) }
    DisposableEffect(enabled, context, view) {
        tiltProcessor.reset(LiquidGlassTilt.Zero)
        tilt = LiquidGlassTilt.Zero
        if (!enabled || view.isInEditMode) {
            return@DisposableEffect onDispose { }
        }
        val sensorManager = context.getSystemService(SensorManager::class.java)
        if (sensorManager == null) {
            return@DisposableEffect onDispose { }
        }
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationSensor == null) {
            return@DisposableEffect onDispose { }
        }
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val rawPitch = orientationAngles[0]
                val rawRoll = orientationAngles[1]
                tiltProcessor.update(rawRoll, rawPitch)?.let { newTilt ->
                    tilt = newTilt
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val registered = sensorManager.registerListener(
            listener,
            rotationSensor,
            SensorManager.SENSOR_DELAY_GAME,
        )
        if (!registered) {
            sensorManager.unregisterListener(listener)
            tiltProcessor.reset(LiquidGlassTilt.Zero)
            tilt = LiquidGlassTilt.Zero
            return@DisposableEffect onDispose { }
        }
        onDispose {
            tiltProcessor.reset(LiquidGlassTilt.Zero)
            sensorManager.unregisterListener(listener)
        }
    }
    return tilt
}
