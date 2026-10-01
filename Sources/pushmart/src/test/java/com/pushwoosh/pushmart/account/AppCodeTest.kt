package com.pushwoosh.pushmart.account

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCodeTest {
    @Test
    fun maskUppercasesAndInsertsDashAfterFifthChar() {
        assertEquals("ABCDE-12345", AppCode.mask("abcde12345"))
        assertEquals("ABCDE-F", AppCode.mask("abcdef"))
        assertEquals("ABCD", AppCode.mask("abcd"))
    }

    @Test
    fun maskDropsNonAlphanumericsAndTruncatesToTenChars() {
        assertEquals("ABCDE-1", AppCode.mask("ab-cd e1"))
        assertEquals("ABCDE-12345", AppCode.mask("ABCDE-12345XYZ"))
        assertEquals("", AppCode.mask("---"))
    }

    @Test
    fun validationRequiresFiveDashFive() {
        assertTrue(AppCode.isValid("5DF3B-09018"))
        assertTrue(AppCode.isValid(" 5DF3B-09018 "))
        assertTrue(AppCode.isValid("abcde-12345"))
        assertFalse(AppCode.isValid("5DF3B09018"))
        assertFalse(AppCode.isValid("5DF3B-0901"))
        assertFalse(AppCode.isValid(""))
    }

    @Test
    fun demoCodeIsValid() {
        assertEquals("5DF3B-09018", AppCode.DEMO)
        assertTrue(AppCode.isValid(AppCode.DEMO))
    }
}
