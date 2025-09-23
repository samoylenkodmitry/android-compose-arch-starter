package com.archstarter.core.designsystem

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.awaitDispose
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import kotlin.math.PI
import kotlin.math.abs

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
    val context = LocalContext.current.applicationContext
    return produceState(
        initialValue = LiquidGlassTilt.Zero,
        enabled,
        context,
        view,
    ) {
        if (!enabled || view.isInEditMode) {
            value = LiquidGlassTilt.Zero
            return@produceState
        }
        val sensorManager = context.getSystemService(SensorManager::class.java)
            ?: run {
                value = LiquidGlassTilt.Zero
                return@produceState
            }
        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: run {
                value = LiquidGlassTilt.Zero
                return@produceState
            }
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        var filteredAngle = value.angle
        var filteredPitch = value.pitch
        var lastSentAngle = value.angle
        var lastSentPitch = value.pitch
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val rawPitch = orientationAngles[1].coerceIn(-MAX_PITCH, MAX_PITCH)
                val rawRoll = orientationAngles[2].coerceIn(-MAX_ROLL, MAX_ROLL)
                val normalizedPitch = rawPitch / MAX_PITCH
                val normalizedRoll = rawRoll / MAX_ROLL
                val targetAngle = normalizedRoll * MAX_SHADER_ROTATION
                val targetPitch = normalizedPitch
                filteredAngle += (targetAngle - filteredAngle) * FILTER_ALPHA
                filteredPitch += (targetPitch - filteredPitch) * FILTER_ALPHA
                if (
                    abs(filteredAngle - lastSentAngle) > ANGLE_EPSILON ||
                    abs(filteredPitch - lastSentPitch) > PITCH_EPSILON
                ) {
                    lastSentAngle = filteredAngle
                    lastSentPitch = filteredPitch
                    value = LiquidGlassTilt(filteredAngle, filteredPitch)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        val registered = sensorManager.registerListener(
            listener,
            rotationSensor,
            SensorManager.SENSOR_DELAY_GAME,
        )
        if (registered) {
            awaitDispose { sensorManager.unregisterListener(listener) }
        } else {
            sensorManager.unregisterListener(listener)
            value = LiquidGlassTilt.Zero
        }
    }.value
}

private const val FILTER_ALPHA = 0.12f
private const val ANGLE_EPSILON = 0.0005f
private const val PITCH_EPSILON = 0.0005f
private const val MAX_SHADER_ROTATION = 0.45f
private val HALF_PI = (PI / 2.0).toFloat()
private val MAX_ROLL = HALF_PI * 0.9f
private val MAX_PITCH = HALF_PI * 0.75f
