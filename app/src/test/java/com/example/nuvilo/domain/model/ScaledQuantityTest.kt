package com.example.nuvilo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaledQuantityTest {

    @Test
    fun wholeQuantities_scaleAccurately() {
        val qty = ScaledQuantity.ofWhole(5)
        assertEquals(5_000L, qty.rawScaledValue)
        assertEquals("5", qty.toDisplayString())
    }

    @Test
    fun decimalQuantities_scaleWithoutFloatingPoint() {
        val half = ScaledQuantity.fromDisplayString("0.5")
        assertEquals(500L, half.rawScaledValue)
        assertEquals("0.5", half.toDisplayString())

        val twoAndQuarter = ScaledQuantity.fromDisplayString("2.25")
        assertEquals(2_250L, twoAndQuarter.rawScaledValue)
        assertEquals("2.25", twoAndQuarter.toDisplayString())
    }

    @Test
    fun additionAndSubtraction_maintainExactPrecision() {
        val a = ScaledQuantity.fromDisplayString("10.5")
        val b = ScaledQuantity.fromDisplayString("2.5")
        val sum = a + b
        val diff = a - b

        assertEquals(13_000L, sum.rawScaledValue)
        assertEquals("13", sum.toDisplayString())
        assertEquals(8_000L, diff.rawScaledValue)
        assertEquals("8", diff.toDisplayString())
    }

    @Test
    fun negativeQuantities_areIdentified() {
        val pos = ScaledQuantity.ofWhole(5)
        val neg = ScaledQuantity.ofWhole(-3)
        val zero = ScaledQuantity.ZERO

        assertTrue(neg.isNegative())
        assertFalse(pos.isNegative())
        assertTrue(zero.isZero())
    }

    @Test
    fun comparisonOperators_workCorrectly() {
        val small = ScaledQuantity.fromDisplayString("1.5")
        val large = ScaledQuantity.fromDisplayString("2.0")

        assertTrue(small < large)
        assertTrue(large > small)
        assertTrue(small <= ScaledQuantity.fromDisplayString("1.5"))
    }
}
