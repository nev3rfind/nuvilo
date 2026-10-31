package com.example.nuvilo.domain.model

/**
 * An immutable, exact fixed-point scaled quantity for medication stock and dose quantities.
 *
 * Internally scales all values by [SCALE] (1,000), representing thousandths of a unit.
 * For example:
 * - 1 tablet = 1,000 raw
 * - 0.5 tablets = 500 raw
 * - 2.25 ml = 2,250 raw
 *
 * Strictly prevents floating-point inaccuracy or cumulative balance drift.
 */
data class ScaledQuantity(val rawScaledValue: Long) : Comparable<ScaledQuantity> {

    operator fun plus(other: ScaledQuantity): ScaledQuantity =
        ScaledQuantity(rawScaledValue + other.rawScaledValue)

    operator fun minus(other: ScaledQuantity): ScaledQuantity =
        ScaledQuantity(rawScaledValue - other.rawScaledValue)

    operator fun times(multiplier: Long): ScaledQuantity =
        ScaledQuantity(rawScaledValue * multiplier)

    operator fun unaryMinus(): ScaledQuantity =
        ScaledQuantity(-rawScaledValue)

    override fun compareTo(other: ScaledQuantity): Int =
        rawScaledValue.compareTo(other.rawScaledValue)

    fun isZero(): Boolean = rawScaledValue == 0L
    fun isPositive(): Boolean = rawScaledValue > 0L
    fun isNegative(): Boolean = rawScaledValue < 0L

    fun abs(): ScaledQuantity = ScaledQuantity(kotlin.math.abs(rawScaledValue))

    /**
     * Converts to clean user-facing display string without trailing decimal zeros.
     * E.g. 5,000 -> "5", 500 -> "0.5", 2,250 -> "2.25".
     */
    fun toDisplayString(): String {
        val sign = if (rawScaledValue < 0) "-" else ""
        val absVal = kotlin.math.abs(rawScaledValue)
        val whole = absVal / SCALE
        val fraction = absVal % SCALE

        return if (fraction == 0L) {
            "$sign$whole"
        } else {
            val fractionStr = String.format("%03d", fraction).trimEnd('0')
            "$sign$whole.$fractionStr"
        }
    }

    companion object {
        const val SCALE = 1_000L

        val ZERO = ScaledQuantity(0L)
        val ONE = ScaledQuantity(SCALE)

        fun ofWhole(whole: Long): ScaledQuantity =
            ScaledQuantity(whole * SCALE)

        fun ofScaled(rawScaledValue: Long): ScaledQuantity =
            ScaledQuantity(rawScaledValue)

        fun ofFraction(numerator: Long, denominator: Long): ScaledQuantity {
            require(denominator != 0L) { "Denominator cannot be zero" }
            return ScaledQuantity((numerator * SCALE) / denominator)
        }

        /**
         * Parses a user-entered string (e.g. "10", "2.5", "0.25") into a [ScaledQuantity].
         */
        fun fromDisplayString(input: String): ScaledQuantity {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return ZERO

            val isNeg = trimmed.startsWith("-")
            val clean = if (isNeg) trimmed.substring(1) else trimmed

            val parts = clean.split(".")
            val whole = parts[0].toLongOrNull() ?: 0L
            val fractionRaw = if (parts.size > 1) parts[1] else "0"

            // Pad or truncate to 3 decimal places
            val fractionPadded = fractionRaw.padEnd(3, '0').take(3)
            val fraction = fractionPadded.toLongOrNull() ?: 0L

            val raw = whole * SCALE + fraction
            return ScaledQuantity(if (isNeg) -raw else raw)
        }
    }
}
