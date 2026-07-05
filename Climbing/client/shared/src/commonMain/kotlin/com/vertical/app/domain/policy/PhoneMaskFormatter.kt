package com.vertical.app.domain.policy

/**
 * Маска российского мобильного: +7 (900) 123-45-67
 */
object PhoneMaskFormatter {
    fun format(input: String): String {
        val digits = extractLocalDigits(input)
        if (digits.isEmpty()) return ""

        val b = StringBuilder("+7 (")
        b.append(digits.take(3))
        if (digits.length < 3) return b.toString()

        b.append(") ")
        if (digits.length > 3) {
            b.append(digits.drop(3).take(3))
        }
        if (digits.length > 6) {
            b.append("-").append(digits.drop(6).take(2))
        }
        if (digits.length > 8) {
            b.append("-").append(digits.drop(8).take(2))
        }
        return b.toString()
    }

    fun extractLocalDigits(input: String): String {
        var digits = input.filter { it.isDigit() }
        if (digits.length >= 11 && (digits.first() == '7' || digits.first() == '8')) {
            digits = digits.drop(1)
        }
        return digits.take(10)
    }
}
