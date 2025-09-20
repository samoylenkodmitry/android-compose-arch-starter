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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

private const val MAX_GLASSES = 6

private const val LIQUID_GLASS_AGSL = """
uniform shader background;

uniform float2 u_size;
uniform float  u_rectCount;
uniform float4 u_rects[${MAX_GLASSES}];
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
    float bestSdf = 1e9;
    float2 bestCenter = float2(0.0);
    float2 bestHalfWH = float2(0.0);
    bool hasHit = false;

    for (int i = 0; i < ${MAX_GLASSES}; ++i) {
        if (float(i) >= u_rectCount) {
            break;
        }
        float4 packed = u_rects[i];
        float2 center = packed.xy;
        float2 halfWH = 0.5 * packed.zw;
        float2 p = coord - center;
        float sdf = sdRoundRect(p, halfWH, u_radius);
        if (sdf <= 0.0 && sdf < bestSdf) {
            bestSdf = sdf;
            bestCenter = center;
            bestHalfWH = halfWH;
            hasHit = true;
        }
    }

    if (!hasHit) {
        return background.eval(coord);
    }

    float2 p = coord - bestCenter;
    float eps = 1.0;
    float sx = sdRoundRect(p + float2(eps, 0.0), bestHalfWH, u_radius) -
               sdRoundRect(p - float2(eps, 0.0), bestHalfWH, u_radius);
    float sy = sdRoundRect(p + float2(0.0, eps), bestHalfWH, u_radius) -
               sdRoundRect(p - float2(0.0, eps), bestHalfWH, u_radius);
    float2 n = normalize(float2(sx, sy));
    float2 inwardN = -n;
    float d = clamp(-bestSdf, 0.0, u_bezel);
    float x = (u_bezel > 0.0) ? (d / u_bezel) : 0.0;
    float slope = dHeightDx(x, u_profile);
    float bend = slope * (1.0 - 1.0 / max(u_ri, 1.0001));
    float2 disp = inwardN * (u_scale * bend);
    float2 sampleCoord = clamp(coord + disp, float2(0.5), u_size - float2(0.5));
    half4 refr = background.eval(sampleCoord);
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

@Stable
data class LiquidGlassRect(
    val left: Dp,
    val top: Dp,
    val width: Dp,
    val height: Dp,
) {
    val isEmpty: Boolean get() = width <= 0.dp || height <= 0.dp
}

@Composable
fun LiquidGlassRectOverlay(
    rect: LiquidGlassRect?,
    modifier: Modifier = Modifier,
    spec: LiquidGlassSpec = LiquidGlassSpec(),
    content: @Composable BoxScope.() -> Unit,
) {
    LiquidGlassRectOverlay(
        rects = rect?.let(::listOf).orEmpty(),
        modifier = modifier,
        spec = spec,
        content = content,
    )
}

@Composable
fun LiquidGlassRectOverlay(
    rects: List<LiquidGlassRect>,
    modifier: Modifier = Modifier,
    spec: LiquidGlassSpec = LiquidGlassSpec(),
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    var containerSize by remember { mutableStateOf(Size.Zero) }
    val shader = remember {
        if (Build.VERSION.SDK_INT >= 33) RuntimeShader(LIQUID_GLASS_AGSL) else null
    }
    val rectsPx = remember(rects, density) {
        rects
            .filterNot { it.isEmpty }
            .map { it.toRect(density) }
    }

    LaunchedEffect(rectsPx, containerSize, spec, density, shader) {
        if (Build.VERSION.SDK_INT < 33) return@LaunchedEffect
        val runtimeShader = shader ?: return@LaunchedEffect
        if (rectsPx.isEmpty()) return@LaunchedEffect
        if (containerSize.width <= 0f || containerSize.height <= 0f) return@LaunchedEffect
        val corner = with(density) { spec.cornerRadius.toPx() }
        val bezel = with(density) { spec.bezelWidth.toPx() }
        runtimeShader.setFloatUniform("u_size", containerSize.width, containerSize.height)
        val count = rectsPx.size.coerceAtMost(MAX_GLASSES)
        runtimeShader.setFloatUniform("u_rectCount", count.toFloat())
        val rectBuffer = FloatArray(MAX_GLASSES * 4)
        for (i in 0 until count) {
            val rect = rectsPx[i]
            val base = i * 4
            rectBuffer[base] = rect.center.x
            rectBuffer[base + 1] = rect.center.y
            rectBuffer[base + 2] = rect.width
            rectBuffer[base + 3] = rect.height
        }
        runtimeShader.setFloatUniform("u_rects", rectBuffer)
        runtimeShader.setFloatUniform("u_radius", corner)
        runtimeShader.setFloatUniform("u_bezel", max(1f, bezel))
        runtimeShader.setFloatUniform("u_scale", spec.displacementScalePx)
        runtimeShader.setFloatUniform("u_ri", spec.refractiveIndex)
        runtimeShader.setFloatUniform("u_profile", spec.profile)
        runtimeShader.setFloatUniform("u_highlight", spec.highlight)
    }

    val renderEffect = remember(rectsPx, shader) {
        if (Build.VERSION.SDK_INT >= 33 && shader != null && rectsPx.isNotEmpty()) {
            RenderEffect
                .createRuntimeShaderEffect(shader, "background")
                .asComposeRenderEffect()
        } else {
            null
        }
    }

    val layerModifier = if (renderEffect != null) {
        Modifier.graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            this.renderEffect = renderEffect
            clip = false
        }
    } else {
        Modifier
    }

    Box(
        modifier
            .onSizeChanged { containerSize = Size(it.width.toFloat(), it.height.toFloat()) }
            .then(layerModifier)
            .drawWithContent {
                drawContent()
                if ((Build.VERSION.SDK_INT < 33 || shader == null) && rectsPx.isNotEmpty()) {
                    drawFallbackGlass(rectsPx, spec)
                }
            }
    ) {
        content()
    }
}

private fun LiquidGlassRect.toRect(density: Density): Rect = with(density) {
    val leftPx = left.toPx()
    val topPx = top.toPx()
    val widthPx = width.toPx()
    val heightPx = height.toPx()
    Rect(leftPx, topPx, leftPx + widthPx, topPx + heightPx)
}

private fun DrawScope.drawFallbackGlass(rects: List<Rect>, spec: LiquidGlassSpec) {
    val corner = androidx.compose.ui.geometry.CornerRadius(spec.cornerRadius.toPx())
    rects.forEach { rect ->
        val topLeft = Offset(rect.left, rect.top)
        val size = Size(rect.width, rect.height)
        drawRoundRect(
            color = Color.White.copy(alpha = 0.06f),
            topLeft = topLeft,
            size = size,
            cornerRadius = corner
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f),
            topLeft = topLeft,
            size = size,
            cornerRadius = corner
        )
    }
}

