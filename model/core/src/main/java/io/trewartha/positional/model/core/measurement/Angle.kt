package io.trewartha.positional.model.core.measurement

/**
 * Angle abstraction
 */
sealed interface Angle {

    /**
     * Converts this angle to degrees
     */
    fun inDegrees(): Degrees

    /**
     * Adds another angle to this angle
     */
    operator fun plus(other: Angle): Angle

    /**
     * Angle in degrees
     *
     * @property value The magnitude of the angle
     */
    @JvmInline
    value class Degrees @Throws(IllegalArgumentException::class) constructor(
        val value: Float
    ) : Angle {

        init {
            require(value.isFinite()) { "Value '$value' is not finite" }
        }

        override fun inDegrees(): Degrees = this

        override operator fun plus(other: Angle): Angle =
            Degrees((value + other.inDegrees().value + DEGREES_360) % DEGREES_360)

        override fun toString(): String = "${value}°"
    }
}

private const val DEGREES_360 = 360f
