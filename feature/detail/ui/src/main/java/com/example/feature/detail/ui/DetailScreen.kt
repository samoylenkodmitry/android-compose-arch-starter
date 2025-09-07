package com.example.feature.detail.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
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
import com.example.feature.detail.api.DetailPresenter
import com.example.feature.detail.api.DetailState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DetailScreen(id: Int, presenter: DetailPresenter? = null) {
  val p = presenter ?: rememberPresenter<DetailPresenter, Int>(params = id)
  val state by p.state.collectAsStateWithLifecycle()
  Column(Modifier.padding(16.dp)) {
    Text(state.title, style = MaterialTheme.typography.titleLarge)
    Text(state.content)
    if (state.ipa != null) {
      Text("IPA: ${state.ipa}", style = MaterialTheme.typography.bodySmall)
    }
    Text("Source: ${state.sourceUrl}", style = MaterialTheme.typography.bodySmall)
  }
}

private class FakeDetailPresenter : DetailPresenter {
  private val _s = MutableStateFlow(DetailState(title = "Preview", content = "Content"))
  override val state: StateFlow<DetailState> = _s
  override fun initOnce(params: Int) {}
}

@Preview(showBackground = true)
@Composable
private fun PreviewDetail() {
  AppTheme { DetailScreen(id = 1, presenter = FakeDetailPresenter()) }
}
