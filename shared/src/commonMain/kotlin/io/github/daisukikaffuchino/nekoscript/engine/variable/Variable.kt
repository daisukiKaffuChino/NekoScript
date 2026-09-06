package io.github.daisukikaffuchino.nekoscript.engine.variable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A typed value stored by a game script. */
@Serializable
sealed interface Variable {

    /** @property value integer value exposed to the script */
    @Serializable
    @SerialName("int")
    data class IntValue(val value: Int) : Variable

    /** @property value boolean value exposed to the script */
    @Serializable
    @SerialName("boolean")
    data class BooleanValue(val value: Boolean) : Variable

    /** @property value floating-point value exposed to the script */
    @Serializable
    @SerialName("double")
    data class DoubleValue(val value: Double) : Variable

    /** @property value string value exposed to the script */
    @Serializable
    @SerialName("string")
    data class StringValue(val value: String) : Variable
}
