package com.archstarter.core.common.presenter

import org.junit.After
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertEquals
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
        registerPresenterMocks(
            presenterMock<TestPresenter>(key = "primary") { TestPresenter("primary") },
        )

        val resolved = findPresenterMock(TestPresenter::class, "primary") as TestPresenter
        assertEquals("primary", resolved.value)
        assertSame(resolved, findPresenterMock(TestPresenter::class, "primary"))
    }

    @Test
    fun resolveFallback_createsDistinctInstancesPerKey() {
        registerPresenterMocks(
            presenterMock<TestPresenter> { TestPresenter("fallback") },
        )

        val first = findPresenterMock(TestPresenter::class, "one") as TestPresenter
        val second = findPresenterMock(TestPresenter::class, "two") as TestPresenter

        assertEquals("fallback", first.value)
        assertEquals("fallback", second.value)
        assertNotSame(first, second)
        assertSame(first, findPresenterMock(TestPresenter::class, "one"))
    }
}
