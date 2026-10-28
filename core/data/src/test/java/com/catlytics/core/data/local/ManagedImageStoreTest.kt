package com.catlytics.core.data.local

import android.content.Context
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ManagedImageStoreTest {
    private lateinit var context: Context
    private lateinit var store: ManagedImageStore
    private lateinit var directory: File
    private lateinit var outside: File

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        store = ManagedImageStore(context, "playlist_covers")
        directory = context.filesDir.resolve("playlist_covers")
        outside = context.filesDir.resolve("catlytics_secret.txt").apply { writeText("secret") }
    }

    @Test
    fun `an owner id with path separators cannot write outside the store`() {
        val path = store.write(byteArrayOf(1, 2, 3), ownerId = "../../evil")

        assertNotNull(path)
        val file = File(path!!)
        assertEquals(directory.canonicalFile, file.canonicalFile.parentFile)
        assertFalse(file.name.contains('/'))
    }

    @Test
    fun `files outside the store cannot be read`() {
        listOf(
            outside.absolutePath,
            "${directory.absolutePath}/../${outside.name}",
            "file://${directory.absolutePath}/../${outside.name}",
            "content://com.catlytics.provider/${outside.name}",
            "../${outside.name}",
        ).forEach { path ->
            assertNull(path, store.read(path, maxBytes = 1_024L))
        }
    }

    @Test
    fun `files in a subdirectory of the store are not managed`() {
        val nested = directory.resolve("nested").apply { mkdirs() }
            .resolve("cover.cover").apply { writeBytes(byteArrayOf(1)) }

        assertNull(store.read(nested.absolutePath, maxBytes = 1_024L))
        store.deleteManaged(nested.absolutePath)
        assertTrue(nested.exists())
    }

    @Test
    fun `own files are read as path or file uri within the size limit`() {
        val bytes = byteArrayOf(4, 5, 6)
        val path = store.write(bytes, ownerId = "focus")!!

        assertArrayEquals(bytes, store.read(path, maxBytes = 3L))
        assertArrayEquals(bytes, store.read("file://$path", maxBytes = 3L))
        assertNull(store.read(path, maxBytes = 2L))
    }

    @Test
    fun `deleting through a traversal path leaves outside files alone`() {
        store.deleteManaged("${directory.absolutePath}/../${outside.name}")
        store.delete(ownerId = "catlytics_secret", path = outside.absolutePath)

        assertTrue(outside.exists())
    }

    @Test
    fun `delete only removes files created for that owner`() {
        val focus = store.write(byteArrayOf(1), ownerId = "focus")!!
        val gym = store.write(byteArrayOf(2), ownerId = "gym")!!

        store.delete(ownerId = "focus", path = gym)
        assertTrue(File(gym).exists())

        store.delete(ownerId = "focus", path = focus)
        assertFalse(File(focus).exists())
    }

    @Test
    fun `cleanup keeps the listed files and never touches files outside the store`() {
        val kept = store.write(byteArrayOf(1), ownerId = "focus")!!
        val stale = store.write(byteArrayOf(2), ownerId = "gym")!!

        store.deleteAllExcept(setOf(kept, outside.absolutePath))

        assertTrue(File(kept).exists())
        assertFalse(File(stale).exists())
        assertTrue(outside.exists())
    }
}
