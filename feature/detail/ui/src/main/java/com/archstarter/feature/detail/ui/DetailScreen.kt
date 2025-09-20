package com.archstarter.feature.detail.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archstarter.core.common.presenter.rememberPresenter
import com.archstarter.core.designsystem.AppTheme
import com.archstarter.core.designsystem.LiquidGlassRect
import com.archstarter.core.designsystem.LiquidGlassRectOverlay
import com.archstarter.feature.detail.api.DetailPresenter
import com.archstarter.feature.detail.api.DetailState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

@Composable
fun DetailScreen(id: Int, presenter: DetailPresenter? = null) {
  val p = presenter ?: rememberPresenter<DetailPresenter, Int>(params = id)
  val state by p.state.collectAsStateWithLifecycle()
  val density = LocalDensity.current
  val highlightPaddingX = 12.dp
  val highlightPaddingY = 6.dp
  val highlightPaddingXPx = with(density) { highlightPaddingX.toPx() }
  val highlightPaddingYPx = with(density) { highlightPaddingY.toPx() }
  var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
  var currentWord by remember { mutableStateOf("") }
  var highlightRange by remember { mutableStateOf<TextBounds?>(null) }
  var translationRange by remember { mutableStateOf<TextBounds?>(null) }
  var targetRect by remember { mutableStateOf<LiquidRectPx?>(null) }
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

  LaunchedEffect(state.highlightedWord, state.highlightedTranslation, highlightRange) {
    val translatedWord = state.highlightedWord
    val translation = state.highlightedTranslation
    translationRange = if (
      translatedWord != null &&
      translation != null &&
      highlightRange != null &&
      translatedWord == currentWord
    ) {
      highlightRange
    } else if (translation == null || translatedWord != currentWord) {
      null
    } else {
      translationRange
    }
  }

  Column(Modifier.padding(16.dp)) {
    Text(state.title, style = MaterialTheme.typography.titleLarge)
    val translation = state.highlightedTranslation
    val content = state.content
    val translationBounds = translationRange
    val displayContent = if (translation != null && translationBounds != null) {
      content.replaceRange(translationBounds.start, translationBounds.end, translation)
    } else {
      content
    }
    val highlightRect = if (translation != null && translationBounds != null) {
      val width = animWidth.value
      val height = animHeight.value
      if (width > 0f && height > 0f) {
        with(density) {
          LiquidGlassRect(
            left = animLeft.value.toDp(),
            top = animTop.value.toDp(),
            width = width.toDp(),
            height = height.toDp()
          )
        }
      } else {
        null
      }
    } else {
      null
    }

    LiquidGlassRectOverlay(rect = highlightRect) {
      Text(
        text = displayContent,
        onTextLayout = { result ->
          layout = result
          val translationSnapshot = translationBounds
          val activeRange = translationSnapshot ?: highlightRange
          if (activeRange != null) {
            val translationLength = translation?.length
            val displayRange = activeRange.toDisplayRange(translationSnapshot, translationLength)
            if (displayRange.isValidFor(displayContent)) {
              result.toLiquidRect(displayRange, highlightPaddingXPx, highlightPaddingYPx)?.let {
                targetRect = it
              }
            }
          }
        },
        modifier = Modifier.pointerInput(
          content,
          displayContent,
          translationRange,
          translation,
          highlightPaddingXPx,
          highlightPaddingYPx
        ) {
          awaitPointerEventScope {
            while (true) {
              val event = awaitPointerEvent()
              val pos = event.changes.first().position
              val layoutResult = layout ?: continue
              val translationSnapshot = translationRange
              val displayIndex = layoutResult
                .getOffsetForPosition(pos)
                .coerceIn(0, displayContent.length)
              val boundary = layoutResult.getWordBoundary(displayIndex)
              if (boundary.end > boundary.start) {
                val displayRange = TextBounds(boundary.start, boundary.end)
                val originalRange = displayRange.toOriginalRange(translationSnapshot, translation?.length)
                if (originalRange.isValidFor(content)) {
                  val normalized = content
                    .substring(originalRange.start, originalRange.end)
                    .trim { !it.isLetterOrDigit() }
                  if (normalized.isNotBlank()) {
                    val highlightDisplayRange = originalRange.toDisplayRange(translationSnapshot, translation?.length)
                    if (highlightDisplayRange.isValidFor(displayContent)) {
                      layoutResult.toLiquidRect(
                        highlightDisplayRange,
                        highlightPaddingXPx,
                        highlightPaddingYPx
                      )?.let { targetRect = it }
                    }
                    if (highlightRange != originalRange) {
                      highlightRange = originalRange
                      translationRange = null
                      currentWord = normalized
                      p.translate(normalized)
                    }
                  }
                }
              }
              event.changes.forEach { it.consume() }
            }
          }
        }
      )
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

private data class LiquidRectPx(val left: Float, val top: Float, val width: Float, val height: Float)

private data class TextBounds(val start: Int, val end: Int) {
  val length: Int get() = end - start
}

private fun TextBounds.isValidFor(text: String): Boolean =
  start >= 0 && end <= text.length && start < end

private fun TextBounds.toOriginalRange(
  translationRange: TextBounds?,
  translationLength: Int?
): TextBounds {
  if (translationRange == null || translationLength == null) return this
  val originalLength = translationRange.length
  val delta = translationLength - originalLength
  val mappedStart = when {
    start < translationRange.start -> start
    start < translationRange.start + translationLength -> translationRange.start
    else -> start - delta
  }
  val mappedEnd = when {
    end <= translationRange.start -> end
    end <= translationRange.start + translationLength -> translationRange.end
    else -> end - delta
  }
  return TextBounds(mappedStart, mappedEnd)
}

private fun TextBounds.toDisplayRange(
  translationRange: TextBounds?,
  translationLength: Int?
): TextBounds {
  if (translationRange == null || translationLength == null) return this
  val originalLength = translationRange.length
  val delta = translationLength - originalLength
  return when {
    this == translationRange -> TextBounds(start, start + translationLength)
    end <= translationRange.start -> this
    start >= translationRange.end -> TextBounds(start + delta, end + delta)
    else -> this
  }
}

private fun TextLayoutResult.toLiquidRect(
  range: TextBounds?,
  paddingX: Float,
  paddingY: Float
): LiquidRectPx? {
  if (range == null || range.length <= 0) return null
  val start = range.start.coerceAtLeast(0)
  val end = range.end
  if (end <= start) return null
  val lastIndex = end - 1
  val startBox = getBoundingBox(start)
  val endBox = getBoundingBox(lastIndex)
  val left = startBox.left
  val top = min(startBox.top, endBox.top)
  val right = endBox.right
  val bottom = max(startBox.bottom, endBox.bottom)
  val expandedLeft = left - paddingX
  val expandedTop = top - paddingY
  val expandedWidth = (right - left + paddingX * 2).coerceAtLeast(0f)
  val expandedHeight = (bottom - top + paddingY * 2).coerceAtLeast(0f)
  return LiquidRectPx(expandedLeft, expandedTop, expandedWidth, expandedHeight)
}

@Preview(showBackground = true)
@Composable
private fun PreviewDetail() {
  AppTheme { DetailScreen(id = 1, presenter = FakeDetailPresenter()) }
}
