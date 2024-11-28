package xyz.d1n0.model

import xyz.d1n0.helper.BytesConverter

sealed class TimeZone(
    open val cityName: String,
    open val identifier: Int,
    open val offset: Double,
    open val dstDiff: Double,
    open val dstRules: Int
) {
    // Identifier bytes (converted to 2-byte representation)
    val identifierBytes: ByteArray
        get() = BytesConverter.intTo2Bytes(identifier)

    // 5 bytes representation
    val bytes: ByteArray
        get() = identifierBytes + byteArrayOf(offsetByte, dstOffsetByte, dstRules.toByte())

    // City name in 18 bytes
    val cityNameBytes: ByteArray
        get() {
            val cityBytes = cityName.encodeToByteArray()
            return ByteArray(18) { index -> if (index < cityBytes.size) cityBytes[index] else 0x00 }
        }

    // Offset byte (offset is in 15-minute intervals)
    val offsetByte: Byte
        get() = (offset * 4).toInt().toByte()

    // DST offset byte
    val dstOffsetByte: Byte
        get() = (dstDiff * 4).toInt().toByte()
}
