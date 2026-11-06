package com.example.nuvilo.regression

import com.example.nuvilo.domain.model.ScaledQuantity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression Test Suite: Category A (Domain Models & Accounting)
 * Tests T001 to T010: Exact Fixed-Point Scaled Arithmetic and Display Conversion
 */
class ScaledQuantityRegressionTest {

    @Test
    fun t001_scaledArithmeticAdditionAndSubtractionExactnessWithoutDrift() {
        // T001: Scaled arithmetic addition and subtraction exactness without drift
        val q1 = ScaledQuantity.ofFraction(1, 3) // 333
        val q2 = ScaledQuantity.ofFraction(2, 3) // 666
        val sum = q1 + q2
        assertEquals(999L, sum.rawScaledValue)

        val tenth = ScaledQuantity.fromDisplayString("0.1") // 100
        var accumulator = ScaledQuantity.ZERO
        for (i in 1..10) {
            accumulator += tenth
        }
        assertEquals(1000L, accumulator.rawScaledValue)
        assertEquals(ScaledQuantity.ONE, accumulator)

        val difference = accumulator - ScaledQuantity.fromDisplayString("0.35")
        assertEquals(650L, difference.rawScaledValue)
        assertEquals("0.65", difference.toDisplayString())
    }

    @Test
    fun t002_scalarMultiplicationWithLargeWholeNumbers() {
        // T002: Scalar multiplication with large whole numbers
        val unitDose = ScaledQuantity.fromDisplayString("2.5") // 2500 raw
        val monthlyMultiplier = 30L
        val totalNeeded = unitDose * monthlyMultiplier
        assertEquals(75000L, totalNeeded.rawScaledValue)
        assertEquals("75", totalNeeded.toDisplayString())

        val zeroProduct = unitDose * 0L
        assertEquals(ScaledQuantity.ZERO, zeroProduct)
    }

    @Test
    fun t003_unaryMinusAndZeroInvariants() {
        // T003: Unary minus and zero invariants
        val positive = ScaledQuantity.ofWhole(5)
        val negative = -positive
        assertEquals(-5000L, negative.rawScaledValue)
        assertTrue(negative.isNegative())
        assertFalse(negative.isPositive())
        assertFalse(negative.isZero())

        val zeroNeg = -ScaledQuantity.ZERO
        assertEquals(0L, zeroNeg.rawScaledValue)
        assertTrue(zeroNeg.isZero())

        val absVal = negative.abs()
        assertEquals(5000L, absVal.rawScaledValue)
        assertTrue(absVal.isPositive())
    }

    @Test
    fun t004_comparisonOrderingNegativeZeroPositive() {
        // T004: Comparison ordering (negative < zero < positive)
        val neg = ScaledQuantity.fromDisplayString("-1.5")
        val zero = ScaledQuantity.ZERO
        val half = ScaledQuantity.fromDisplayString("0.5")
        val whole = ScaledQuantity.ofWhole(1)

        assertTrue(neg < zero)
        assertTrue(zero < half)
        assertTrue(half < whole)
        assertTrue(whole > neg)
        assertEquals(0, half.compareTo(ScaledQuantity.ofScaled(500L)))
    }

    @Test
    fun t005_ofFractionExactDivisionToThousandths() {
        // T005: ofFraction scaling exact division to thousandths
        val half = ScaledQuantity.ofFraction(1, 2)
        assertEquals(500L, half.rawScaledValue)
        assertEquals("0.5", half.toDisplayString())

        val quarter = ScaledQuantity.ofFraction(1, 4)
        assertEquals(250L, quarter.rawScaledValue)
        assertEquals("0.25", quarter.toDisplayString())

        val eighth = ScaledQuantity.ofFraction(1, 8)
        assertEquals(125L, eighth.rawScaledValue)
        assertEquals("0.125", eighth.toDisplayString())
    }

    @Test
    fun t006_ofFractionRejectsZeroDenominatorWithIllegalArgumentException() {
        // T006: ofFraction rejects zero denominator with IllegalArgumentException
        assertThrows(IllegalArgumentException::class.java) {
            ScaledQuantity.ofFraction(5, 0)
        }
    }

    @Test
    fun t007_toDisplayStringFormattingForWholeNumbersWithoutTrailingZeros() {
        // T007: toDisplayString formatting for whole numbers (no trailing decimal zeros)
        assertEquals("0", ScaledQuantity.ZERO.toDisplayString())
        assertEquals("1", ScaledQuantity.ONE.toDisplayString())
        assertEquals("28", ScaledQuantity.ofWhole(28).toDisplayString())
        assertEquals("100", ScaledQuantity.ofWhole(100).toDisplayString())
        assertEquals("-15", ScaledQuantity.ofWhole(-15).toDisplayString())
    }

    @Test
    fun t008_toDisplayStringFormattingForFractionalAndHalfQuantities() {
        // T008: toDisplayString formatting for half and fractional quantities
        assertEquals("0.5", ScaledQuantity.ofScaled(500L).toDisplayString())
        assertEquals("2.25", ScaledQuantity.ofScaled(2250L).toDisplayString())
        assertEquals("0.125", ScaledQuantity.ofScaled(125L).toDisplayString())
        assertEquals("0.05", ScaledQuantity.ofScaled(50L).toDisplayString())
        assertEquals("0.005", ScaledQuantity.ofScaled(5L).toDisplayString())
        assertEquals("-0.75", ScaledQuantity.ofScaled(-750L).toDisplayString())
    }

    @Test
    fun t009_fromDisplayStringParsingOfVariousFormats() {
        // T009: fromDisplayString parsing of whole, decimal, padded, and negative numbers
        assertEquals(10000L, ScaledQuantity.fromDisplayString("10").rawScaledValue)
        assertEquals(2500L, ScaledQuantity.fromDisplayString("2.5").rawScaledValue)
        assertEquals(250L, ScaledQuantity.fromDisplayString("0.25").rawScaledValue)
        assertEquals(125L, ScaledQuantity.fromDisplayString("0.125").rawScaledValue)
        assertEquals(125L, ScaledQuantity.fromDisplayString("0.12599").rawScaledValue) // truncates past 3 decimals
        assertEquals(-3500L, ScaledQuantity.fromDisplayString("-3.5").rawScaledValue)
    }

    @Test
    fun t010_fromDisplayStringEmptyAndBlankReturnsZero() {
        // T010: fromDisplayString empty and blank strings return ZERO
        assertEquals(ScaledQuantity.ZERO, ScaledQuantity.fromDisplayString(""))
        assertEquals(ScaledQuantity.ZERO, ScaledQuantity.fromDisplayString("   "))
        assertEquals(ScaledQuantity.ZERO, ScaledQuantity.fromDisplayString("invalid"))
    }
}
