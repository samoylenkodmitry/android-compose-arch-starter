package com.archstarter.feature.detail.ui

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailDisplayFormatterTest {

  @Test
  fun originalWordIsPaddedToTranslationWidth() {
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
    val padCount = translations.getValue("beta").length - "beta".length

    assertTrue(padCount > 0)
    assertEquals("beta".length + padCount, segment.length)
    assertTrue(segment.takeLast(padCount).all { it == DETAIL_DISPLAY_PAD_CHAR })
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

    assertTrue(highlightSegment.startsWith("go"))
    assertEquals("Gamma".length, highlightSegment.length)
    assertTrue(highlightSegment.takeLast(expectedPad).all { it == DETAIL_DISPLAY_PAD_CHAR })
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
    assertTrue(highlightSegment.startsWith("WWW"))
    assertTrue(baseSegment.contains(DETAIL_DISPLAY_PAD_CHAR))
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
