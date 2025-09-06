package com.example.feature.catalog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.AppTheme
import com.example.feature.catalog.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun CatalogScreen(
  presenter: CatalogPresenter? = null
) {
  val p = presenter ?: rememberPresenter<CatalogPresenter, Unit>()
  val state by p.state.collectAsStateWithLifecycle()
  Column(Modifier.padding(16.dp)) {
    Text("Catalog", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Button(onClick = p::onRefresh) { Text("Refresh (${state.items.size})") }
    Spacer(Modifier.height(8.dp))
    state.items.forEach { Text("• " + it) }
  }
}

// ---- Fake for preview (no Hilt in UI module) ----
private class FakeCatalogPresenter : CatalogPresenter {
  private val _s = MutableStateFlow(CatalogState(listOf("Alpha","Beta","Gamma")))
  override val state: StateFlow<CatalogState> = _s
  override fun onRefresh() { _s.value = _s.value.copy(items = _s.value.items + "!") }
}

@Preview(showBackground = true)
@Composable
private fun PreviewCatalog() {
  AppTheme { CatalogScreen(presenter = FakeCatalogPresenter()) }
}
