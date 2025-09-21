package com.archstarter.core.common.presenter

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

private class TestPresenter(private val label: String) : ParamInit<Unit> {
    val value: String = label
    override fun initOnce(params: Unit?) {}
}

class PresenterMocksTest {
    @Before
    fun setUp() {
        clearPresenterMocks()
    }

    @After
    fun tearDown() {
        clearPresenterMocks()
    }

    @Test
    fun resolveDirectKey_returnsRegisteredInstance() {
        MocksMap[PresenterMockKey(TestPresenter::class, "primary")] = TestPresenter("primary")

        val resolved = findPresenterMock(TestPresenter::class, "primary") as TestPresenter
        assertEquals("primary", resolved.value)
        assertSame(resolved, findPresenterMock(TestPresenter::class, "primary"))
    }

    @Test
    fun resolveFallback_returnsSharedInstanceWhenAvailable() {
        val fallback = TestPresenter("fallback")
        MocksMap[PresenterMockKey(TestPresenter::class, null)] = fallback

        val first = findPresenterMock(TestPresenter::class, "one") as TestPresenter
        val second = findPresenterMock(TestPresenter::class, "two") as TestPresenter

        assertSame(fallback, first)
        assertSame(fallback, second)
    }
}
