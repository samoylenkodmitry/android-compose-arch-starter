package com.archstarter.feature.detail.ui

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailDisplayFormatterTest {

  @Test
  fun cachedTranslationIsDisplayedByDefault() {
    val content = "Alpha beta"
    val words = content.toWordEntries()
    val translations = mapOf("beta" to "translated")

    val display = buildDisplayContent(
      content = content,
      words = words,
      highlightIndex = null,
      translation = null,
      translations = translations,
      measureWidth = ::charCountMeasure,
    )

    val wordBounds = display.bounds[1]
    val segment = display.text.substring(wordBounds.start, wordBounds.end)
    val trimmed = segment.trim(DETAIL_DISPLAY_PAD_CHAR)

    assertEquals("translated", trimmed)
  }

  @Test
  fun highlightedTranslationRetainsDisplayWidth() {
    val content = "Gamma world"
    val words = content.toWordEntries()
    val translations = mapOf("gamma" to "go")

    val base = buildDisplayContent(
      content = content,
      words = words,
      highlightIndex = null,
      translation = null,
      translations = translations,
      measureWidth = ::charCountMeasure,
    )
    val highlighted = buildDisplayContent(
      content = content,
      words = words,
      highlightIndex = 0,
      translation = "go",
      translations = translations,
      measureWidth = ::charCountMeasure,
    )

    assertEquals(base.text.length, highlighted.text.length)

    val highlightBounds = highlighted.bounds[0]
    val highlightSegment = highlighted.text.substring(highlightBounds.start, highlightBounds.end)
    val expectedPad = "Gamma".length - translations.getValue("gamma").length

    val leadingPads = highlightSegment.takeWhile { it == DETAIL_DISPLAY_PAD_CHAR }.length
    val trailingPads = highlightSegment.reversed().takeWhile { it == DETAIL_DISPLAY_PAD_CHAR }.length

    assertEquals("Gamma".length, highlightSegment.length)
    assertEquals(expectedPad, leadingPads + trailingPads)
    assertTrue(abs(leadingPads - trailingPads) <= 1)
    assertEquals("go", highlightSegment.substring(leadingPads, highlightSegment.length - trailingPads))
  }

  @Test
  fun measuredWidthsAlignForVariableCharacters() {
    val content = "ill wig"
    val words = content.toWordEntries()
    val translations = mapOf("ill" to "WWW")

    val base = buildDisplayContent(
      content = content,
      words = words,
      highlightIndex = null,
      translation = null,
      translations = translations,
      measureWidth = ::variableWidthMeasure,
    )
    val highlighted = buildDisplayContent(
      content = content,
      words = words,
      highlightIndex = 0,
      translation = "WWW",
      translations = translations,
      measureWidth = ::variableWidthMeasure,
    )

    val baseBounds = base.bounds[0]
    val baseSegment = base.text.substring(baseBounds.start, baseBounds.end)
    val highlightBounds = highlighted.bounds[0]
    val highlightSegment = highlighted.text.substring(highlightBounds.start, highlightBounds.end)

    val baseWidth = variableWidthMeasure(baseSegment)
    val highlightWidth = variableWidthMeasure(highlightSegment)

    assertTrue(
      "Measured widths differ: base=$baseWidth highlight=$highlightWidth",
      abs(baseWidth - highlightWidth) < 0.01f
    )
    val baseTrimmed = baseSegment.trim(DETAIL_DISPLAY_PAD_CHAR)
    val highlightTrimmed = highlightSegment.trim(DETAIL_DISPLAY_PAD_CHAR)

    assertEquals("WWW", highlightTrimmed)
    assertEquals("WWW", baseTrimmed)
  }
}

private fun charCountMeasure(text: String): Float = text.length.toFloat()

private fun variableWidthMeasure(text: String): Float {
  var width = 0f
  for (ch in text) {
    val normalized = ch.lowercaseChar()
    width += when (normalized) {
      'i', 'l' -> 1f
      'w', 'm' -> 3f
      DETAIL_DISPLAY_PAD_CHAR -> 1f
      ' ' -> 1f
      else -> 2f
    }
  }
  return width
}
