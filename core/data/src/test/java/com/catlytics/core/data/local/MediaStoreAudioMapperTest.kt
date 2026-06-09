package com.catlytics.core.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaStoreAudioMapperTest {
    @Test
    fun `toTrackEntity maps music row`() {
        val folder = MediaStoreAudioMapper.folderFromRelativePath(
            volumeName = "external_primary",
            relativePath = "Music/Favorites/",
        )
        val track = MediaStoreAudioMapper.toTrackEntity(
            id = 42L,
            title = "Local Song",
            artist = "Local Artist",
            artistId = 7L,
            albumId = 9L,
            durationMillis = 180_000L,
            isMusic = 1,
            mediaUri = "content://media/external/audio/media/42",
            folder = folder,
        )

        requireNotNull(track)
        assertEquals("mediastore-42", track.id)
        assertEquals("Local Song", track.title)
        assertEquals("mediastore-artist-7", track.artistId)
        assertEquals("Local Artist", track.artistName)
        assertEquals(180_000L, track.durationMillis)
        assertEquals("content://media/external/audio/media/42", track.mediaUri)
        assertEquals("content://media/external/audio/albumart/9", track.artworkUri)
        assertEquals("external_primary:Music/Favorites", track.folderId)
        assertEquals("Favorites", track.folderName)
        assertEquals("Music/Favorites", track.folderPath)
    }

    @Test
    fun `relative folders with the same name keep distinct ids`() {
        val musicFolder = MediaStoreAudioMapper.folderFromRelativePath(
            volumeName = "external_primary",
            relativePath = "Music/Game/",
        )
        val downloadsFolder = MediaStoreAudioMapper.folderFromRelativePath(
            volumeName = "external_primary",
            relativePath = "Download/Game/",
        )

        requireNotNull(musicFolder)
        requireNotNull(downloadsFolder)
        assertEquals("Game", musicFolder.name)
        assertEquals("Game", downloadsFolder.name)
        assert(musicFolder.id != downloadsFolder.id)
    }

    @Test
    fun `absolute path maps its parent folder`() {
        val folder = MediaStoreAudioMapper.folderFromAbsolutePath(
            absolutePath = "/storage/emulated/0/Music/Favorites/song.mp3",
        )

        requireNotNull(folder)
        assertEquals("external:storage/emulated/0/Music/Favorites", folder.id)
        assertEquals("Favorites", folder.name)
        assertEquals("storage/emulated/0/Music/Favorites", folder.path)
    }

    @Test
    fun `toTrackEntity uses unknown artist fallback`() {
        val track = MediaStoreAudioMapper.toTrackEntity(
            id = 42L,
            title = "Local Song",
            artist = "<unknown>",
            artistId = 7L,
            albumId = 9L,
            durationMillis = 180_000L,
            isMusic = 1,
            mediaUri = "content://media/external/audio/media/42",
        )

        requireNotNull(track)
        assertEquals("Artista desconocido", track.artistName)
    }

    @Test
    fun `toTrackEntity preserves media uri when metadata is normalized`() {
        val track = MediaStoreAudioMapper.toTrackEntity(
            id = 42L,
            title = "",
            artist = "<unknown>",
            artistId = 7L,
            albumId = 0L,
            durationMillis = 180_000L,
            isMusic = 1,
            mediaUri = "content://media/external/audio/media/42",
        )

        requireNotNull(track)
        assertEquals("Cancion sin titulo", track.title)
        assertEquals("Artista desconocido", track.artistName)
        assertEquals("content://media/external/audio/media/42", track.mediaUri)
        assertNull(track.artworkUri)
    }

    @Test
    fun `toTrackEntity ignores invalid duration`() {
        val track = MediaStoreAudioMapper.toTrackEntity(
            id = 42L,
            title = "Local Song",
            artist = "Local Artist",
            artistId = 7L,
            albumId = 9L,
            durationMillis = 0L,
            isMusic = 1,
            mediaUri = "content://media/external/audio/media/42",
        )

        assertNull(track)
    }

    @Test
    fun `toTrackEntity ignores non music rows`() {
        val track = MediaStoreAudioMapper.toTrackEntity(
            id = 42L,
            title = "Local Song",
            artist = "Local Artist",
            artistId = 7L,
            albumId = 9L,
            durationMillis = 180_000L,
            isMusic = 0,
            mediaUri = "content://media/external/audio/media/42",
        )

        assertNull(track)
    }
}
