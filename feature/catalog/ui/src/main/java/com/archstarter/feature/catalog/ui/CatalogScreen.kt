package com.archstarter.feature.catalog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archstarter.core.common.presenter.rememberPresenter
import com.archstarter.core.designsystem.AppTheme
import com.archstarter.core.designsystem.LiquidGlassBox
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
  val centeredHeight by remember {
    derivedStateOf {
      val info = listState.layoutInfo
      val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
      val item = info.visibleItemsInfo.minByOrNull {
        abs((it.offset + it.size / 2) - viewportCenter)
      }
      item?.size?.let { with(density) { it.toDp() } } ?: 0.dp
    }
  }
  val glassHeight by animateDpAsState(
    targetValue = centeredHeight,
    animationSpec = tween(durationMillis = 300, delayMillis = 100),
    label = "glassHeight"
  )

  Column(Modifier.fillMaxSize().padding(16.dp)) {
    Text("Catalog", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Button(onClick = p::onSettingsClick) { Text("Settings") }
    Spacer(Modifier.height(8.dp))
    Button(onClick = p::onRefresh) { Text("Refresh (${state.items.size})") }
    Spacer(Modifier.height(8.dp))
    Box(Modifier.weight(1f)) {
      LazyColumn(state = listState) {
        items(state.items) { id ->
          CatalogItemCard(id = id)
          Spacer(Modifier.height(8.dp))
        }
      }
      if (glassHeight > 0.dp) {
        LiquidGlassBox(
          modifier = Modifier
            .align(Alignment.Center)
            .fillMaxWidth()
            .height(glassHeight)
        )
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
