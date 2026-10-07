package com.example.builder

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ManifestPatcher {

    private const val CHUNK_XML_ROOT = 0x00080003
    private const val CHUNK_STRING_POOL = 0x001C0001
    private const val UTF8_FLAG = 0x00000100

    fun patchManifest(
        originalAxml: ByteArray,
        newPackageName: String
    ): ByteArray {
        val buffer = ByteBuffer.wrap(originalAxml).order(ByteOrder.LITTLE_ENDIAN)

        val rootChunkType = buffer.int
        val rootChunkSize = buffer.int
        if (rootChunkType != CHUNK_XML_ROOT) {
            throw IllegalArgumentException("Invalid AXML root header: 0x${Integer.toHexString(rootChunkType)}")
        }

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

        // Read strings
        val strings = ArrayList<String>(stringCount)
        val spDataOffset = 8 + stringsStart

        for (i in 0 until stringCount) {
            val offset = spDataOffset + stringOffsets[i]
            if (isUtf8) {
                var p = offset
                var u16len = originalAxml[p].toInt() and 0xFF
                p++
                if ((u16len and 0x80) != 0) p++
                var u8len = originalAxml[p].toInt() and 0xFF
                p++
                if ((u8len and 0x80) != 0) {
                    u8len = ((u8len and 0x7F) shl 8) or (originalAxml[p].toInt() and 0xFF)
                    p++
                }
                strings.add(String(originalAxml, p, u8len, Charsets.UTF_8))
            } else {
                val u16len = (originalAxml[offset].toInt() and 0xFF) or
                        ((originalAxml[offset + 1].toInt() and 0xFF) shl 8)
                val p = offset + 2
                strings.add(String(originalAxml, p, u16len * 2, Charsets.UTF_16LE))
            }
        }

        // Replace target strings
        val oldPackage = "com.webtoapk.template"
        val oldAuthority = "com.webtoapk.template.fileprovider"
        val newAuthority = "$newPackageName.fileprovider"

        for (i in 0 until strings.size) {
            if (strings[i] == oldPackage) {
                strings[i] = newPackageName
            } else if (strings[i] == oldAuthority) {
                strings[i] = newAuthority
            }
            // CRITICAL: MainActivity fully qualified class name is intentionally preserved!
        }

        // Rebuild string pool data (UTF-16LE as original)
        val strBytesBaos = ByteArrayOutputStream()
        val newStringOffsets = IntArray(stringCount)

        for (i in 0 until stringCount) {
            newStringOffsets[i] = strBytesBaos.size()
            val str = strings[i]
            if (isUtf8) {
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
            } else {
                val len = str.length
                strBytesBaos.write(len and 0xFF)
                strBytesBaos.write((len shr 8) and 0xFF)
                strBytesBaos.write(str.toByteArray(Charsets.UTF_16LE))
                strBytesBaos.write(0)
                strBytesBaos.write(0)
            }
        }

        // 4-byte align string bytes
        while (strBytesBaos.size() % 4 != 0) {
            strBytesBaos.write(0)
        }

        val styleBytes = if (styleCount > 0) {
            val start = 8 + stylesStart
            val end = 8 + spChunkSize
            originalAxml.copyOfRange(start, end)
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
        spHeaderBuf.putShort(0x0001.toShort())
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

        // Assemble full AXML
        val afterSpOffset = 8 + spChunkSize
        val afterSpBytes = originalAxml.copyOfRange(afterSpOffset, originalAxml.size)

        val outBaos = ByteArrayOutputStream(8 + newSpBytes.size + afterSpBytes.size)
        val newRootSize = 8 + newSpBytes.size + afterSpBytes.size

        val rootHeaderBuf = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        rootHeaderBuf.putInt(CHUNK_XML_ROOT)
        rootHeaderBuf.putInt(newRootSize)

        outBaos.write(rootHeaderBuf.array())
        outBaos.write(newSpBytes)
        outBaos.write(afterSpBytes)

        return outBaos.toByteArray()
    }
}
