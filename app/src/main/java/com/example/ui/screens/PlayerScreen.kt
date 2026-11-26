package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.example.R
import com.example.ui.MusicUiState
import com.example.ui.components.AuraIconButton
import com.example.ui.components.FloatingMotto
import com.example.ui.components.GlassPanel
import com.example.ui.components.RotatingAlbumDisc
import com.example.ui.formatTrackDuration
import com.example.ui.theme.CoolSilverText
import com.example.ui.theme.CrispIceWhite
import com.example.ui.theme.CyanAura
import com.example.ui.theme.DeepAuraBlue
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ErrorCrimson
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.SoftVioletGlow
import com.example.ui.theme.SubtleViolet
import kotlin.math.roundToLong

@Composable
fun PlayerScreen(
    uiState: MusicUiState,
    showLibraryButton: Boolean,
    onOpenLibrary: () -> Unit,
    onRequestPermission: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    var lastNonZeroVolume by remember { mutableFloatStateOf(0.7f) }
    if (uiState.volumeLevel > 0.02f) {
        lastNonZeroVolume = uiState.volumeLevel
    }

    val safeDuration = uiState.durationMs.coerceAtLeast(0L)
    val playbackFraction = if (safeDuration > 0L) {
        (uiState.currentPositionMs.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val sliderValue = if (isDraggingSlider) dragFraction else playbackFraction
    val displayedPositionMs = if (isDraggingSlider && safeDuration > 0L) {
        (dragFraction * safeDuration).roundToLong()
    } else {
        uiState.currentPositionMs
    }

    val repeatActive = uiState.repeatMode != Player.REPEAT_MODE_OFF

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val discSize = when {
            maxHeight < 640.dp -> 168.dp
            maxHeight < 760.dp -> 212.dp
            else -> 244.dp
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .widthIn(max = 560.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar: App name + Library action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = CrispIceWhite,
                            shadow = Shadow(
                                color = ElectricBlue.copy(alpha = 0.55f),
                                offset = Offset(0f, 0f),
                                blurRadius = 12f,
                            ),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (showLibraryButton) {
                    AuraIconButton(
                        onClick = onOpenLibrary,
                        size = 48.dp,
                        active = false,
                        modifier = Modifier.testTag("open_library_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = stringResource(R.string.cd_open_library),
                            tint = CyanAura,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Floating DBZ-inspired motto: "Não seja um beta."
            FloatingMotto()

            Spacer(modifier = Modifier.weight(0.55f, fill = true))
            Spacer(modifier = Modifier.height(16.dp))

            // Rotating Album Artwork Disc
            RotatingAlbumDisc(
                albumArtUri = uiState.currentTrack?.albumArtUri,
                isPlaying = uiState.isPlaying,
                modifier = Modifier.size(discSize),
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Track Title & Artist
            AnimatedContent(
                targetState = uiState.currentTrack,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "track_metadata_transition",
            ) { track ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val titleText = track?.title
                        ?: if (uiState.tracks.isNotEmpty()) {
                            stringResource(R.string.no_track_selected)
                        } else {
                            stringResource(R.string.app_name)
                        }
                    val artistText = when {
                        track != null -> track.artist.ifBlank { stringResource(R.string.unknown_artist) }
                        !uiState.hasAudioPermission -> stringResource(R.string.permission_title)
                        uiState.tracks.isNotEmpty() -> stringResource(R.string.select_track_prompt)
                        else -> stringResource(R.string.empty_library_title)
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = CrispIceWhite,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(iterations = Int.MAX_VALUE),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = artistText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = CoolSilverText,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Optional inline error or permission banner
            AnimatedVisibility(visible = !uiState.hasAudioPermission) {
                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    shape = RoundedCornerShape(18.dp),
                    highlighted = true,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FolderOpen,
                                contentDescription = null,
                                tint = CyanAura,
                            )
                            Text(
                                text = stringResource(R.string.permission_title),
                                style = MaterialTheme.typography.bodyMedium,
                                color = CrispIceWhite,
                            )
                        }
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = ObsidianBlack,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("player_grant_permission_button"),
                        ) {
                            Text(
                                text = stringResource(R.string.permission_grant_button).substringBefore(" às"),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = uiState.playbackErrorMessage != null) {
                Text(
                    text = uiState.playbackErrorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorCrimson,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(modifier = Modifier.weight(0.65f, fill = true))
            Spacer(modifier = Modifier.height(16.dp))

            // Glassmorphic Control Deck
            GlassPanel(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                elevated = true,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                ) {
                    // Seekbar
                    Slider(
                        value = sliderValue,
                        onValueChange = { fraction ->
                            isDraggingSlider = true
                            dragFraction = fraction
                        },
                        onValueChangeFinished = {
                            if (safeDuration > 0L) {
                                onSeekTo((dragFraction * safeDuration).roundToLong())
                            }
                            isDraggingSlider = false
                        },
                        enabled = safeDuration > 0L,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanAura,
                            activeTrackColor = ElectricBlue,
                            inactiveTrackColor = Color.White.copy(alpha = 0.14f),
                            disabledThumbColor = MutedSlateText,
                            disabledActiveTrackColor = MutedSlateText.copy(alpha = 0.35f),
                            disabledInactiveTrackColor = Color.White.copy(alpha = 0.08f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("progress_slider"),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = formatTrackDuration(displayedPositionMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanAura,
                        )
                        Text(
                            text = formatTrackDuration(safeDuration),
                            style = MaterialTheme.typography.labelMedium,
                            color = CoolSilverText,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Main Transport Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        // Shuffle
                        val shuffleDesc = stringResource(
                            if (uiState.shuffleEnabled) R.string.shuffle_on else R.string.shuffle_off
                        )
                        AuraIconButton(
                            onClick = onToggleShuffle,
                            size = 48.dp,
                            active = uiState.shuffleEnabled,
                            modifier = Modifier.testTag("shuffle_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = shuffleDesc,
                                tint = if (uiState.shuffleEnabled) CyanAura else CoolSilverText,
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        // Previous
                        AuraIconButton(
                            onClick = onPrevious,
                            size = 52.dp,
                            modifier = Modifier.testTag("previous_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = stringResource(R.string.cd_previous_track),
                                tint = CrispIceWhite,
                                modifier = Modifier.size(30.dp),
                            )
                        }

                        // Hero Play / Pause Button
                        HeroPlayPauseButton(
                            isPlaying = uiState.isPlaying,
                            onClick = onTogglePlayPause,
                            modifier = Modifier.testTag("play_pause_button"),
                        )

                        // Next
                        AuraIconButton(
                            onClick = onNext,
                            size = 52.dp,
                            modifier = Modifier.testTag("next_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = stringResource(R.string.cd_next_track),
                                tint = CrispIceWhite,
                                modifier = Modifier.size(30.dp),
                            )
                        }

                        // Repeat
                        val repeatDesc = when (uiState.repeatMode) {
                            Player.REPEAT_MODE_ONE -> stringResource(R.string.repeat_one)
                            Player.REPEAT_MODE_ALL -> stringResource(R.string.repeat_all)
                            else -> stringResource(R.string.repeat_off)
                        }
                        AuraIconButton(
                            onClick = onCycleRepeat,
                            size = 48.dp,
                            active = repeatActive,
                            modifier = Modifier.testTag("repeat_button"),
                        ) {
                            Icon(
                                imageVector = if (uiState.repeatMode == Player.REPEAT_MODE_ONE) {
                                    Icons.Rounded.RepeatOne
                                } else {
                                    Icons.Rounded.Repeat
                                },
                                contentDescription = repeatDesc,
                                tint = when (uiState.repeatMode) {
                                    Player.REPEAT_MODE_ONE -> SoftVioletGlow
                                    Player.REPEAT_MODE_ALL -> CyanAura
                                    else -> CoolSilverText
                                },
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Volume Slider Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val volumeIcon = when {
                            uiState.volumeLevel <= 0.01f -> Icons.AutoMirrored.Rounded.VolumeOff
                            uiState.volumeLevel < 0.45f -> Icons.AutoMirrored.Rounded.VolumeDown
                            else -> Icons.AutoMirrored.Rounded.VolumeUp
                        }
                        AuraIconButton(
                            onClick = {
                                if (uiState.volumeLevel > 0.01f) {
                                    onVolumeChange(0f)
                                } else {
                                    onVolumeChange(lastNonZeroVolume)
                                }
                            },
                            size = 40.dp,
                            containerColor = Color.Transparent,
                            modifier = Modifier.testTag("volume_mute_button"),
                        ) {
                            Icon(
                                imageVector = volumeIcon,
                                contentDescription = stringResource(R.string.cd_volume),
                                tint = CoolSilverText,
                                modifier = Modifier.size(20.dp),
                            )
                        }

                        Slider(
                            value = uiState.volumeLevel,
                            onValueChange = onVolumeChange,
                            colors = SliderDefaults.colors(
                                thumbColor = SoftVioletGlow,
                                activeTrackColor = SubtleViolet,
                                inactiveTrackColor = Color.White.copy(alpha = 0.12f),
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("volume_slider"),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HeroPlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "hero_play_scale",
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .scale(scale)
            .size(72.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        CyanAura,
                        ElectricBlue,
                        DeepAuraBlue,
                        SubtleViolet,
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.7f), SoftVioletGlow.copy(alpha = 0.6f))
                ),
                shape = CircleShape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = Color.White),
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "play_pause_icon_anim",
        ) { playing ->
            Icon(
                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(
                    if (playing) R.string.cd_pause else R.string.cd_play
                ),
                tint = ObsidianBlack,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}
