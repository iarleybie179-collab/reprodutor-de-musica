package com.example

import android.Manifest
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.test.core.app.ApplicationProvider
import com.example.data.AudioTrack
import com.example.player.MusicPlaybackService
import com.example.player.SafeBitmapLoader
import com.example.ui.MusicUiState
import com.example.ui.MusicViewModel
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.theme.ReprodutorDeMusicaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlaybackFlowRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleTracks = listOf(
        AudioTrack(
            id = 101L,
            title = "Cha-La Head-Cha-La",
            artist = "Hironobu Kageyama",
            album = "DBZ",
            durationMs = 198_000L,
            contentUri = Uri.parse("content://media/external/audio/media/101"),
            albumArtUri = null,
            dateAdded = 1000L,
            mimeType = "audio/mpeg",
        ),
        AudioTrack(
            id = 102L,
            title = "Faixa Sem Metadados",
            artist = "",
            album = "",
            durationMs = 120_000L,
            contentUri = Uri.parse("content://media/external/audio/media/102"),
            albumArtUri = Uri.parse("content://media/external/audio/albumart/99999"),
            dateAdded = 2000L,
            mimeType = "audio/flac",
        ),
    )

    private fun bindRealPlaybackServiceForTest(app: Application): MusicPlaybackService {
        val serviceController = Robolectric.buildService(MusicPlaybackService::class.java).create()
        val service = serviceController.get()
        val binder = service.onBind(Intent("androidx.media3.session.MediaSessionService"))
        Shadows.shadowOf(app).setComponentNameAndServiceForBindService(
            ComponentName(app, MusicPlaybackService::class.java),
            binder,
        )
        return service
    }

    @Test
    fun `clicking a track in LibraryScreen triggers selection and renders mini player controls`() {
        var selectedTrack: AudioTrack? = null
        var miniToggleClicked = false
        var miniNextClicked = false
        var miniBarClicked = false
        var uiState by mutableStateOf(
            MusicUiState(
                tracks = sampleTracks,
                filteredTracks = sampleTracks,
                hasAudioPermission = true,
                hasAskedPermission = true,
            )
        )

        composeTestRule.setContent {
            ReprodutorDeMusicaTheme {
                LibraryScreen(
                    uiState = uiState,
                    showBackButton = true,
                    showMiniPlayer = true,
                    onBack = {},
                    onTrackSelected = { track ->
                        selectedTrack = track
                        uiState = uiState.copy(
                            currentTrack = track,
                            isPlaying = true,
                            durationMs = track.durationMs,
                        )
                    },
                    onSearchQueryChange = {},
                    onSortOptionChange = {},
                    onRefresh = {},
                    onRequestPermission = {},
                    onOpenSettings = {},
                    onTogglePlayPause = { miniToggleClicked = true },
                    onNext = { miniNextClicked = true },
                    onMiniPlayerClick = { miniBarClicked = true },
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("track_item_101").performClick()
        composeTestRule.waitForIdle()

        assertEquals(101L, selectedTrack?.id)
        composeTestRule.onNodeWithTag("mini_player_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("mini_play_pause_button").performClick()
        composeTestRule.onNodeWithTag("mini_next_button").performClick()
        composeTestRule.onNodeWithTag("mini_player_bar").performClick()
        composeTestRule.waitForIdle()

        assertTrue(miniToggleClicked)
        assertTrue(miniNextClicked)
        assertTrue(miniBarClicked)
    }

    @Test
    fun `clicking track with missing metadata and zero duration does not crash`() {
        var uiState by mutableStateOf(
            MusicUiState(
                tracks = sampleTracks,
                filteredTracks = sampleTracks,
                hasAudioPermission = true,
                hasAskedPermission = true,
            )
        )

        composeTestRule.setContent {
            ReprodutorDeMusicaTheme {
                LibraryScreen(
                    uiState = uiState,
                    showBackButton = true,
                    showMiniPlayer = true,
                    onBack = {},
                    onTrackSelected = { track ->
                        uiState = uiState.copy(
                            currentTrack = track,
                            isPlaying = true,
                            currentPositionMs = 5000L,
                            durationMs = 0L,
                        )
                    },
                    onSearchQueryChange = {},
                    onSortOptionChange = {},
                    onRefresh = {},
                    onRequestPermission = {},
                    onOpenSettings = {},
                    onTogglePlayPause = {},
                    onNext = {},
                    onMiniPlayerClick = {},
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("track_item_102").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("mini_player_bar").assertIsDisplayed()
    }

    @Test
    fun `PlayerScreen controls work with incomplete metadata`() {
        var playPauseClicked = false
        var nextClicked = false
        var prevClicked = false

        val uiState = MusicUiState(
            tracks = sampleTracks,
            filteredTracks = sampleTracks,
            currentTrack = sampleTracks[1],
            isPlaying = false,
            currentPositionMs = 15_000L,
            durationMs = 120_000L,
            shuffleEnabled = false,
            repeatMode = Player.REPEAT_MODE_OFF,
            hasAudioPermission = true,
        )

        composeTestRule.setContent {
            ReprodutorDeMusicaTheme {
                PlayerScreen(
                    uiState = uiState,
                    showLibraryButton = true,
                    onOpenLibrary = {},
                    onRequestPermission = {},
                    onTogglePlayPause = { playPauseClicked = true },
                    onPrevious = { prevClicked = true },
                    onNext = { nextClicked = true },
                    onSeekTo = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onVolumeChange = {},
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("play_pause_button").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("next_button").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("previous_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertTrue(playPauseClicked)
        assertTrue(nextClicked)
        assertTrue(prevClicked)
    }

    @Test
    fun `MusicViewModel handles playTrack, next, previous, togglePlayPause with large queue`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        val service = bindRealPlaybackServiceForTest(app)

        val viewModel = MusicViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val largeQueue = List(120) { index ->
            AudioTrack(
                id = (index + 1).toLong(),
                title = "Track $index",
                artist = if (index % 2 == 0) "Artist $index" else "",
                album = "",
                durationMs = 180_000L,
                contentUri = Uri.parse("content://media/external/audio/media/${index + 1}"),
                albumArtUri = null,
                dateAdded = index.toLong(),
                mimeType = "audio/mpeg",
            )
        }

        viewModel.playTrack(largeQueue[60], largeQueue)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val state = viewModel.uiState.value
        assertEquals(61L, state.currentTrack?.id)
        assertEquals(180_000L, state.durationMs)

        viewModel.skipToNext()
        viewModel.skipToPrevious()
        viewModel.togglePlayPause()
        viewModel.toggleShuffle()
        viewModel.cycleRepeatMode()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        service.onDestroy()
    }

    @Test
    fun `MusicViewModel blocks playback gracefully when permission is missing`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        val service = bindRealPlaybackServiceForTest(app)

        val viewModel = MusicViewModel(app)
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        viewModel.playTrack(sampleTracks.first(), sampleTracks)
        assertFalse(viewModel.uiState.value.hasAudioPermission)

        service.onDestroy()
    }

    @androidx.annotation.OptIn(UnstableApi::class)
    @Test
    fun `SafeBitmapLoader downsamples default DBZ cover within safe memory bounds`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val loader = SafeBitmapLoader(app, maxDimensionPx = 256)
        val invalidAlbumUri = Uri.parse("content://media/external/audio/albumart/999999")
        val bitmap = loader.loadBitmap(invalidAlbumUri).get()
        assertNotNull(bitmap)
        assertTrue(bitmap.width <= 512)
        assertTrue(bitmap.height <= 512)
    }

    @Test
    fun `MusicPlaybackService creates session cleanly`() {
        val controller = Robolectric.buildService(MusicPlaybackService::class.java)
            .create()
        val service = controller.get()
        assertNotNull(service)
        controller.destroy()
    }
}
