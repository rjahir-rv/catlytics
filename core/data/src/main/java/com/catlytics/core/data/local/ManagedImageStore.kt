package com.catlytics.core.data.local

import android.content.Context
import androidx.core.net.toUri
import java.io.File
import java.util.UUID

internal class ManagedImageStore(
    private val context: Context?,
    private val directoryName: String,
) {
    /** Copies [sourceUri] into the store and returns the new absolute path, or null on failure. */
    fun copyFrom(sourceUri: String, ownerId: String): String? {
        val ctx = context ?: return null
        val target = newFile(ctx, ownerId)
        return try {
            val input = ctx.contentResolver.openInputStream(sourceUri.toUri()) ?: return null
            input.use { source -> target.outputStream().use(source::copyTo) }
            target.absolutePath
        } catch (_: Exception) {
            target.delete()
            null
        }
    }

    fun write(bytes: ByteArray, ownerId: String): String? {
        val ctx = context ?: return null
        val target = newFile(ctx, ownerId)
        return runCatching {
            target.writeBytes(bytes)
            target.absolutePath
        }.getOrElse {
            target.delete()
            null
        }
    }

    fun read(path: String?, maxBytes: Long): ByteArray? {
        val file = path?.let(::managedFile) ?: return null
        return runCatching {
            file.takeIf { it.isFile && it.length() in 1..maxBytes }?.readBytes()
        }.getOrNull()
    }

    /** Deletes [path] only when it is a file this store created for [ownerId]. */
    fun delete(ownerId: String, path: String?) {
        val file = path?.let(::managedFile) ?: return
        val prefix = ownerId.toFileNamePart()
        val isOwned = file.name == "$prefix.cover" || file.name.startsWith("$prefix-")
        if (isOwned) file.delete()
    }

    fun deleteAllExcept(keepPaths: Set<String>) {
        val keep = keepPaths.mapNotNullTo(HashSet()) { path ->
            managedFile(path)?.let { runCatching { it.canonicalPath }.getOrNull() }
        }
        context?.filesDir?.resolve(directoryName)?.listFiles()
            ?.filterNot { runCatching { it.canonicalPath }.getOrNull() in keep }
            ?.forEach(File::delete)
    }

    private fun managedFile(value: String): File? {
        val ctx = context ?: return null
        val uri = value.toUri()
        val file = when {
            uri.scheme == null || File(value).isAbsolute -> File(value)
            uri.scheme == "file" -> uri.path?.let(::File)
            else -> null
        } ?: return null
        val directory = ctx.filesDir.resolve(directoryName)
        val isManaged = runCatching {
            file.parentFile?.canonicalFile == directory.canonicalFile
        }.getOrDefault(false)
        return file.takeIf { isManaged }
    }

    private fun newFile(ctx: Context, ownerId: String): File {
        val directory = ctx.filesDir.resolve(directoryName).apply { mkdirs() }
        return File(directory, "${ownerId.toFileNamePart()}-${UUID.randomUUID()}.cover")
    }
}

private fun String.toFileNamePart(): String = replace(Regex("[^A-Za-z0-9_.-]"), "_")
