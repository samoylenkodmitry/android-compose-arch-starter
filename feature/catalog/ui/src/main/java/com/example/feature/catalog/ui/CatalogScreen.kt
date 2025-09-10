package com.example.feature.catalog.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.common.presenter.rememberPresenter
import com.example.core.designsystem.AppTheme
import com.example.feature.catalog.api.CatalogPresenter
import com.example.feature.catalog.api.CatalogState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun CatalogScreen(
  presenter: CatalogPresenter? = null,
) {
  val p = presenter ?: rememberPresenter<CatalogPresenter, Unit>()
  val state by p.state.collectAsStateWithLifecycle()
  Column(Modifier.padding(16.dp)) {
    Text("Catalog", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Button(onClick = p::onSettingsClick) { Text("Settings") }
    Spacer(Modifier.height(8.dp))
    Button(onClick = p::onRefresh) { Text("Refresh (${state.items.size})") }
    Spacer(Modifier.height(8.dp))
    LazyColumn {
      items(state.items) { id ->
        CatalogItemCard(id = id)
        Spacer(Modifier.height(8.dp))
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
