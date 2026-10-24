package com.catlytics.core.data.repository

import android.content.Context
import androidx.core.net.toUri
import com.catlytics.core.model.BackupException
import java.io.InputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

/**
 * Todo lo que define el archivo de respaldo JSON: formato, versión de esquema, límite de tamaño,
 * configuración de [Json] y lectura/escritura. Al cambiar el esquema solo se toca este archivo
 * (y [BackupDocument], con los campos nuevos con valor por defecto).
 */
internal object BackupCodec {
    const val UNIFIED_FORMAT = "catlytics.backup"

    /** Formatos de versiones anteriores, que exportaban solo estadísticas o solo playlists. */
    const val STATISTICS_FORMAT = "catlytics.statistics.backup"
    const val PLAYLISTS_FORMAT = "catlytics.playlists.backup"

    const val SUPPORTED_SCHEMA_VERSION = 4
    const val MIN_SUPPORTED_SCHEMA_VERSION = 1
    const val ALIAS_SCHEMA_VERSION = 2
    const val METADATA_SCHEMA_VERSION = 4
    const val MAX_BACKUP_BYTES = 64L * 1024L * 1024L

    private val json = Json {
        ignoreUnknownKeys = true
    }

    /** Lee y valida el documento; falla con [BackupException] si no es un respaldo válido. */
    fun read(context: Context, uri: String): BackupDocument {
        val document = context.contentResolver.openInputStream(uri.toUri())?.use { input ->
            decode(SizeLimitedInputStream(input, MAX_BACKUP_BYTES))
        } ?: error("No se pudo abrir el archivo de respaldo.")
        BackupDocumentValidator.validate(document)
        return document
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun write(context: Context, uri: String, document: BackupDocument) {
        context.contentResolver.openOutputStream(uri.toUri())?.use { output ->
            json.encodeToStream(document, SizeLimitedOutputStream(output, MAX_BACKUP_BYTES))
            output.flush()
        } ?: error("No se pudo abrir el archivo de destino para exportar el respaldo.")
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun decode(input: InputStream): BackupDocument = try {
        json.decodeFromStream<BackupDocument>(input)
    } catch (error: BackupException) {
        throw error
    } catch (error: Exception) {
        throw BackupException.InvalidFormat("Not a Catlytics backup document.", error)
    }
}
