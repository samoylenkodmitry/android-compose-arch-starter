package com.archstarter.core.designsystem

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

private const val LIQUID_GLASS_AGSL = """
uniform shader background;

uniform float2 u_size;
uniform float2 u_center;
uniform float2 u_rectSize;
uniform float  u_radius;
uniform float  u_bezel;
uniform float  u_scale;
uniform float  u_ri;
uniform float  u_profile;
uniform float  u_highlight;

float sdRoundRect(float2 p, float2 halfWH, float r) {
    float2 q = abs(p) - (halfWH - float2(r));
    return length(max(q, 0.0)) - r + min(max(q.x, q.y), 0.0);
}

float heightProfile(float x, float profile) {
    float hCircle   = x * x;
    float hSquircle = 1.0 - pow(1.0 - x, 4.0);
    return mix(hCircle, hSquircle, clamp(profile, 0.0, 1.0));
}

float dHeightDx(float x, float profile) {
    float dCircle   = 2.0 * x;
    float dSquircle = 4.0 * pow(1.0 - x, 3.0);
    return mix(dCircle, dSquircle, clamp(profile, 0.0, 1.0));
}

half4 main(float2 coord) {
    float2 p = coord - u_center;
    float2 halfWH = 0.5 * u_rectSize;
    float sdf = sdRoundRect(p, halfWH, u_radius);
    if (sdf > 0.0) {
        return background.eval(coord);
    }
    float eps = 1.0;
    float sx = sdRoundRect(p + float2(eps, 0.0), halfWH, u_radius) -
               sdRoundRect(p - float2(eps, 0.0), halfWH, u_radius);
    float sy = sdRoundRect(p + float2(0.0, eps), halfWH, u_radius) -
               sdRoundRect(p - float2(0.0, eps), halfWH, u_radius);
    float2 n = normalize(float2(sx, sy));
    float2 inwardN = -n;
    float d = clamp(-sdf, 0.0, u_bezel);
    float x = (u_bezel > 0.0) ? (d / u_bezel) : 0.0;
    float slope = dHeightDx(x, u_profile);
    float bend = slope * (1.0 - 1.0 / max(u_ri, 1.0001));
    float2 disp = inwardN * (u_scale * bend);
    half4 refr = background.eval(coord + disp);
    float rim = pow(1.0 - x, 3.0);
    half specA = half(clamp(u_highlight * rim, 0.0, 1.0));
    half4 spec = half4(1.0, 1.0, 1.0, specA);
    half4 base = refr;
    half4 outc = base + spec * spec.a * 0.85;
    return outc;
}
"""

@Stable
data class LiquidGlassSpec(
    val cornerRadius: Dp = 28.dp,
    val bezelWidth: Dp = 14.dp,
    val displacementScalePx: Float = 44f,
    val refractiveIndex: Float = 1.5f,
    val profile: Float = 1f,
    val highlight: Float = 0.6f,
)

@Composable
fun LiquidGlassBox(
    modifier: Modifier = Modifier,
    spec: LiquidGlassSpec = LiquidGlassSpec(),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val density = LocalDensity.current
    var sizePx by remember { mutableStateOf(Size.Zero) }
    val shader = remember {
        if (Build.VERSION.SDK_INT >= 33) RuntimeShader(LIQUID_GLASS_AGSL) else null
    }

    fun updateUniforms() {
        if (Build.VERSION.SDK_INT < 33 || shader == null) return
        val w = sizePx.width
        val h = sizePx.height
        if (w <= 0f || h <= 0f) return
        val cx = w / 2f
        val cy = h / 2f
        with(density) {
            val corner = spec.cornerRadius.toPx()
            val bezel = spec.bezelWidth.toPx()
            shader.setFloatUniform("u_size", w, h)
            shader.setFloatUniform("u_center", cx, cy)
            shader.setFloatUniform("u_rectSize", w, h)
            shader.setFloatUniform("u_radius", corner)
            shader.setFloatUniform("u_bezel", max(1f, bezel))
            shader.setFloatUniform("u_scale", spec.displacementScalePx)
            shader.setFloatUniform("u_ri", spec.refractiveIndex)
            shader.setFloatUniform("u_profile", spec.profile)
            shader.setFloatUniform("u_highlight", spec.highlight)
        }
    }

    LaunchedEffect(sizePx, spec) { updateUniforms() }

    if (Build.VERSION.SDK_INT >= 33 && shader != null) {
        Box(
            modifier
                .onSizeChanged { sizePx = Size(it.width.toFloat(), it.height.toFloat()) }
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    renderEffect = RenderEffect
                        .createRuntimeShaderEffect(shader, "background")
                        .asComposeRenderEffect()
                    clip = false
                }
        ) {
            content()
        }
    } else {
        Box(
            modifier
                .onSizeChanged { sizePx = Size(it.width.toFloat(), it.height.toFloat()) }
                .drawBehind { drawFallbackGlass(spec) }
        ) {
            content()
        }
    }
}

private fun DrawScope.drawFallbackGlass(spec: LiquidGlassSpec) {
    val corner = androidx.compose.ui.geometry.CornerRadius(spec.cornerRadius.toPx())
    drawRoundRect(
        color = Color.White.copy(alpha = 0.06f),
        cornerRadius = corner
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.10f),
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = corner
    )
}

