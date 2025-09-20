package com.archstarter.feature.catalog.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun CatalogScreen(
  presenter: CatalogPresenter? = null,
) {
  val p = presenter ?: rememberPresenter<CatalogPresenter, Unit>()
  val state by p.state.collectAsStateWithLifecycle()
  val listState = rememberLazyListState()
  val density = LocalDensity.current
  var viewportSize by remember { mutableStateOf(IntSize.Zero) }
  var bottomControlsSize by remember { mutableStateOf(IntSize.Zero) }
  val centeredItemInfo by remember {
    derivedStateOf {
      val info = listState.layoutInfo
      val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
      info.visibleItemsInfo.minByOrNull {
        abs((it.offset + it.size / 2) - viewportCenter)
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
  val bottomGlassRect = remember(bottomControlsSize, density) {
    if (bottomControlsSize.width == 0 || bottomControlsSize.height == 0) {
      null
    } else {
      val widthDp = (bottomControlsSize.width.toFloat() / density.density).dp
      val heightDp = (bottomControlsSize.height.toFloat() / density.density).dp
      LiquidGlassRect(
        left = 0.dp,
        top = 0.dp,
        width = widthDp,
        height = heightDp,
      )
    }
  }
  val listBottomPadding = 32.dp + (bottomGlassRect?.height ?: 0.dp)

  Box(Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
      Text("Catalog", style = MaterialTheme.typography.titleLarge)
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
            contentPadding = PaddingValues(bottom = listBottomPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(state.items, key = { it }) { id ->
              CatalogItemCard(id = id)
            }
          }
        }
      }
    }

    LiquidGlassRectOverlay(
      rect = bottomGlassRect,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(horizontal = 16.dp, vertical = 16.dp)
        .navigationBarsPadding()
    ) {
      Row(
        modifier = Modifier
          .padding(horizontal = 20.dp, vertical = 12.dp)
          .onSizeChanged { bottomControlsSize = it },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Button(onClick = p::onSettingsClick) { Text("Settings") }
        Button(onClick = p::onRefresh) { Text("Refresh (${state.items.size})") }
      }
    }
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
