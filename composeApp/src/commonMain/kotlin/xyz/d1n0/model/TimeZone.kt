package xyz.d1n0.model

import xyz.d1n0.constant.timezoneRadioIdData
import xyz.d1n0.helper.to2BytesLittleEndian

abstract class Timezone {
    abstract val cityName: String
    abstract val identifier: Short
    abstract val offset: Double
    abstract val dstDiff: Double
    abstract val dstRules: Byte

    /**
     * Converts the identifier integer into a little-endian 2-byte array.
     */
    val identifierBytes: ByteArray
        get() = identifier.to2BytesLittleEndian()

    /**
     * Represents a 5-byte array constructed from components of the TimeZone object.
     * The array includes a 2-byte little-endian identifier, a 1-byte offset,
     * a 1-byte daylight saving time (DST) offset, and a 1-byte DST rules.
     */
    val bytes: ByteArray
        get() = byteArrayOf(
            *identifierBytes,
            offsetByte,
            dstOffsetByte,
            dstRules,
        )

    /**
     * Converts the city name to a byte array of 18 bytes.
     * Each character of the city name is encoded, and the result is filled with zeros if the name
     * is shorter than 18 bytes.
     *
     * @throws IllegalArgumentException if the city name cannot be encoded to a byte array
     * properly.
     */
    val cityNameBytes: ByteArray
        get() = cityName.encodeToByteArray().copyOf(18)

    /**
     * Converts the offset, which is represented as a double indicating the time zone offset in hours, into a single byte.
     * The conversion scales the offset by a factor of 4 because the offset is stored in 15-minute intervals.
     * This means every unit increase in the byte value represents a 15-minute difference in the time zone offset.
     * Thus, an offset of 1.0 (hour) is converted to a byte value of 4, representing a total of 4 * 15 minutes = 60 minutes.
     * This byte can then be used to form part of the 5-byte array describing the time zone.
     */
    val offsetByte: Byte
        get() = (offset * 4).toInt().toByte()

    /**
     * Converts the daylight saving time difference (dstDiff) from hours into a single byte for compact storage.
     * The dstDiff is multiplied by 4 to fit it into 15-minute intervals, similar to offset conversion.
     * This scaled value provides a means to represent the DST offset adjustment within the time zone structure.
     * It enables precise calculation of DST time changes, packaged for streamlined data handling in the byte array.
     */
    val dstOffsetByte: Byte
        get() = (dstDiff * 4).toInt().toByte()

    /**
     * Retrieves the multiband 6 radio ID associated with the specified timezone offset.
     *
     * @param timezoneId The timezone identifier (e.g., "Australia/Adelaide") to find the radio ID for.
     * @return The corresponding integer radio ID if found; otherwise, returns 0 if no matching timezone ID exists.
     */
    val radioId: Int
        get() = timezoneRadioIdData.entries.firstOrNull {
            it.key.toDouble() == offset
        }?.value ?: 0

    val radioIdByte: Byte
        get() = radioId.toByte()

    open abstract val coordinatesBytes: ByteArray

}
