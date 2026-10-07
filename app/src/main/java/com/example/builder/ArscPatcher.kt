package com.example.builder

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ArscPatcher {

    private const val CHUNK_RES_TABLE = 0x0002
    private const val CHUNK_STRING_POOL = 0x0001
    private const val UTF8_FLAG = 0x00000100

    fun patchResourcesArsc(
        originalArsc: ByteArray,
        newAppName: String
    ): ByteArray {
        val buffer = ByteBuffer.wrap(originalArsc).order(ByteOrder.LITTLE_ENDIAN)

        val tableType = buffer.short.toInt() and 0xFFFF
        val tableHeaderSize = buffer.short.toInt() and 0xFFFF
        val tableSize = buffer.int
        val packageCount = buffer.int

        if (tableType != CHUNK_RES_TABLE) {
            throw IllegalArgumentException("Invalid ARSC table header: 0x${Integer.toHexString(tableType)}")
        }

        // Global string pool starts immediately after table header (offset 12)
        val spType = buffer.short.toInt() and 0xFFFF
        val spHeaderSize = buffer.short.toInt() and 0xFFFF
        val spChunkSize = buffer.int
        val stringCount = buffer.int
        val styleCount = buffer.int
        val flags = buffer.int
        val stringsStart = buffer.int
        val stylesStart = buffer.int

        val isUtf8 = (flags and UTF8_FLAG) != 0

        val stringOffsets = IntArray(stringCount)
        for (i in 0 until stringCount) {
            stringOffsets[i] = buffer.int
        }

        val styleOffsets = IntArray(styleCount)
        for (i in 0 until styleCount) {
            styleOffsets[i] = buffer.int
        }

        val spStartOffset = 12
        val stringsDataOffset = spStartOffset + stringsStart

        val strings = ArrayList<String>(stringCount)
        for (i in 0 until stringCount) {
            val offset = stringsDataOffset + stringOffsets[i]
            if (isUtf8) {
                var p = offset
                var u16len = originalArsc[p].toInt() and 0xFF
                p++
                if ((u16len and 0x80) != 0) p++
                var u8len = originalArsc[p].toInt() and 0xFF
                p++
                if ((u8len and 0x80) != 0) {
                    u8len = ((u8len and 0x7F) shl 8) or (originalArsc[p].toInt() and 0xFF)
                    p++
                }
                strings.add(String(originalArsc, p, u8len, Charsets.UTF_8))
            } else {
                val u16len = (originalArsc[offset].toInt() and 0xFF) or
                        ((originalArsc[offset + 1].toInt() and 0xFF) shl 8)
                val p = offset + 2
                strings.add(String(originalArsc, p, u16len * 2, Charsets.UTF_16LE))
            }
        }

        // Replace template app name
        val templateName = "WebToAPK Template"
        for (i in 0 until strings.size) {
            if (strings[i] == templateName) {
                strings[i] = newAppName
            }
        }

        // Rebuild string data in UTF-8
        val strBytesBaos = ByteArrayOutputStream()
        val newStringOffsets = IntArray(stringCount)

        for (i in 0 until stringCount) {
            newStringOffsets[i] = strBytesBaos.size()
            val str = strings[i]
            val encoded = str.toByteArray(Charsets.UTF_8)
            val charLen = str.length
            if (charLen > 127) {
                strBytesBaos.write(((charLen shr 8) and 0x7F) or 0x80)
                strBytesBaos.write(charLen and 0xFF)
            } else {
                strBytesBaos.write(charLen)
            }
            val byteLen = encoded.size
            if (byteLen > 127) {
                strBytesBaos.write(((byteLen shr 8) and 0x7F) or 0x80)
                strBytesBaos.write(byteLen and 0xFF)
            } else {
                strBytesBaos.write(byteLen)
            }
            strBytesBaos.write(encoded)
            strBytesBaos.write(0)
        }

        while (strBytesBaos.size() % 4 != 0) {
            strBytesBaos.write(0)
        }

        val styleBytes = if (styleCount > 0) {
            val start = spStartOffset + stylesStart
            val end = spStartOffset + spChunkSize
            originalArsc.copyOfRange(start, end)
        } else {
            ByteArray(0)
        }

        val newStringsStart = 28 + (stringCount * 4) + (styleCount * 4)
        val newStylesStart = if (styleCount > 0) newStringsStart + strBytesBaos.size() else 0
        var newSpSize = newStringsStart + strBytesBaos.size() + styleBytes.size
        while (newSpSize % 4 != 0) {
            newSpSize++
            strBytesBaos.write(0)
        }

        // Construct new StringPool Chunk
        val newSpBaos = ByteArrayOutputStream(newSpSize)
        val spHeaderBuf = ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN)
        spHeaderBuf.putShort(CHUNK_STRING_POOL.toShort())
        spHeaderBuf.putShort(28.toShort())
        spHeaderBuf.putInt(newSpSize)
        spHeaderBuf.putInt(stringCount)
        spHeaderBuf.putInt(styleCount)
        spHeaderBuf.putInt(flags)
        spHeaderBuf.putInt(newStringsStart)
        spHeaderBuf.putInt(newStylesStart)
        newSpBaos.write(spHeaderBuf.array())

        val offsetsBuf = ByteBuffer.allocate((stringCount + styleCount) * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (off in newStringOffsets) {
            offsetsBuf.putInt(off)
        }
        for (off in styleOffsets) {
            offsetsBuf.putInt(off)
        }
        newSpBaos.write(offsetsBuf.array())
        newSpBaos.write(strBytesBaos.toByteArray())
        if (styleBytes.isNotEmpty()) {
            newSpBaos.write(styleBytes)
        }

        val newSpBytes = newSpBaos.toByteArray()

        // Remainder of ARSC table
        val afterSpOffset = spStartOffset + spChunkSize
        val afterSpBytes = originalArsc.copyOfRange(afterSpOffset, originalArsc.size)

        val newTableSize = 12 + newSpBytes.size + afterSpBytes.size
        val outBaos = ByteArrayOutputStream(newTableSize)

        val tableHeaderBuf = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
        tableHeaderBuf.putShort(CHUNK_RES_TABLE.toShort())
        tableHeaderBuf.putShort(12.toShort())
        tableHeaderBuf.putInt(newTableSize)
        tableHeaderBuf.putInt(packageCount)

        outBaos.write(tableHeaderBuf.array())
        outBaos.write(newSpBytes)
        outBaos.write(afterSpBytes)

        return outBaos.toByteArray()
    }
}
