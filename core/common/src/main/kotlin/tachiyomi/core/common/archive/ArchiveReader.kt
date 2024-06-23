package tachiyomi.core.archive

import android.content.Context
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import com.hippo.unifile.UniFile
import tachiyomi.core.common.storage.openFileDescriptor
import java.io.Closeable
import java.io.InputStream

class ArchiveReader(pfd: ParcelFileDescriptor) : Closeable {
    val size = pfd.statSize
    val address = Os.mmap(0, size, OsConstants.PROT_READ, OsConstants.MAP_PRIVATE, pfd.fileDescriptor, 0)

    inline fun <T> useEntries(block: (Sequence<Archive.Entry>) -> T): T =
        Archive(address, size).use { block(generateSequence { it.readEntry() }) }

    fun getInputStream(entryName: String): InputStream? {
        val archive = Archive(address, size)
        try {
            while (true) {
                val entry = archive.readEntry() ?: break
                if (entry.name == entryName) {
                    return archive
                }
            }
        } catch (e: Throwable) {
            archive.close()
            throw e
        }
        archive.close()
        return null
    }

    override fun close() {
        Os.munmap(address, size)
    }
}

fun UniFile.archiveReader(context: Context) = openFileDescriptor(context, "r").use { ArchiveReader(it) }
