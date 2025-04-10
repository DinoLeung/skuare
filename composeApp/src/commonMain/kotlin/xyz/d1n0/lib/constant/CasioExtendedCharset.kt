package xyz.d1n0.lib.constant

val fallbackByte = 0xFF.toByte()
val fallbackChar = '■'

val customCharset: Map<Int, Map<Int, Char>> = mapOf(
    0x8 to mapOf(
        0x0 to '¥',
        0x1 to ':',
        0x2 to '《',
        0x3 to '￢',
        0x4 to '‐',
        0x5 to '￣',
        0x6 to 'ﾟ',
        0x7 to '±',
        0x8 to '´',
        0x9 to '･',
        0xA to ',',
        0xB to '》',
        0xC to '◆',
        0xD to '♪',
    ),
    0x9 to mapOf(
        0x0 to '【',
        0x1 to '】',
        0x2 to '◀',
        0x3 to '▶',
    )
)

val customCharsetByte: Map<Char, Byte> = customCharset.flatMap { (row, colMap) ->
    colMap.map { (col, char) ->
        char to ((row shl 4) or col).toByte()
    }
}.toMap()

val jisX0201Charset: Map<Int, Map<Int, Char>> = mapOf(
    0xA to mapOf(
        0x1 to '｡',
        0x2 to '｢',
        0x3 to '｣',
        0x4 to '､',
        0x5 to '･',
        0x6 to 'ｦ',
        0x7 to 'ｧ',
        0x8 to 'ｨ',
        0x9 to 'ｩ',
        0xA to 'ｪ',
        0xB to 'ｫ',
        0xC to 'ｬ',
        0xD to 'ｭ',
        0xE to 'ｮ',
        0xF to 'ｯ'
    ),
    0xB to mapOf(
        0x0 to 'ｰ',
        0x1 to 'ｱ',
        0x2 to 'ｲ',
        0x3 to 'ｳ',
        0x4 to 'ｴ',
        0x5 to 'ｵ',
        0x6 to 'ｶ',
        0x7 to 'ｷ',
        0x8 to 'ｸ',
        0x9 to 'ｹ',
        0xA to 'ｺ',
        0xB to 'ｻ',
        0xC to 'ｼ',
        0xD to 'ｽ',
        0xE to 'ｾ',
        0xF to 'ｿ'
    ),
    0xC to mapOf(
        0x0 to 'ﾀ',
        0x1 to 'ﾁ',
        0x2 to 'ﾂ',
        0x3 to 'ﾃ',
        0x4 to 'ﾄ',
        0x5 to 'ﾅ',
        0x6 to 'ﾆ',
        0x7 to 'ﾇ',
        0x8 to 'ﾈ',
        0x9 to 'ﾉ',
        0xA to 'ﾊ',
        0xB to 'ﾋ',
        0xC to 'ﾌ',
        0xD to 'ﾍ',
        0xE to 'ﾎ',
        0xF to 'ﾏ'
    ),
    0xD to mapOf(
        0x0 to 'ﾐ',
        0x1 to 'ﾑ',
        0x2 to 'ﾒ',
        0x3 to 'ﾓ',
        0x4 to 'ﾔ',
        0x5 to 'ﾕ',
        0x6 to 'ﾖ',
        0x7 to 'ﾗ',
        0x8 to 'ﾘ',
        0x9 to 'ﾙ',
        0xA to 'ﾚ',
        0xB to 'ﾛ',
        0xC to 'ﾜ',
        0xD to 'ﾝ',
        0xE to 'ﾞ',
        0xF to 'ﾟ'
    ),
)

val jisX0201CharsetByte: Map<Char, Byte> = jisX0201Charset.flatMap { (row, colMap) ->
    colMap.map { (col, char) ->
        char to ((row shl 4) or col).toByte()
    }
}.toMap()

// Casio custom charset table
//     0123456789ABCDEF
// 0x1 ■■■■■■■■■■■■■■■■
// 0x2  !"#$%&'()*+,-./
// 0x3 0123456789:;<=>?
// 0x4 @ABCDEFGHIJKLMNO
// 0x5 PQRSTUVWXYZ[\]^_
// 0x6 `abcdefghijklmno
// 0x7 pqrstuvwxyz{|}~
// 0x8 ¥:《￢‐￣ﾟ±´･,》◆♪■■
// 0x9 【】◀▶■■■■■■■■■■■■
// 0xA ■｡｢｣､･ｦｧｨｩｪｫｬｭｮｯ
// 0xB ｰｱｲｳｴｵｶｷｸｹｺｻｼｽｾｿ
// 0xC ﾀﾁﾂﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏ
// 0xD ﾐﾑﾒﾓﾔﾕﾖﾗﾘﾙﾚﾛﾜﾝﾞﾟ
// 0xE ■■■■■■■■■■■■■■■■
// 0xF ■■■■■■■■■■■■■■■■
//
// 0x20 to 0x7F ASCII
// 0x80 to 0x9F custom
// 0xA0 to 0xFF JIS X 0201
// 0x00 to 0x0F writing to the wat will always results into 0x00
// uses ■ as fall back character
