package com.archstarter.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

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
internal expect fun rememberLiquidGlassTilt(enabled: Boolean): LiquidGlassTilt
