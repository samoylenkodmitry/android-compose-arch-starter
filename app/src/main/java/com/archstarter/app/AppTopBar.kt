package com.archstarter.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.archstarter.core.designsystem.LiquidGlassRect
import com.archstarter.core.designsystem.LiquidGlassRectOverlay
import com.archstarter.core.designsystem.LiquidGlassSpec

@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String = "Liquid learning",
) {
    val density = LocalDensity.current
    var barOffset by remember { mutableStateOf(Offset.Zero) }
    var barSize by remember { mutableStateOf(IntSize.Zero) }
    var accentOffset by remember { mutableStateOf(Offset.Zero) }
    var accentSize by remember { mutableStateOf(IntSize.Zero) }
    var beamOffset by remember { mutableStateOf(Offset.Zero) }
    var beamSize by remember { mutableStateOf(IntSize.Zero) }

    val barRect = remember(barOffset, barSize, density) {
        if (barSize.width == 0 || barSize.height == 0) {
            null
        } else {
            with(density) {
                LiquidGlassRect(
                    left = barOffset.x.toDp(),
                    top = barOffset.y.toDp(),
                    width = barSize.width.toDp(),
                    height = barSize.height.toDp(),
                    tintColor = Color(0x3323417A)
                )
            }
        }
    }
    val accentRect = remember(accentOffset, accentSize, density) {
        if (accentSize.width == 0 || accentSize.height == 0) {
            null
        } else {
            with(density) {
                LiquidGlassRect(
                    left = accentOffset.x.toDp(),
                    top = accentOffset.y.toDp(),
                    width = accentSize.width.toDp(),
                    height = accentSize.height.toDp(),
                    tintColor = Color(0x66F9F871)
                )
            }
        }
    }
    val beamRect = remember(beamOffset, beamSize, density) {
        if (beamSize.width == 0 || beamSize.height == 0) {
            null
        } else {
            with(density) {
                LiquidGlassRect(
                    left = beamOffset.x.toDp(),
                    top = beamOffset.y.toDp(),
                    width = beamSize.width.toDp(),
                    height = beamSize.height.toDp(),
                    tintColor = Color(0x6630E5FF)
                )
            }
        }
    }
    val glassRects = remember(barRect, accentRect, beamRect) {
        listOfNotNull(barRect, accentRect, beamRect)
    }

    LiquidGlassRectOverlay(
        rects = glassRects,
        spec = LiquidGlassSpec(
            cornerRadius = 40.dp,
            bezelWidth = 20.dp,
            displacementScalePx = 54f,
            highlight = 0.72f,
            tiltPitch = 0.12f,
        ),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(84.dp)
                    .shadow(18.dp, RoundedCornerShape(40.dp))
                    .clip(RoundedCornerShape(40.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1F2A8C),
                                Color(0xFF4C4FF0),
                                Color(0xFF00B4D8),
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(820f, 520f),
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(40.dp)
                    )
                    .onGloballyPositioned { coords ->
                        barOffset = coords.positionInRoot()
                        barSize = coords.size
                    }
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 16.dp)
                        .size(96.dp)
                        .clip(RoundedCornerShape(48.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .onGloballyPositioned { coords ->
                            accentOffset = coords.positionInRoot()
                            accentSize = coords.size
                        }
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = 16.dp)
                        .size(width = 132.dp, height = 38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .onGloballyPositioned { coords ->
                            beamOffset = coords.positionInRoot()
                            beamSize = coords.size
                        }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.24f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.app_icon),
                                contentDescription = null,
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.size(16.dp))
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFF9F871),
                                                Color(0xFFF3722C),
                                                Color(0xFFFF2D75),
                                            )
                                        ),
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.35f),
                                            offset = Offset(0f, 3f),
                                            blurRadius = 8f,
                                        )
                                    )
                                ) {
                                    append(title)
                                }
                            },
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = Color.White
                            )
                        )
                    }
                    if (subtitle.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White.copy(alpha = 0.16f))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
