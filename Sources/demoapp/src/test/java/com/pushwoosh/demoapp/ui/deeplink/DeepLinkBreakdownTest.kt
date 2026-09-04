package com.pushwoosh.demoapp.ui.deeplink

import org.junit.Assert.assertEquals
import org.junit.Test

class DeepLinkBreakdownTest {

    @Test
    fun rowsListHostPathAndQueryParams() {
        val rows = DeepLinkBreakdown.rows("demo", "/screen", listOf("id" to "42"))

        assertEquals(listOf("host=demo", "path=/screen", "id=42"), rows)
    }

    @Test
    fun rowsKeepQueryParamOrder() {
        val rows = DeepLinkBreakdown.rows("demo", "/screen", listOf("id" to "42", "utm" to "push"))

        assertEquals(listOf("host=demo", "path=/screen", "id=42", "utm=push"), rows)
    }

    @Test
    fun rowsMarkMissingHostAndPath() {
        val rows = DeepLinkBreakdown.rows(null, "", emptyList())

        assertEquals(listOf("host=—", "path=—"), rows)
    }
}
