package com.archstarter.core.common.presenter

import org.junit.After
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class PresenterPreviewRegistryTest {
    private class TestPresenter : ParamInit<Unit> {
        override fun initOnce(params: Unit?) {}
    }

    private val registered = mutableListOf<PresenterMocks>()

    @After
    fun tearDown() {
        registered.forEach { mocks ->
            PresenterPreviewRegistry.unregister(mocks.entries)
        }
        registered.clear()
    }

    @Test
    fun resolve_usesFallbackFactoryPerKey() {
        val mocks = presenterMocksOf(
            presenterMock<TestPresenter> { TestPresenter() },
        )
        PresenterPreviewRegistry.register(mocks.entries)
        registered += mocks

        val first = PresenterPreviewRegistry.resolve(TestPresenter::class, "a") as TestPresenter
        val second = PresenterPreviewRegistry.resolve(TestPresenter::class, "a") as TestPresenter
        val third = PresenterPreviewRegistry.resolve(TestPresenter::class, "b") as TestPresenter

        assertSame(first, second)
        assertNotSame(first, third)
    }

    @Test
    fun resolve_prefersKeyedFactory() {
        val keyedPresenter = TestPresenter()
        val mocks = presenterMocksOf(
            presenterMock<TestPresenter>(key = "target") { keyedPresenter },
            presenterMock<TestPresenter> { TestPresenter() },
        )
        PresenterPreviewRegistry.register(mocks.entries)
        registered += mocks

        val resolvedKeyed = PresenterPreviewRegistry.resolve(TestPresenter::class, "target") as TestPresenter
        val resolvedFallback = PresenterPreviewRegistry.resolve(TestPresenter::class, "other") as TestPresenter

        assertSame(keyedPresenter, resolvedKeyed)
        assertNotSame(keyedPresenter, resolvedFallback)
    }
}
