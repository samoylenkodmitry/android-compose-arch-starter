package com.example.core.common.presenter

import org.junit.Assert.assertEquals
import org.junit.Test

class ParamInitTest {
    private class TestInit : ParamInit<String> {
        var value: String? = null
        override fun initOnce(params: String) {
            if (value == null) {
                value = params
            }
        }
    }

    @Test
    fun initOnce_onlyStoresFirstValue() {
        val init = TestInit()
        init.initOnce("first")
        init.initOnce("second")
        assertEquals("first", init.value)
    }
}
