package com.pushwoosh.pushmart.account

object AppCode {
    const val DEMO = "5DF3B-09018"

    private val pattern = Regex("^[A-Za-z0-9]{5}-[A-Za-z0-9]{5}$")

    fun isValid(code: String): Boolean = pattern.matches(code.trim())

    fun mask(raw: String): String {
        val clean = raw.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.take(10)
        return if (clean.length > 5) clean.substring(0, 5) + "-" + clean.substring(5) else clean
    }
}
