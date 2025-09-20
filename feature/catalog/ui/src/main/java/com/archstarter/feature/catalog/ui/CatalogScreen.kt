package com.archstarter.feature.catalog.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
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
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun CatalogScreen(
  presenter: CatalogPresenter? = null,
) {
  val p = presenter ?: rememberPresenter<CatalogPresenter, Unit>()
  val state by p.state.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  val density = LocalDensity.current
  var viewportSize by remember { mutableStateOf(IntSize.Zero) }
  val centeredItemInfo by remember {
    derivedStateOf {
      val info = listState.layoutInfo
      val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
      info.visibleItemsInfo
        .filterNot { it.key.isSpacerKey() }
        .minByOrNull {
          abs((it.offset + it.size / 2) - viewportCenter)
        }
    }
  }
  val centerPaddingDp by remember {
    derivedStateOf {
      val viewportHeight = viewportSize.height
      val itemSize = centeredItemInfo?.size ?: 0
      if (viewportHeight == 0 || itemSize == 0) {
        0.dp
      } else {
        with(density) { ((viewportHeight - itemSize) / 2f).coerceAtLeast(0f).toDp() }
      }
    }
  }
  val targetGlassRect by remember {
    derivedStateOf {
      val info = centeredItemInfo ?: return@derivedStateOf null
      if (viewportSize.width == 0 || viewportSize.height == 0) return@derivedStateOf null
      val layoutInfo = listState.layoutInfo
      val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
      val topPx = viewportCenter - info.size / 2f
      val maxTopPx = (viewportSize.height - info.size).coerceAtLeast(0)
      val clampedTopPx = topPx.coerceIn(0f, maxTopPx.toFloat())
      with(density) {
        LiquidGlassRect(
          left = 0.dp,
          top = clampedTopPx.toDp(),
          width = viewportSize.width.toDp(),
          height = info.size.toDp(),
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

  var didSnapThisInteraction by remember { mutableStateOf(false) }
  var snapInProgress by remember { mutableStateOf(false) }
  LaunchedEffect(listState) {
    snapshotFlow {
      val layoutInfo = listState.layoutInfo
      SnapFrame(
        isScrolling = listState.isScrollInProgress,
        viewportStart = layoutInfo.viewportStartOffset,
        viewportEnd = layoutInfo.viewportEndOffset,
        centeredItem = centeredItemInfo?.let { info ->
          SnapItem(index = info.index, offset = info.offset, size = info.size)
        }
      )
    }
      .distinctUntilChanged()
      .collect { frame ->
        if (frame.isScrolling) {
          if (!snapInProgress) {
            didSnapThisInteraction = false
          }
          return@collect
        }
        if (didSnapThisInteraction) return@collect
        val info = frame.centeredItem ?: return@collect
        val viewportStart = frame.viewportStart.toFloat()
        val viewportEnd = frame.viewportEnd.toFloat()
        if (viewportEnd <= viewportStart) return@collect
        val viewportCenter = (viewportStart + viewportEnd) / 2f
        val itemCenter = info.offset + info.size / 2f
        val difference = itemCenter - viewportCenter
        if (abs(difference) > 0.5f) {
          val viewportSize = viewportEnd - viewportStart
          val minTop = viewportStart
          val maxTop = (viewportStart + viewportSize - info.size).coerceAtLeast(minTop)
          val desiredTop = (viewportCenter - info.size / 2f).coerceIn(minTop, maxTop)
          val scrollOffset = (desiredTop - viewportStart).roundToInt()
          didSnapThisInteraction = true
          snapInProgress = true
          try {
            listState.animateScrollToItem(
              index = info.index,
              scrollOffset = scrollOffset,
            )
          } finally {
            snapInProgress = false
          }
        }
      }
  }

  Column(Modifier.fillMaxSize().padding(16.dp)) {
    Text("Catalog", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Button(onClick = p::onSettingsClick) { Text("Settings") }
    Spacer(Modifier.height(8.dp))
    Button(onClick = p::onRefresh) { Text("Refresh (${state.items.size})") }
    Spacer(Modifier.height(8.dp))
    Box(Modifier.weight(1f)) {
      LiquidGlassRectOverlay(
        rect = glassRect,
        modifier = Modifier
          .fillMaxSize()
          .onSizeChanged { viewportSize = it }
      ) {
        LazyColumn(
          state = listState,
        ) {
          item(key = TOP_SPACER_KEY) {
            Spacer(Modifier.height(centerPaddingDp))
          }
          state.items.forEachIndexed { index, id ->
            item(key = itemKey(id)) {
              CatalogItemCard(id = id)
            }
            if (index < state.items.lastIndex) {
              item(key = interItemSpacerKey(index)) {
                Spacer(Modifier.height(8.dp))
              }
            }
          }
          item(key = BOTTOM_SPACER_KEY) {
            Spacer(Modifier.height(centerPaddingDp))
          }
        }
      }
    }
  }
}

private const val TOP_SPACER_KEY = "top_spacer"
private const val BOTTOM_SPACER_KEY = "bottom_spacer"
private const val INTER_ITEM_SPACER_KEY_PREFIX = "between_spacer_"

private fun Any?.isSpacerKey(): Boolean {
  return this == TOP_SPACER_KEY ||
    this == BOTTOM_SPACER_KEY ||
    (this is String && startsWith(INTER_ITEM_SPACER_KEY_PREFIX))
}

private fun itemKey(id: Int): Any = id

private fun interItemSpacerKey(index: Int): String = "$INTER_ITEM_SPACER_KEY_PREFIX$index"

private data class SnapFrame(
  val isScrolling: Boolean,
  val viewportStart: Int,
  val viewportEnd: Int,
  val centeredItem: SnapItem?,
)

private data class SnapItem(
  val index: Int,
  val offset: Int,
  val size: Int,
)

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
