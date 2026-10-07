package com.example.builder

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipAlignAndPacker {

    private class EntryData(
        val name: String,
        val bytes: ByteArray,
        val isStored: Boolean
    )

    fun repackAndAlign(
        templateApkStream: InputStream,
        patchedManifest: ByteArray,
        patchedArsc: ByteArray,
        icons: IconGenerator.GeneratedIcons,
        appConfigJson: String,
        webFiles: Map<String, ByteArray>,
        outputFile: File
    ) {
        val entries = LinkedHashMap<String, EntryData>()

        // 1. Read template APK entries
        ZipInputStream(templateApkStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                // Skip signatures in META-INF
                if (!name.startsWith("META-INF/")) {
                    val baos = ByteArrayOutputStream()
                    val buf = ByteArray(8192)
                    var read: Int
                    while (zis.read(buf).also { read = it } != -1) {
                        baos.write(buf, 0, read)
                    }
                    val data = baos.toByteArray()
                    // By default, preserve STORED vs DEFLATED
                    val shouldStore = shouldBeStored(name)
                    entries[name] = EntryData(name, data, shouldStore)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // 2. Replace AndroidManifest.xml (can be deflated or stored)
        entries["AndroidManifest.xml"] = EntryData("AndroidManifest.xml", patchedManifest, false)

        // 3. Replace resources.arsc (MUST BE STORED!)
        entries["resources.arsc"] = EntryData("resources.arsc", patchedArsc, true)

        // 4. Replace launcher icons
        for ((density, pngBytes) in icons.squareIcons) {
            val path = "res/mipmap-${density.folderSuffix}/ic_launcher.png"
            entries[path] = EntryData(path, pngBytes, false)
        }
        for ((density, pngBytes) in icons.roundIcons) {
            val path = "res/mipmap-${density.folderSuffix}/ic_launcher_round.png"
            entries[path] = EntryData(path, pngBytes, false)
        }

        // 5. Replace/Add assets/app_config.json (MUST BE STORED!)
        entries["assets/app_config.json"] = EntryData(
            "assets/app_config.json",
            appConfigJson.toByteArray(Charsets.UTF_8),
            true
        )

        // 6. Replace/Add web files under assets/web/ (MUST BE STORED!)
        for ((relPath, fileBytes) in webFiles) {
            val cleanPath = "assets/web/" + relPath.removePrefix("/")
            entries[cleanPath] = EntryData(cleanPath, fileBytes, true)
        }

        // 7. Write strictly aligned zip
        writeAlignedZip(entries.values.toList(), outputFile)
    }

    private fun shouldBeStored(name: String): Boolean {
        if (name == "resources.arsc") return true
        if (name.startsWith("assets/")) return true
        return false
    }

    private fun writeAlignedZip(entries: List<EntryData>, outputFile: File) {
        val fos = FileOutputStream(outputFile)
        var currentOffset = 0L

        class CentralDirRecord(
            val name: String,
            val crc: Long,
            val compressedSize: Long,
            val uncompressedSize: Long,
            val method: Int,
            val localHeaderOffset: Long,
            val extra: ByteArray
        )

        val cdRecords = ArrayList<CentralDirRecord>()

        for (entry in entries) {
            val nameBytes = entry.name.toByteArray(Charsets.UTF_8)
            val crcCalc = CRC32()
            crcCalc.update(entry.bytes)
            val crc = crcCalc.value

            val method: Int
            val payloadBytes: ByteArray

            if (entry.isStored) {
                method = 0 // STORED
                payloadBytes = entry.bytes
            } else {
                method = 8 // DEFLATED
                val baos = ByteArrayOutputStream()
                val def = Deflater(Deflater.DEFAULT_COMPRESSION, true)
                DeflaterOutputStream(baos, def).use { it.write(entry.bytes) }
                payloadBytes = baos.toByteArray()
            }

            // Calculate padding needed for 4-byte alignment
            var extraBytes = ByteArray(0)
            if (entry.isStored) {
                val headerBaseLen = 30 + nameBytes.size
                val rawDataOffset = currentOffset + headerBaseLen
                val remainder = (rawDataOffset % 4).toInt()
                if (remainder != 0) {
                    val padLen = 4 - remainder
                    extraBytes = ByteArray(padLen)
                }
            }

            val localHeaderOffset = currentOffset

            // Write Local File Header (30 bytes + name + extra)
            val lfh = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN)
            lfh.putInt(0x04034b50) // Local file header signature
            lfh.putShort(20.toShort()) // Version needed to extract (2.0)
            lfh.putShort(0.toShort())  // General purpose bit flag
            lfh.putShort(method.toShort()) // Compression method
            lfh.putShort(0.toShort())  // File last mod time
            lfh.putShort(0.toShort())  // File last mod date
            lfh.putInt(crc.toInt())    // CRC-32
            lfh.putInt(payloadBytes.size) // Compressed size
            lfh.putInt(entry.bytes.size)  // Uncompressed size
            lfh.putShort(nameBytes.size.toShort()) // File name length
            lfh.putShort(extraBytes.size.toShort()) // Extra field length

            fos.write(lfh.array())
            fos.write(nameBytes)
            if (extraBytes.isNotEmpty()) {
                fos.write(extraBytes)
            }
            fos.write(payloadBytes)

            val totalEntryLen = 30 + nameBytes.size + extraBytes.size + payloadBytes.size
            currentOffset += totalEntryLen

            cdRecords.add(
                CentralDirRecord(
                    name = entry.name,
                    crc = crc,
                    compressedSize = payloadBytes.size.toLong(),
                    uncompressedSize = entry.bytes.size.toLong(),
                    method = method,
                    localHeaderOffset = localHeaderOffset,
                    extra = extraBytes
                )
            )
        }

        // Write Central Directory
        val cdStartOffset = currentOffset
        var cdTotalSize = 0L

        for (rec in cdRecords) {
            val nameBytes = rec.name.toByteArray(Charsets.UTF_8)
            val cdh = ByteBuffer.allocate(46).order(ByteOrder.LITTLE_ENDIAN)
            cdh.putInt(0x02014b50) // Central directory header signature
            cdh.putShort(20.toShort()) // Version made by
            cdh.putShort(20.toShort()) // Version needed to extract
            cdh.putShort(0.toShort())  // General purpose bit flag
            cdh.putShort(rec.method.toShort()) // Compression method
            cdh.putShort(0.toShort())  // Last mod time
            cdh.putShort(0.toShort())  // Last mod date
            cdh.putInt(rec.crc.toInt())
            cdh.putInt(rec.compressedSize.toInt())
            cdh.putInt(rec.uncompressedSize.toInt())
            cdh.putShort(nameBytes.size.toShort())
            cdh.putShort(0.toShort()) // Extra field length in CD
            cdh.putShort(0.toShort()) // File comment length
            cdh.putShort(0.toShort()) // Disk number start
            cdh.putShort(0.toShort()) // Internal file attributes
            cdh.putInt(0)             // External file attributes
            cdh.putInt(rec.localHeaderOffset.toInt())

            fos.write(cdh.array())
            fos.write(nameBytes)

            val recordLen = 46 + nameBytes.size
            cdTotalSize += recordLen
            currentOffset += recordLen
        }

        // Write End of Central Directory Record (22 bytes)
        val eocd = ByteBuffer.allocate(22).order(ByteOrder.LITTLE_ENDIAN)
        eocd.putInt(0x06054b50) // EOCD signature
        eocd.putShort(0.toShort()) // Number of this disk
        eocd.putShort(0.toShort()) // Disk where central directory starts
        eocd.putShort(cdRecords.size.toShort()) // Number of central directory records on this disk
        eocd.putShort(cdRecords.size.toShort()) // Total number of central directory records
        eocd.putInt(cdTotalSize.toInt())        // Size of central directory
        eocd.putInt(cdStartOffset.toInt())      // Offset of start of central directory
        eocd.putShort(0.toShort())             // Comment length

        fos.write(eocd.array())
        fos.flush()
        fos.close()
    }
}
