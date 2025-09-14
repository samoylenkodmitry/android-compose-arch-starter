package com.archstarter.feature.detail.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archstarter.core.common.presenter.rememberPresenter
import com.archstarter.core.designsystem.AppTheme
import com.archstarter.feature.detail.api.DetailPresenter
import com.archstarter.feature.detail.api.DetailState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun DetailScreen(id: Int, presenter: DetailPresenter? = null) {
  val p = presenter ?: rememberPresenter<DetailPresenter, Int>(params = id)
  val state by p.state.collectAsStateWithLifecycle()
  val density = LocalDensity.current
  var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
  var currentWord by remember { mutableStateOf("") }
  var targetRect by remember { mutableStateOf<Rect?>(null) }
  val animRect = remember { Animatable(Rect.Zero, Rect.VectorConverter) }

  LaunchedEffect(targetRect) {
    targetRect?.let { animRect.animateTo(it) }
  }

  Column(Modifier.padding(16.dp)) {
    Text(state.title, style = MaterialTheme.typography.titleLarge)
    Box {
      Text(
        text = state.highlightedWord?.let { w ->
          state.highlightedTranslation?.let { t ->
            state.content.replaceFirst(w, t)
          }
        } ?: state.content,
        onTextLayout = { layout = it },
        modifier = Modifier.pointerInput(state.content) {
          awaitPointerEventScope {
            while (true) {
              val event = awaitPointerEvent()
              val pos = event.changes.first().position
              val layoutResult = layout ?: continue
              val text = state.content
              val index = layoutResult.getOffsetForPosition(pos).coerceIn(0, text.length)
              if (index < text.length) {
                val start = text.take(index).lastIndexOfAny(charArrayOf(' ', '\n')) + 1
                val end = text.indexOfAny(charArrayOf(' ', '\n'), start).let { if (it == -1) text.length else it }
                val word = text.substring(start, end)
                if (word.isNotBlank() && word != currentWord) {
                  currentWord = word
                  val startBox = layoutResult.getBoundingBox(start)
                  val endBox = layoutResult.getBoundingBox(end - 1)
                  targetRect = Rect(startBox.left, startBox.top, endBox.right, startBox.bottom)
                  p.translate(word)
                }
              }
              event.changes.forEach { it.consumeAllChanges() }
            }
          }
        }
      )
      if (state.highlightedWord != null && state.highlightedTranslation != null) {
        val rect = animRect.value
        Box(
          Modifier
            .offset { IntOffset(rect.left.toInt(), rect.top.toInt()) }
            .size(
              with(density) { rect.width.toDp() },
              with(density) { rect.height.toDp() }
            )
            .background(
              MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
              shape = MaterialTheme.shapes.small
            )
        )
      }
    }
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
  override fun translate(word: String) {}
}

@Preview(showBackground = true)
@Composable
private fun PreviewDetail() {
  AppTheme { DetailScreen(id = 1, presenter = FakeDetailPresenter()) }
}
