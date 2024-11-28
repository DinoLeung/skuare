package xyz.d1n0.model

enum class DstStatus (val value: Int) {
    // 00000000
    MANUAL_OFF(0),
    // 00000001
    MANUAL_ON(1),
    // 00000010
    AUTO_OFF(3),
    // 00000011
    AUTO_ON(4);
}
