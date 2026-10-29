package com.catlytics.core.model

sealed class BackupException(message: String, cause: Throwable? = null) :
    IllegalArgumentException(message, cause) {

    class TooLarge(maxBytes: Long) : BackupException("Backup exceeds $maxBytes bytes.")

    class InvalidFormat(detail: String, cause: Throwable? = null) : BackupException(detail, cause)

    class UnsupportedVersion(val schemaVersion: Int) :
        BackupException("Unsupported backup schema version $schemaVersion.")

    class InvalidContent(detail: String) : BackupException(detail)
}
