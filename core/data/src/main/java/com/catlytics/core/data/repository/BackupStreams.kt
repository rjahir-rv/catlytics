package com.catlytics.core.data.repository

import java.io.FilterInputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.coroutines.cancellation.CancellationException

internal class BackupTooLargeException : IllegalArgumentException(
    "El respaldo supera el límite permitido de 64 MB.",
)

internal class SizeLimitedInputStream(
    input: InputStream,
    private val maxBytes: Long,
) : FilterInputStream(input) {
    private var bytesRead = 0L

    override fun read(): Int {
        val value = super.read()
        if (value >= 0) accountFor(1)
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val count = super.read(buffer, offset, length)
        if (count > 0) accountFor(count)
        return count
    }

    private fun accountFor(count: Int) {
        bytesRead += count
        if (bytesRead > maxBytes) throw BackupTooLargeException()
    }
}

internal class SizeLimitedOutputStream(
    private val output: OutputStream,
    private val maxBytes: Long,
) : OutputStream() {
    private var bytesWritten = 0L

    override fun write(value: Int) {
        accountFor(1)
        output.write(value)
    }

    override fun write(buffer: ByteArray, offset: Int, length: Int) {
        accountFor(length)
        output.write(buffer, offset, length)
    }

    override fun flush() = output.flush()

    private fun accountFor(count: Int) {
        bytesWritten += count
        if (bytesWritten > maxBytes) throw BackupTooLargeException()
    }
}

internal suspend inline fun <T> runSuspendCatching(
    crossinline block: suspend () -> T,
): Result<T> = try {
    Result.success(block())
} catch (cancellationException: CancellationException) {
    throw cancellationException
} catch (throwable: Throwable) {
    Result.failure(throwable)
}
