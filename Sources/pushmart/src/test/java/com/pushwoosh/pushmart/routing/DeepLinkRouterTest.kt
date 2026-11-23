package com.pushwoosh.pushmart.routing

import com.pushwoosh.pushmart.routing.DeepLinkRouter.fromCustomData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeepLinkRouterTest {
    @Test
    fun routesSaleProductAndPushOnLinks() {
        assertEquals(Route.Sale, DeepLinkRouter.fromUri("pushwoosh://sale"))
        assertEquals(
            Route.Product("dress_04"), DeepLinkRouter.fromUri("pushwoosh://product/dress_04"))
        assertEquals(
            Route.Product("dress_04"), DeepLinkRouter.fromUri("pushwoosh://product/dress_04/"))
        assertEquals(
            Route.AddToBag("dress_04"), DeepLinkRouter.fromUri("pushon-add-product://dress_04"))
        assertEquals(Route.Cart, DeepLinkRouter.fromUri("pushon-open-tab://cart"))
    }

    @Test
    fun routesProduct1And2ToOutwear01And02() {
        assertEquals(Route.Product("outwear_01"), DeepLinkRouter.fromUri("pushwoosh://product1"))
        assertEquals(Route.Product("outwear_02"), DeepLinkRouter.fromUri("pushwoosh://product2"))
    }

    @Test
    fun routesSchemeAndHostCaseInsensitively() {
        assertEquals(Route.Sale, DeepLinkRouter.fromUri("PUSHWOOSH://SALE"))
        assertEquals(
            Route.Product("tops_02"), DeepLinkRouter.fromUri("Pushwoosh://Product/tops_02"))
        assertEquals(Route.Cart, DeepLinkRouter.fromUri("PUSHON-OPEN-TAB://CART"))
    }

    @Test
    fun ignoresProductLinkWithoutKnownId() {
        assertNull(DeepLinkRouter.fromUri("pushwoosh://product/"))
        assertNull(DeepLinkRouter.fromUri("pushwoosh://product"))
        assertNull(DeepLinkRouter.fromUri("pushwoosh://product/unknown"))
        assertNull(DeepLinkRouter.fromUri("pushwoosh://product/DRESS_04"))
        assertNull(DeepLinkRouter.fromUri("pushwoosh://product/chrono"))
        assertNull(DeepLinkRouter.fromUri("pushon-add-product://unknown"))
        assertNull(DeepLinkRouter.fromUri("pushon-add-product://DRESS_04"))
        assertNull(DeepLinkRouter.fromUri("pushon-add-product://"))
    }

    @Test
    fun ignoresForeignSchemesUnknownHostsAndGarbage() {
        assertNull(DeepLinkRouter.fromUri("https://pushwoosh.com/sale"))
        assertNull(DeepLinkRouter.fromUri("pushwoosh://inbox"))
        assertNull(DeepLinkRouter.fromUri("pushwoosh-5DF3B-09018://createTestDevice"))
        assertNull(DeepLinkRouter.fromUri("not a uri at all"))
        assertNull(DeepLinkRouter.fromUri(""))
        assertNull(DeepLinkRouter.fromUri("pushon-open-tab://shop"))
        assertNull(DeepLinkRouter.fromUri("pushon-open-tab://"))
    }

    @Test
    fun customDataPrefersPushOnThenVoucherThenProductThenSale() {
        assertEquals(
            Route.Voucher("SAVE20"),
            fromCustomData(
                mapOf("voucher" to "SAVE20", "product_id" to "dress_04", "sale" to true)))
        assertEquals(
            Route.Product("dress_04"),
            fromCustomData(mapOf("product_id" to "dress_04", "sale" to true)))
        assertEquals(Route.Sale, fromCustomData(mapOf("sale" to true)))
        assertEquals(Route.Sale, fromCustomData(mapOf("sale" to "1")))
        assertEquals(Route.Sale, fromCustomData(mapOf("sale" to "")))
        assertEquals(
            Route.Voucher("-20%"),
            fromCustomData(
                mapOf(
                    "custom" to
                        mapOf(
                            "voucher" to mapOf("discount" to 20),
                            "specialOffer" to mapOf("product_id" to "tops_02")),
                    "voucher" to "SAVE20")))
        assertEquals(
            Route.Product("tops_02"),
            fromCustomData(
                mapOf(
                    "custom" to
                        mapOf(
                            "specialOffer" to mapOf("product_id" to "tops_02"),
                            "items_in_basket" to 2),
                    "product_id" to "dress_04")))
        assertEquals(
            Route.Cart,
            fromCustomData(mapOf("custom" to mapOf("items_in_basket" to null), "sale" to true)))
        for (discount in listOf("15", " 15", "+15", "15.0", "15%")) assertEquals(
            Route.Voucher("-15%"),
            fromCustomData(mapOf("custom" to mapOf("voucher" to mapOf("discount" to discount)))))
        assertEquals(
            Route.Voucher("-13%"),
            fromCustomData(mapOf("custom" to mapOf("voucher" to mapOf("discount" to 12.6)))))
        assertEquals(
            Route.Voucher("-500%"),
            fromCustomData(mapOf("custom" to mapOf("voucher" to mapOf("discount" to 1500)))))
    }

    @Test
    fun customDataSkipsEmptyVoucherAndUnknownProduct() {
        assertEquals(
            Route.Product("tops_02"),
            fromCustomData(mapOf("voucher" to "", "product_id" to "tops_02")))
        assertEquals(Route.Sale, fromCustomData(mapOf("product_id" to "unknown", "sale" to "yes")))
        assertNull(fromCustomData(mapOf("product_id" to "unknown")))
        assertNull(fromCustomData(mapOf("product_id" to "aeroknit")))
        assertNull(fromCustomData(mapOf("sale" to null)))
        assertNull(fromCustomData(emptyMap()))
        assertNull(
            fromCustomData(
                mapOf(
                    "custom" to mapOf("specialOffer" to mapOf("product_id" to "unknown")),
                    "product_id" to "dress_04")))
        for (discount in listOf("0", "-5", "", "abc", "%15", "+ 15", "0x1F")) assertEquals(
            Route.Sale,
            fromCustomData(
                mapOf(
                    "custom" to mapOf("voucher" to mapOf("discount" to discount)), "sale" to true)))
        assertEquals(
            Route.Product("dress_04"),
            fromCustomData(mapOf("custom" to mapOf("other" to 1), "product_id" to "dress_04")))
    }
}
