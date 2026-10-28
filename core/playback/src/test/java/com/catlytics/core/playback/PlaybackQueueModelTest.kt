package com.catlytics.core.playback

import com.catlytics.core.model.Artist
import com.catlytics.core.model.PlaybackQueueSource
import com.catlytics.core.model.PlaybackRepeatMode
import com.catlytics.core.model.PlaybackSessionSnapshot
import com.catlytics.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PlaybackQueueModelTest {
    private val model = PlaybackQueueModel(Random(7))
    private val album = (1..6).map { track("track-$it") }

    @Test
    fun `play keeps the given order and starts at the selected track`() {
        val replacement = model.play(album[2], album + album[0], startIndex = 2, PlaybackQueueSource.Static)

        assertEquals(ids(album), ids(replacement.queue))
        assertEquals(2, replacement.startIndex)
        assertEquals(ids(album), ids(model.originalQueue))
    }

    @Test
    fun `play falls back to the track when the queue is empty`() {
        val replacement = model.play(album[0], emptyList(), startIndex = 4, PlaybackQueueSource.Static)

        assertEquals(listOf(album[0]), replacement.queue)
        assertEquals(0, replacement.startIndex)
    }

    @Test
    fun `play with shuffle starts with the track and turning shuffle off restores the album order`() {
        model.setShuffleEnabled(true, currentTrack = null)

        val shuffled = model.play(album[3], album, startIndex = 3, PlaybackQueueSource.Static)
        assertEquals(album[3], shuffled.queue.first())
        assertEquals(0, shuffled.startIndex)
        assertNotEquals(ids(album), ids(shuffled.queue))

        val restored = checkNotNull(model.setShuffleEnabled(false, currentTrack = album[3]))
        assertEquals(ids(album), ids(restored.queue))
        assertEquals(3, restored.startIndex)
    }

    @Test
    fun `setting the same shuffle mode again leaves the queue alone`() {
        model.play(album[0], album, startIndex = 0, PlaybackQueueSource.Static)
        assertTrue(model.setShuffleEnabled(true, album[0]) != null)

        assertNull(model.setShuffleEnabled(true, album[0]))
    }

    @Test
    fun `add to queue goes after earlier manual tracks and play next goes before them`() {
        model.play(album[0], album.take(3), startIndex = 0, PlaybackQueueSource.Playlist("p"))
        val extraA = track("extra-a")
        val extraB = track("extra-b")

        model.insert(listOf(extraA), currentTrackId = album[0].id, playNext = false)
        val updated = model.insert(listOf(extraB), currentTrackId = album[0].id, playNext = true)

        assertEquals(listOf("track-1", "extra-b", "extra-a", "track-2", "track-3"), ids(checkNotNull(updated)))
        assertEquals(PlaybackQueueSource.Static, model.source)
    }

    @Test
    fun `inserting only the current track changes nothing`() {
        model.play(album[0], album, startIndex = 0, PlaybackQueueSource.Static)

        assertNull(model.insert(listOf(album[0]), currentTrackId = album[0].id, playNext = true))
    }

    @Test
    fun `manual tracks stay after the current track when shuffle is turned on`() {
        model.play(album[0], album, startIndex = 0, PlaybackQueueSource.Static)
        val extra = track("extra")
        model.insert(listOf(extra), currentTrackId = album[0].id, playNext = false)

        val shuffled = checkNotNull(model.setShuffleEnabled(true, currentTrack = album[0]))

        assertEquals(listOf("track-1", "extra"), ids(shuffled.queue).take(2))
        assertEquals(0, shuffled.startIndex)
    }

    @Test
    fun `move reorders the queue and drops manual tracks moved before the current one`() {
        model.play(album[0], album, startIndex = 0, PlaybackQueueSource.Static)
        model.insert(listOf(album[4]), currentTrackId = album[0].id, playNext = false)
        // Queue: 1, 5, 2, 3, 4, 6. Moving "5" before the current track.
        assertTrue(model.move(fromIndex = 1, toIndex = 0, currentTrackId = album[0].id))

        assertEquals(listOf("track-5", "track-1", "track-2", "track-3", "track-4", "track-6"), ids(model.queue))
        assertTrue(model.manualQueue.isEmpty())
        assertTrue(!model.move(fromIndex = 0, toIndex = 99, currentTrackId = album[0].id))
    }

    @Test
    fun `remove returns the position of the track`() {
        model.play(album[0], album, startIndex = 0, PlaybackQueueSource.Static)

        assertEquals(2, model.remove(album[2].id))
        assertNull(model.remove("missing"))
        assertEquals(5, model.queue.size)
    }

    @Test
    fun `restore keeps the saved unshuffled order and skips missing tracks`() {
        val shuffledIds = listOf("track-3", "track-1", "missing", "track-2")
        val snapshot = PlaybackSessionSnapshot(
            queueTrackIds = shuffledIds,
            currentTrackId = "track-1",
            queueSource = PlaybackQueueSource.Static,
            currentIndex = 1,
            positionMillis = 0L,
            isShuffleEnabled = true,
            repeatMode = PlaybackRepeatMode.Off,
            originalQueueTrackIds = listOf("track-1", "track-2", "track-3"),
        )

        val replacement = checkNotNull(model.restore(snapshot, album.associateBy(Track::id)))

        assertEquals(listOf("track-3", "track-1", "track-2"), ids(replacement.queue))
        assertEquals(1, replacement.startIndex)
        assertEquals(listOf("track-1", "track-2", "track-3"), ids(model.originalQueue))
        assertTrue(model.isShuffleEnabled)
        assertNull(model.restore(snapshot.copy(queueTrackIds = listOf("missing")), album.associateBy(Track::id)))
    }

    @Test
    fun `an unchanged playlist does not replace the queue`() {
        model.play(album[0], album, startIndex = 0, PlaybackQueueSource.Playlist("p"))

        assertNull(model.reconcileWithSource(album, currentTrackId = album[0].id))
    }

    @Test
    fun `a playlist that lost the current track restarts from its first track`() {
        model.play(album[1], album, startIndex = 1, PlaybackQueueSource.Playlist("p"))
        val edited = album - album[1]

        val replacement = checkNotNull(model.reconcileWithSource(edited, currentTrackId = album[1].id))

        assertEquals(ids(edited), ids(replacement.queue))
        assertEquals(0, replacement.startIndex)
    }

    @Test
    fun `removing unavailable tracks keeps the current one and an empty library clears the queue`() {
        model.play(album[2], album, startIndex = 2, PlaybackQueueSource.Static)

        val kept = checkNotNull(model.removeUnavailable(setOf("track-2", "track-3"), currentTrackId = album[2].id))
        assertEquals(listOf("track-2", "track-3"), ids(kept.queue))
        assertEquals(1, kept.startIndex)

        val cleared = checkNotNull(model.removeUnavailable(emptySet(), currentTrackId = album[2].id))
        assertTrue(cleared.queue.isEmpty())
        assertTrue(model.queue.isEmpty())
    }

    @Test
    fun `refresh metadata returns only the tracks that changed`() {
        model.play(album[0], album, startIndex = 0, PlaybackQueueSource.Static)
        val edited = album[1].copy(title = "Edited")

        val changed = model.refreshMetadata(mapOf(edited.id to edited, album[2].id to album[2]))

        assertEquals(mapOf(edited.id to edited), changed)
        assertEquals("Edited", model.queue[1].title)
        assertEquals("Edited", model.originalQueue[1].title)
    }

    private fun ids(tracks: List<Track>) = tracks.map(Track::id)
}

private fun track(id: String) = Track(
    id = id,
    title = id,
    artist = Artist(id = "artist", name = "Artist"),
    durationMillis = 1_000L,
    mediaUri = "content://media/$id",
    artworkUri = null,
    albumId = null,
    albumTitle = null,
)
