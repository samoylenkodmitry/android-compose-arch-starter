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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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
import java.util.Locale

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
  var highlightWordIndex by remember { mutableStateOf<Int?>(null) }
  var currentNormalizedWord by remember { mutableStateOf<String?>(null) }
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

  Column(Modifier.padding(16.dp)) {
    Text(state.title, style = MaterialTheme.typography.titleLarge)
    val content = state.content
    val words = remember(content) { content.toWordEntries() }
    LaunchedEffect(content) {
      highlightWordIndex = null
      currentNormalizedWord = null
      targetRect = null
      animLeft.snapTo(0f)
      animTop.snapTo(0f)
      animWidth.snapTo(0f)
      animHeight.snapTo(0f)
    }
    val translations = state.wordTranslations
    val activeTranslation = state.highlightedTranslation?.takeIf {
      val normalized = currentNormalizedWord
      normalized != null && normalized == state.highlightedWord
    }
    val display = remember(content, words, highlightWordIndex, activeTranslation, translations) {
      buildDisplayContent(content, words, highlightWordIndex, activeTranslation, translations)
    }
    val displayContent = display.text
    val displayBounds = display.bounds
    val highlightBounds = highlightWordIndex?.let { index ->
      displayBounds.firstOrNull { it.index == index }?.textBounds
    }
    val highlightRect = if (highlightWordIndex != null) {
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
          val range = highlightBounds
          if (range != null) {
            result.toLiquidRect(range, highlightPaddingXPx, highlightPaddingYPx)?.let {
              targetRect = it
            }
          }
        },
        modifier = Modifier.pointerInput(
          content,
          displayContent,
          displayBounds,
          words,
          highlightPaddingXPx,
          highlightPaddingYPx
        ) {
          awaitPointerEventScope {
            while (true) {
              val event = awaitPointerEvent()
              val pos = event.changes.first().position
              val layoutResult = layout ?: continue
              if (displayContent.isEmpty()) {
                event.changes.forEach { it.consume() }
                continue
              }
              val textLength = displayContent.length
              val rawOffset = layoutResult
                .getOffsetForPosition(pos)
                .coerceIn(0, textLength)
              val searchOffset = if (rawOffset == textLength) rawOffset - 1 else rawOffset
              if (searchOffset < 0) {
                event.changes.forEach { it.consume() }
                continue
              }
              val wordBounds = displayBounds.firstOrNull { it.contains(searchOffset) }
              if (wordBounds != null) {
                val entry = words.getOrNull(wordBounds.index)
                if (entry != null) {
                  val normalized = entry.normalized
                  if (normalized.isNotBlank()) {
                    layoutResult.toLiquidRect(
                      wordBounds.textBounds,
                      highlightPaddingXPx,
                      highlightPaddingYPx
                    )?.let { targetRect = it }
                    if (highlightWordIndex != wordBounds.index) {
                      highlightWordIndex = wordBounds.index
                    }
                    if (currentNormalizedWord != normalized) {
                      currentNormalizedWord = normalized
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

private data class WordEntry(
  val index: Int,
  val start: Int,
  val end: Int,
  val text: String,
  val normalized: String,
  val prefix: String,
  val suffix: String
)

private data class DisplayContent(
  val text: String,
  val bounds: List<DisplayWordBounds>
)

private data class DisplayWordBounds(val index: Int, val start: Int, val end: Int) {
  fun contains(offset: Int): Boolean = offset in start until end
  val textBounds: TextBounds get() = TextBounds(start, end)
}

private fun String.toWordEntries(): List<WordEntry> {
  if (isEmpty()) return emptyList()
  val entries = mutableListOf<WordEntry>()
  var index = 0
  var pos = 0
  while (pos < length) {
    while (pos < length && this[pos].isWhitespace()) {
      pos++
    }
    if (pos >= length) break
    val start = pos
    while (pos < length && !this[pos].isWhitespace()) {
      pos++
    }
    val end = pos
    val word = substring(start, end)
    val firstLetter = word.indexOfFirst { it.isLetterOrDigit() }
    if (firstLetter == -1) {
      entries += WordEntry(
        index = index++,
        start = start,
        end = end,
        text = word,
        normalized = "",
        prefix = word,
        suffix = ""
      )
    } else {
      val lastLetter = word.indexOfLast { it.isLetterOrDigit() }
      val prefix = if (firstLetter > 0) word.substring(0, firstLetter) else ""
      val suffix = if (lastLetter + 1 < word.length) word.substring(lastLetter + 1) else ""
      val normalized = word.substring(firstLetter, lastLetter + 1)
      entries += WordEntry(
        index = index++,
        start = start,
        end = end,
        text = word,
        normalized = normalized,
        prefix = prefix,
        suffix = suffix
      )
    }
  }
  return entries
}

private fun buildDisplayContent(
  content: String,
  words: List<WordEntry>,
  highlightIndex: Int?,
  translation: String?,
  translations: Map<String, String>
): DisplayContent {
  if (words.isEmpty()) return DisplayContent(content, emptyList())
  val builder = StringBuilder(content.length + (translation?.length ?: 0))
  val bounds = ArrayList<DisplayWordBounds>(words.size)
  var cursor = 0
  for (entry in words) {
    if (cursor < entry.start) {
      builder.append(content, cursor, entry.start)
    }
    val start = builder.length
    val normalized = entry.normalized
    val displayWord = if (normalized.isEmpty()) {
      entry.text
    } else {
      val key = normalized.lowercase(Locale.ROOT)
      val cachedTranslation = translations[key]?.takeIf { it.isNotEmpty() }
      val isHighlighted = highlightIndex != null && translation != null && entry.index == highlightIndex
      var targetLength = normalized.length
      if (cachedTranslation != null) {
        targetLength = max(targetLength, cachedTranslation.length)
      }
      val highlightedTranslation = translation?.takeIf { isHighlighted && it.isNotEmpty() }
      if (highlightedTranslation != null) {
        targetLength = max(targetLength, highlightedTranslation.length)
      }
      val displayCore = highlightedTranslation ?: normalized
      buildString {
        append(entry.prefix)
        append(displayCore)
        append(entry.suffix)
        if (targetLength > displayCore.length) {
          repeat(targetLength - displayCore.length) { append(' ') }
        }
      }
    }
    builder.append(displayWord)
    val end = builder.length
    bounds += DisplayWordBounds(entry.index, start, end)
    cursor = entry.end
  }
  if (cursor < content.length) {
    builder.append(content, cursor, content.length)
  }
  return DisplayContent(builder.toString(), bounds)
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
