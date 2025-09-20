package com.archstarter.feature.detail.ui

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
    )
    val highlighted = buildDisplayContent(
      content = content,
      words = words,
      highlightIndex = 0,
      translation = "go",
      translations = translations,
    )

    assertEquals(base.text.length, highlighted.text.length)

    val highlightBounds = highlighted.bounds[0]
    val highlightSegment = highlighted.text.substring(highlightBounds.start, highlightBounds.end)
    val expectedPad = "Gamma".length - translations.getValue("gamma").length

    assertTrue(highlightSegment.startsWith("go"))
    assertEquals("Gamma".length, highlightSegment.length)
    assertTrue(highlightSegment.takeLast(expectedPad).all { it == DETAIL_DISPLAY_PAD_CHAR })
  }
}
