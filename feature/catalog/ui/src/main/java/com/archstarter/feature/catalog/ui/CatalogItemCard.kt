package com.archstarter.feature.catalog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archstarter.core.common.presenter.MocksMap
import com.archstarter.core.common.presenter.PresenterMockKey
import com.archstarter.core.common.presenter.rememberPresenter
import com.archstarter.core.designsystem.AppTheme
import com.archstarter.feature.catalog.api.CatalogItem
import com.archstarter.feature.catalog.api.CatalogItemPresenter
import com.archstarter.feature.catalog.ui.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun CatalogItemCard(
  id: Int,
  modifier: Modifier = Modifier,
) {
  CatalogItemCardWithPresenter(id = id, modifier = modifier)
}

@Composable
private fun CatalogItemCardWithPresenter(
  id: Int,
  modifier: Modifier,
) {
  val p = rememberPresenter<CatalogItemPresenter, Int>(key = "item$id", params = id)
  val state by p.state.collectAsStateWithLifecycle()
  CatalogItemCardContent(
    state = state,
    onClick = p::onClick,
    modifier = modifier
  )
}

@Composable
internal fun CatalogItemCardContent(
  state: CatalogItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(12.dp)
  ) {
    Text(state.title)
    Text(state.summary, style = MaterialTheme.typography.bodySmall)
  }
}

// ---- Fake for preview ----
private class FakeCatalogItemPresenter : CatalogItemPresenter {
  private val _s = MutableStateFlow(CatalogItem(1, "Alpha", "Summary"))
  override val state: StateFlow<CatalogItem> = _s
  override fun onClick() {}
  override fun initOnce(params: Int?) {
    val id = params ?: return
    _s.value = CatalogItem(id, "Item $id", "Summary for item #$id")
  }
}

private val catalogItemPreviewKeys = listOf<String?>(null, "item1", "item2", "item3", "item4", "item5")

@Suppress("unused")
private val catalogItemPreviewMocks = if (BuildConfig.DEBUG) {
  catalogItemPreviewKeys.forEach { key ->
    MocksMap[PresenterMockKey(CatalogItemPresenter::class, key)] = FakeCatalogItemPresenter()
  }
} else {
  Unit
}

@Preview
@Composable
private fun PreviewCatalogItemCard() {
  AppTheme {
    CatalogItemCard(id = 1)
  }
}
