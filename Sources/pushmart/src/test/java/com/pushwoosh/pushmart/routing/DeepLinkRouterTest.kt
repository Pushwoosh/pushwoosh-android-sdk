package com.pushwoosh.pushmart.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkRouterTest {
    @Test
    fun routesSaleAndProductLinks() {
        assertEquals(Route.Sale, DeepLinkRouter.fromUri("pushwoosh://sale"))
        assertEquals(Route.Product("chrono"), DeepLinkRouter.fromUri("pushwoosh://product/chrono"))
        assertEquals(Route.Product("chrono"), DeepLinkRouter.fromUri("pushwoosh://product/chrono/"))
    }

    @Test
    fun routesLegacyProduct1And2ToAeroknitAndChrono() {
        assertEquals(Route.Product("aeroknit"), DeepLinkRouter.fromUri("pushwoosh://product1"))
        assertEquals(Route.Product("chrono"), DeepLinkRouter.fromUri("pushwoosh://product2"))
    }

    @Test
    fun routesSchemeAndHostCaseInsensitively() {
        assertEquals(Route.Sale, DeepLinkRouter.fromUri("PUSHWOOSH://SALE"))
        assertEquals(Route.Product("orbit"), DeepLinkRouter.fromUri("Pushwoosh://Product/orbit"))
    }

    @Test
    fun ignoresProductLinkWithoutKnownId() {
        assertNull(DeepLinkRouter.fromUri("pushwoosh://product/"))
        assertNull(DeepLinkRouter.fromUri("pushwoosh://product"))
        assertNull(DeepLinkRouter.fromUri("pushwoosh://product/unknown"))
        assertNull(DeepLinkRouter.fromUri("pushwoosh://product/CHRONO"))
    }

    @Test
    fun ignoresForeignSchemesUnknownHostsAndGarbage() {
        assertNull(DeepLinkRouter.fromUri("https://pushwoosh.com/sale"))
        assertNull(DeepLinkRouter.fromUri("pushwoosh://inbox"))
        assertNull(DeepLinkRouter.fromUri("pushwoosh-5DF3B-09018://createTestDevice"))
        assertNull(DeepLinkRouter.fromUri("not a uri at all"))
        assertNull(DeepLinkRouter.fromUri(""))
    }

    @Test
    fun customDataPrefersVoucherThenProductThenSale() {
        assertEquals(
            Route.Voucher("SAVE20"),
            DeepLinkRouter.fromCustomData(
                mapOf("voucher" to "SAVE20", "product_id" to "chrono", "sale" to true)))
        assertEquals(
            Route.Product("chrono"),
            DeepLinkRouter.fromCustomData(mapOf("product_id" to "chrono", "sale" to true)))
        assertEquals(Route.Sale, DeepLinkRouter.fromCustomData(mapOf("sale" to true)))
        assertEquals(Route.Sale, DeepLinkRouter.fromCustomData(mapOf("sale" to "1")))
        assertEquals(Route.Sale, DeepLinkRouter.fromCustomData(mapOf("sale" to "")))
    }

    @Test
    fun customDataSkipsEmptyVoucherAndUnknownProduct() {
        assertEquals(
            Route.Product("orbit"),
            DeepLinkRouter.fromCustomData(mapOf("voucher" to "", "product_id" to "orbit")))
        assertEquals(
            Route.Sale,
            DeepLinkRouter.fromCustomData(mapOf("product_id" to "unknown", "sale" to "yes")))
        assertNull(DeepLinkRouter.fromCustomData(mapOf("product_id" to "unknown")))
        assertNull(DeepLinkRouter.fromCustomData(mapOf("sale" to null)))
        assertNull(DeepLinkRouter.fromCustomData(emptyMap()))
    }
}
