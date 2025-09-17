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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun DetailScreen(id: Int, presenter: DetailPresenter? = null) {
  val p = presenter ?: rememberPresenter<DetailPresenter, Int>(params = id)
  val state by p.state.collectAsStateWithLifecycle()
  val density = LocalDensity.current
  var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
  var currentWord by remember { mutableStateOf("") }
  var targetRect by remember { mutableStateOf<LiquidRect?>(null) }
  val animLeft = remember { Animatable(0f) }
  val animTop = remember { Animatable(0f) }
  val animWidth = remember { Animatable(0f) }
  val animHeight = remember { Animatable(0f) }

  LaunchedEffect(targetRect) {
    targetRect?.let { rect ->
      coroutineScope {
        launch { animLeft.animateTo(rect.left) }
        launch { animTop.animateTo(rect.top) }
        launch { animWidth.animateTo(rect.width) }
        launch { animHeight.animateTo(rect.height) }
      }
    }
  }

  Column(Modifier.padding(16.dp)) {
    Text(state.title, style = MaterialTheme.typography.titleLarge)
    Box {
      val content = state.content
      Text(
        text = content,
        onTextLayout = { layout = it },
        modifier = Modifier.pointerInput(content) {
          awaitPointerEventScope {
            while (true) {
              val event = awaitPointerEvent()
              val pos = event.changes.first().position
              val layoutResult = layout ?: continue
              val index = layoutResult.getOffsetForPosition(pos).coerceIn(0, content.length)
              if (index < content.length) {
                val range = layoutResult.getWordBoundary(index)
                if (range.end > range.start) {
                  val word = content.substring(range.start, range.end)
                  val normalized = word.trim { !it.isLetterOrDigit() }
                  if (normalized.isNotBlank() && normalized != currentWord) {
                    currentWord = normalized
                    val startBox = layoutResult.getBoundingBox(range.start)
                    val endBox = layoutResult.getBoundingBox(range.end - 1)
                    val left = startBox.left
                    val top = min(startBox.top, endBox.top)
                    val right = endBox.right
                    val bottom = max(startBox.bottom, endBox.bottom)
                    targetRect = LiquidRect(left, top, right - left, bottom - top)
                    p.translate(normalized)
                  }
                }
              }
              event.changes.forEach { it.consume() }
            }
          }
        }
      )
      val translation = state.highlightedTranslation
      if (state.highlightedWord != null && translation != null) {
        val left = animLeft.value
        val top = animTop.value
        val width = animWidth.value
        val height = animHeight.value
        if (width > 0f && height > 0f) {
          Box(
            Modifier
              .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
              .size(
                with(density) { width.toDp() },
                with(density) { height.toDp() }
              )
              .background(
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                shape = MaterialTheme.shapes.small
              ),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = translation,
              modifier = Modifier.padding(horizontal = 4.dp),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSecondary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
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

private data class LiquidRect(val left: Float, val top: Float, val width: Float, val height: Float)

@Preview(showBackground = true)
@Composable
private fun PreviewDetail() {
  AppTheme { DetailScreen(id = 1, presenter = FakeDetailPresenter()) }
}
