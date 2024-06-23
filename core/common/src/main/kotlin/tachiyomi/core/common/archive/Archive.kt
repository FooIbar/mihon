package tachiyomi.core.archive

import me.zhanghai.android.libarchive.Archive
import me.zhanghai.android.libarchive.ArchiveEntry
import java.io.InputStream
import java.nio.ByteBuffer

class Archive(buffer: Long, size: Long) : InputStream() {
    private val archive = Archive.readNew()

    init {
        try {
            Archive.setCharset(archive, Charsets.UTF_8.name().toByteArray())
            Archive.readSupportFilterAll(archive)
            Archive.readSupportFormatAll(archive)
            Archive.readOpenMemoryUnsafe(archive, buffer, size)
        } catch (e: Throwable) {
            close()
            throw e
        }
    }

    private val oneByteBuffer = ByteBuffer.allocateDirect(1)

    override fun read(): Int {
        read(oneByteBuffer)
        return if (oneByteBuffer.hasRemaining()) oneByteBuffer.get().toUByte().toInt() else -1
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val buffer = ByteBuffer.wrap(b, off, len)
        read(buffer)
        return if (buffer.hasRemaining()) buffer.remaining() else -1
    }

    private fun read(buffer: ByteBuffer) {
        buffer.clear()
        Archive.readData(archive, buffer)
        buffer.flip()
    }

    override fun close() {
        Archive.readFree(archive)
    }

    fun readEntry(): Entry? {
        val entry = Archive.readNextHeader(archive)
        if (entry == 0L) return null
        val name = ArchiveEntry.pathnameUtf8(entry) ?: ArchiveEntry.pathname(entry)?.decodeToString() ?: return null
        val isFile = ArchiveEntry.filetype(entry) == ArchiveEntry.AE_IFREG
        return Entry(name, isFile)
    }

    class Entry(val name: String, val isFile: Boolean)
}
