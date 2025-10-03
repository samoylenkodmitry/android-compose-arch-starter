package com.archstarter.feature.catalog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.archstarter.core.common.presenter.rememberPresenter
import com.archstarter.feature.catalog.api.CatalogItem
import com.archstarter.feature.catalog.api.CatalogItemPresenter

@Composable
fun CatalogItemCard(
  id: Int,
  modifier: Modifier = Modifier,
) {
  val presenter = rememberPresenter<CatalogItemPresenter, Int>(params = id)
  val state by presenter.state.collectAsState()
  CatalogItemCardContent(
    state = state,
    onClick = presenter::onClick,
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