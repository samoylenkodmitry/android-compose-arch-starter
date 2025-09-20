package com.archstarter.core.designsystem

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

private const val MAX_GLASSES = 6

private const val LIQUID_GLASS_AGSL = """
uniform shader background;
uniform shader blurred;

uniform float2 u_size;
uniform float2 u_center;
uniform float2 u_rectSize;
uniform float  u_radius;
uniform float  u_bezel;
uniform float  u_scale;
uniform float  u_ri;
uniform float  u_profile;
uniform float  u_highlight;
uniform float4 u_tintColor;
uniform float  u_blurRadius;

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
    float2 sampleCoord = clamp(coord + disp, float2(0.5), u_size - float2(0.5));
    half4 refr = background.eval(sampleCoord);
    float rim = pow(1.0 - x, 3.0);
    half specA = half(clamp(u_highlight * rim, 0.0, 1.0));
    half4 spec = half4(1.0, 1.0, 1.0, specA);
    half4 base = refr;
    float blurPx = clamp(u_blurRadius, 0.0, 80.0);
    if (blurPx > 0.001) {
        half4 blurSample = blurred.eval(sampleCoord);
        float blurStrength = clamp(blurPx / (blurPx + 8.0), 0.0, 0.85);
        base = mix(base, blurSample, blurStrength);
    }
    half4 outc = base + spec * spec.a * 0.85;
    half tintA = half(clamp(u_tintColor.w, 0.0, 1.0));
    half3 tintRgb = half3(u_tintColor.rgb);
    outc.rgb = mix(outc.rgb, tintRgb, tintA);
    return outc;
}
"""

private const val LIQUID_GLASS_BLUR_AGSL = """
uniform shader background;

uniform float2 u_size;
uniform float  u_blurRadius;

half4 main(float2 coord) {
    float blurPx = clamp(u_blurRadius, 0.0, 80.0);
    if (blurPx <= 0.001) {
        return background.eval(coord);
    }
    float2 clampMin = float2(0.5);
    float2 clampMax = u_size - float2(0.5);
    float2 baseCoord = clamp(coord, clampMin, clampMax);
    float sampleStep = max(0.5, blurPx * 0.4);
    float diagStep = sampleStep * 0.70710678;
    half4 center = background.eval(baseCoord);
    half4 axisSum = half4(0.0);
    axisSum += background.eval(clamp(baseCoord + float2( sampleStep, 0.0), clampMin, clampMax));
    axisSum += background.eval(clamp(baseCoord + float2(-sampleStep, 0.0), clampMin, clampMax));
    axisSum += background.eval(clamp(baseCoord + float2(0.0,  sampleStep), clampMin, clampMax));
    axisSum += background.eval(clamp(baseCoord + float2(0.0, -sampleStep), clampMin, clampMax));
    half4 diagSum = half4(0.0);
    diagSum += background.eval(clamp(baseCoord + float2( diagStep,  diagStep), clampMin, clampMax));
    diagSum += background.eval(clamp(baseCoord + float2(-diagStep,  diagStep), clampMin, clampMax));
    diagSum += background.eval(clamp(baseCoord + float2( diagStep, -diagStep), clampMin, clampMax));
    diagSum += background.eval(clamp(baseCoord + float2(-diagStep, -diagStep), clampMin, clampMax));
    half4 blur = center * 4.0;
    blur += axisSum * 2.0;
    blur += diagSum;
    blur *= half(1.0 / 16.0);
    return blur;
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
    val tintColor: Color = Color.Transparent,
    val blurRadius: Dp = 0.dp,
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
    var overlayOffset by remember { mutableStateOf(Offset.Zero) }
    val rectsPx = remember(rects, density, overlayOffset) {
        rects
            .filterNot { it.isEmpty }
            .map { it.toRuntimeRect(density, overlayOffset) }
    }
    val renderEffect = remember(rectsPx, containerSize, spec, density) {
        if (Build.VERSION.SDK_INT >= 33) {
            createRuntimeEffect(rectsPx, containerSize, spec, density)?.asComposeRenderEffect()
        } else {
            null
        }
    }

    val hasRuntimeEffect = Build.VERSION.SDK_INT >= 33 && renderEffect != null

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
            .onGloballyPositioned { overlayOffset = it.positionInRoot() }
            .then(layerModifier)
            .drawWithContent {
                drawContent()
                if (!hasRuntimeEffect && rectsPx.isNotEmpty()) {
                    drawFallbackGlass(rectsPx, spec)
                }
            }
    ) {
        content()
    }
}

private data class LiquidGlassRuntimeRect(
    val rect: Rect,
    val tintColor: Color,
    val blurRadiusPx: Float,
)

private fun LiquidGlassRect.toRuntimeRect(
    density: Density,
    overlayOffset: Offset,
): LiquidGlassRuntimeRect = with(density) {
    val widthPx = width.toPx()
    val heightPx = height.toPx()
    val leftPx = left.toPx() - overlayOffset.x
    val topPx = top.toPx() - overlayOffset.y
    val blurPx = if (blurRadius > 0.dp) blurRadius.toPx() else 0f
    LiquidGlassRuntimeRect(
        rect = Rect(leftPx, topPx, leftPx + widthPx, topPx + heightPx),
        tintColor = tintColor,
        blurRadiusPx = blurPx,
    )
}

private fun DrawScope.drawFallbackGlass(rects: List<LiquidGlassRuntimeRect>, spec: LiquidGlassSpec) {
    val corner = androidx.compose.ui.geometry.CornerRadius(spec.cornerRadius.toPx())
    rects.forEach { entry ->
        val rect = entry.rect
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
        if (entry.tintColor.alpha > 0f) {
            drawRoundRect(
                color = entry.tintColor,
                topLeft = topLeft,
                size = size,
                cornerRadius = corner
            )
        }
    }
}

private fun createRuntimeEffect(
    rects: List<LiquidGlassRuntimeRect>,
    containerSize: Size,
    spec: LiquidGlassSpec,
    density: Density,
): RenderEffect? {
    if (rects.isEmpty()) return null
    if (containerSize.width <= 0f || containerSize.height <= 0f) return null

    val corner = with(density) { spec.cornerRadius.toPx() }
    val bezel = max(1f, with(density) { spec.bezelWidth.toPx() })

    var chainedEffect: RenderEffect? = null
    rects.take(MAX_GLASSES).forEach { runtimeRect ->
        val rect = runtimeRect.rect
        val shader = RuntimeShader(LIQUID_GLASS_AGSL)
        shader.setFloatUniform("u_size", containerSize.width, containerSize.height)
        shader.setFloatUniform("u_center", rect.center.x, rect.center.y)
        shader.setFloatUniform("u_rectSize", rect.width, rect.height)
        shader.setFloatUniform("u_radius", corner)
        shader.setFloatUniform("u_bezel", bezel)
        shader.setFloatUniform("u_scale", spec.displacementScalePx)
        shader.setFloatUniform("u_ri", spec.refractiveIndex)
        shader.setFloatUniform("u_profile", spec.profile)
        shader.setFloatUniform("u_highlight", spec.highlight)
        shader.setFloatUniform(
            "u_tintColor",
            runtimeRect.tintColor.red,
            runtimeRect.tintColor.green,
            runtimeRect.tintColor.blue,
            runtimeRect.tintColor.alpha,
        )
        shader.setFloatUniform("u_blurRadius", runtimeRect.blurRadiusPx)

        val blurShader = RuntimeShader(LIQUID_GLASS_BLUR_AGSL)
        blurShader.setFloatUniform("u_size", containerSize.width, containerSize.height)
        blurShader.setFloatUniform("u_blurRadius", runtimeRect.blurRadiusPx)
        shader.setInputShader("blurred", blurShader)

        val effect = RenderEffect.createRuntimeShaderEffect(shader, "background")
        chainedEffect = if (chainedEffect == null) {
            effect
        } else {
            RenderEffect.createChainEffect(effect, chainedEffect)
        }
    }

    return chainedEffect
}

