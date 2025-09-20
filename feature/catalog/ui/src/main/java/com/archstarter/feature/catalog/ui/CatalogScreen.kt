package com.archstarter.feature.catalog.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archstarter.core.common.presenter.rememberPresenter
import com.archstarter.core.designsystem.AppTheme
import com.archstarter.core.designsystem.LiquidGlassRect
import com.archstarter.core.designsystem.LiquidGlassRectOverlay
import com.archstarter.feature.catalog.api.CatalogPresenter
import com.archstarter.feature.catalog.api.CatalogState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.min

private const val TOP_SPACER_KEY = "catalog_top_spacer"
private const val BOTTOM_SPACER_KEY = "catalog_bottom_spacer"

@Composable
fun CatalogScreen(
  presenter: CatalogPresenter? = null,
) {
  val p = presenter ?: rememberPresenter<CatalogPresenter, Unit>()
  val state by p.state.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  val flingBehavior = rememberSnapFlingBehavior(
    lazyListState = listState,
    snapPosition = SnapPosition.Center
  )
  val density = LocalDensity.current
  var viewportSize by remember { mutableStateOf(IntSize.Zero) }
  var viewportOffset by remember { mutableStateOf(Offset.Zero) }
  var bottomControlsHeight by remember { mutableStateOf(0.dp) }
  var settingsButtonOffset by remember { mutableStateOf<Offset?>(null) }
  var refreshButtonOffset by remember { mutableStateOf<Offset?>(null) }
  var settingsButtonSize by remember { mutableStateOf<IntSize?>(null) }
  var refreshButtonSize by remember { mutableStateOf<IntSize?>(null) }
  val centeredItemInfo by remember {
    derivedStateOf {
      val info = listState.layoutInfo
      val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
      val visibleItems = info.visibleItemsInfo.filterNot { item ->
        item.key == TOP_SPACER_KEY || item.key == BOTTOM_SPACER_KEY
      }
      if (visibleItems.isEmpty()) {
        null
      } else {
        visibleItems.minByOrNull {
          abs((it.offset + it.size / 2) - viewportCenter)
        }
      }
    }
  }
  val centerGlassTint = MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
  val targetGlassRect by remember(centerGlassTint) {
    derivedStateOf {
      val info = centeredItemInfo ?: return@derivedStateOf null
      if (viewportSize.width == 0 || viewportSize.height == 0) return@derivedStateOf null
      val layoutInfo = listState.layoutInfo
      val viewportStart = layoutInfo.viewportStartOffset
      val viewportEnd = layoutInfo.viewportEndOffset
      val viewportHeightPx = viewportEnd - viewportStart
      if (viewportHeightPx <= 0) return@derivedStateOf null
      val rawTopPx = (info.offset - viewportStart).toFloat()
      val maxTopPx = (viewportHeightPx - info.size).coerceAtLeast(0)
      val clampedTopPx = rawTopPx.coerceIn(0f, maxTopPx.toFloat())
      with(density) {
        LiquidGlassRect(
          left = 0.dp,
          top = clampedTopPx.toDp(),
          width = viewportSize.width.toDp(),
          height = info.size.toDp(),
          tintColor = centerGlassTint,
        )
      }
    }
  }
  val glassTop by animateDpAsState(
    targetValue = targetGlassRect?.top ?: 0.dp,
    animationSpec = tween(durationMillis = 300, delayMillis = 100),
    label = "glassTop"
  )
  val glassHeight by animateDpAsState(
    targetValue = targetGlassRect?.height ?: 0.dp,
    animationSpec = tween(durationMillis = 300, delayMillis = 100),
    label = "glassHeight"
  )
  val glassWidth by animateDpAsState(
    targetValue = targetGlassRect?.width ?: 0.dp,
    animationSpec = tween(durationMillis = 300, delayMillis = 100),
    label = "glassWidth"
  )
  val glassRect = targetGlassRect?.let { rect ->
    if (glassWidth > 0.dp && glassHeight > 0.dp) {
      rect.copy(top = glassTop, width = glassWidth, height = glassHeight)
    } else {
      null
    }
  }
  val centerGlassRect = remember(glassRect, viewportOffset, density) {
    glassRect?.let { rect ->
      val offsetX = with(density) { viewportOffset.x.toDp() }
      val offsetY = with(density) { viewportOffset.y.toDp() }
      rect.copy(left = rect.left + offsetX, top = rect.top + offsetY)
    }
  }
  val buttonGlassTint = Color(0xFF98A9CF)
  val settingsGlassRect = remember(settingsButtonOffset, settingsButtonSize, density, buttonGlassTint) {
    val buttonOffset = settingsButtonOffset
    val buttonSize = settingsButtonSize
    if (buttonOffset == null || buttonSize == null) {
      null
    } else {
      with(density) {
        LiquidGlassRect(
          left = buttonOffset.x.toDp(),
          top = buttonOffset.y.toDp(),
          width = buttonSize.width.toDp(),
          height = buttonSize.height.toDp(),
          tintColor = buttonGlassTint,
        )
      }
    }
  }
  val refreshGlassRect = remember(refreshButtonOffset, refreshButtonSize, density, buttonGlassTint) {
    val buttonOffset = refreshButtonOffset
    val buttonSize = refreshButtonSize
    if (buttonOffset == null || buttonSize == null) {
      null
    } else {
      with(density) {
        LiquidGlassRect(
          left = buttonOffset.x.toDp(),
          top = buttonOffset.y.toDp(),
          width = buttonSize.width.toDp(),
          height = buttonSize.height.toDp(),
          tintColor = buttonGlassTint,
        )
      }
    }
  }
  val listBottomPadding = 32.dp + bottomControlsHeight
  val itemSpacing = 8.dp
  val centerItemPadding = run {
    val viewportHeightPx = viewportSize.height
    val itemHeightPx = centeredItemInfo?.size ?: 0
    if (viewportHeightPx <= 0 || itemHeightPx <= 0) {
      0.dp
    } else {
      val extraPx = (viewportHeightPx / 2f - itemHeightPx / 2f).coerceAtLeast(0f)
      with(density) { extraPx.toDp() }
    }
  }
  val edgeSpacerHeight = if (centerItemPadding > itemSpacing) {
    centerItemPadding - itemSpacing
  } else {
    0.dp
  }
  val glassRects = remember(centerGlassRect, settingsGlassRect, refreshGlassRect) {
    listOfNotNull(centerGlassRect, settingsGlassRect, refreshGlassRect)
  }

  Box(Modifier.fillMaxSize()) {
    LiquidGlassRectOverlay(
      rects = glassRects,
      modifier = Modifier.fillMaxSize()
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp, vertical = 16.dp)
      ) {
        Box(
          modifier = Modifier
            .weight(1f)
            .onGloballyPositioned { coords -> viewportOffset = coords.positionInRoot() }
            .onSizeChanged { viewportSize = it }
        ) {
          LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(bottom = listBottomPadding),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
          ) {
            item(key = TOP_SPACER_KEY) {
              GwernDecoratedSpacer(
                height = edgeSpacerHeight,
                isTop = true,
              )
            }
            items(state.items, key = { it }) { id ->
              CatalogItemCard(id = id)
            }
            item(key = BOTTOM_SPACER_KEY) {
              GwernDecoratedSpacer(
                height = edgeSpacerHeight,
                isTop = false,
              )
            }
          }
        }
      }
    }

    val glassPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
    val transparentButtonColors = ButtonDefaults.buttonColors(
      containerColor = Color.Transparent,
      contentColor = MaterialTheme.colorScheme.onSurface,
      disabledContainerColor = Color.Transparent,
      disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    )
    val transparentButtonElevation = ButtonDefaults.buttonElevation(
      defaultElevation = 0.dp,
      pressedElevation = 0.dp,
      focusedElevation = 0.dp,
      hoveredElevation = 0.dp,
    )
    Row(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .navigationBarsPadding()
        .onSizeChanged { size ->
          bottomControlsHeight = with(density) { size.height.toDp() }
        },
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier.onGloballyPositioned { coords ->
          settingsButtonOffset = coords.positionInRoot()
          settingsButtonSize = coords.size
        }
      ) {
        Box(modifier = Modifier.padding(glassPadding)) {
          Button(
            onClick = p::onSettingsClick,
            colors = transparentButtonColors,
            elevation = transparentButtonElevation,
          ) { Text("Settings", color = MaterialTheme.colorScheme.inverseOnSurface) }
        }
      }
      Box(
        modifier = Modifier.onGloballyPositioned { coords ->
          refreshButtonOffset = coords.positionInRoot()
          refreshButtonSize = coords.size
        }
      ) {
        Box(modifier = Modifier.padding(glassPadding)) {
          Button(
            onClick = p::onRefresh,
            colors = transparentButtonColors,
            elevation = transparentButtonElevation,
          ) { Text("Refresh (${state.items.size})", color = MaterialTheme.colorScheme.inverseOnSurface) }
        }
      }
    }
  }
}

@Composable
private fun GwernDecoratedSpacer(
  height: Dp,
  isTop: Boolean,
  modifier: Modifier = Modifier,
) {
  val coercedHeight = height.coerceAtLeast(0.dp)
  if (coercedHeight <= 0.dp) {
    Spacer(modifier.height(coercedHeight))
    return
  }

  val density = LocalDensity.current
  val accentColor = Color(0xFFAD8A4A)
  val backgroundTop = Color(0xFFF7F5EF)
  val backgroundBottom = Color(0xFFF0EFEA)
  val borderColor = Color(0xFF9C9C9C)
  val stripeColor = accentColor.copy(alpha = 0.18f)

  Canvas(
    modifier
      .fillMaxWidth()
      .height(coercedHeight)
  ) {
    val widthPx = size.width
    val heightPx = size.height
    val backgroundBrush = if (isTop) {
      Brush.verticalGradient(listOf(backgroundBottom, backgroundTop))
    } else {
      Brush.verticalGradient(listOf(backgroundTop, backgroundBottom))
    }
    drawRect(brush = backgroundBrush)

    val stripeSpacing = with(density) { 22.dp.toPx() }
    val stripeStrokeWidth = with(density) { 0.75.dp.toPx() }
    if (stripeSpacing > 0f && stripeStrokeWidth > 0f) {
      var startX = -heightPx
      while (startX < widthPx + heightPx) {
        val startY = if (isTop) heightPx else 0f
        val endY = if (isTop) 0f else heightPx
        drawLine(
          color = stripeColor,
          start = Offset(startX, startY),
          end = Offset(startX + heightPx, endY),
          strokeWidth = stripeStrokeWidth,
        )
        startX += stripeSpacing
      }
    }

    val accentStrokeWidth = with(density) { 1.25.dp.toPx() }
    val accentMargin = min(heightPx / 3f, with(density) { 12.dp.toPx() })
    val accentY = if (isTop) heightPx - accentMargin else accentMargin
    drawLine(
      color = accentColor,
      start = Offset(0f, accentY),
      end = Offset(widthPx, accentY),
      strokeWidth = accentStrokeWidth,
    )

    val dotRadius = min(with(density) { 2.5.dp.toPx() }, accentMargin / 1.8f)
    val dotSpacing = with(density) { 20.dp.toPx() }
    if (dotSpacing > 0f && dotRadius > 0f) {
      var x = dotSpacing / 2f
      while (x < widthPx) {
        drawCircle(
          color = accentColor.copy(alpha = 0.85f),
          radius = dotRadius,
          center = Offset(x, accentY),
        )
        drawCircle(
          color = Color(0xFFF9F7F1),
          radius = dotRadius * 0.45f,
          center = Offset(x, accentY),
        )
        x += dotSpacing
      }
    }

    val borderStrokeWidth = with(density) { 1.dp.toPx() }
    val borderY = if (isTop) {
      heightPx - borderStrokeWidth / 2f
    } else {
      borderStrokeWidth / 2f
    }
    drawLine(
      color = borderColor,
      start = Offset(0f, borderY),
      end = Offset(widthPx, borderY),
      strokeWidth = borderStrokeWidth,
    )
  }
}

// ---- Fake for preview (no Hilt in UI module) ----
private class FakeCatalogPresenter : CatalogPresenter {
  private val _s = MutableStateFlow(CatalogState(listOf(1)))
  override val state: StateFlow<CatalogState> = _s
  override fun onRefresh() { _s.value = _s.value.copy(items = _s.value.items + (_s.value.items.size+1)) }
  override fun onItemClick(id: Int) {}
  override fun onSettingsClick() {}
  override fun initOnce(params: Unit) {}
}

@Preview(showBackground = true)
@Composable
private fun PreviewCatalog() {
  AppTheme { CatalogScreen(presenter = FakeCatalogPresenter()) }
}
