package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AudioTrack
import com.example.data.SortOption
import com.example.ui.MusicUiState
import com.example.ui.components.AuraIconButton
import com.example.ui.components.EqualizerIndicator
import com.example.ui.components.GlassPanel
import com.example.ui.components.TrackArtwork
import com.example.ui.formatTrackDuration
import com.example.ui.theme.CoolSilverText
import com.example.ui.theme.CrispIceWhite
import com.example.ui.theme.CyanAura
import com.example.ui.theme.DeepCosmicNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GlassBorderBlue
import com.example.ui.theme.GlassSurfaceDark
import com.example.ui.theme.GlassSurfaceElevated
import com.example.ui.theme.MutedSlateText
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.SoftVioletGlow
import com.example.ui.theme.SubtleViolet

@Composable
fun LibraryScreen(
    uiState: MusicUiState,
    showBackButton: Boolean,
    showMiniPlayer: Boolean,
    onBack: () -> Unit,
    onTrackSelected: (AudioTrack) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSortOptionChange: (SortOption) -> Unit,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onMiniPlayerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .widthIn(max = 640.dp)
                .fillMaxWidth(),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (showBackButton) {
                        AuraIconButton(
                            onClick = onBack,
                            size = 48.dp,
                            modifier = Modifier.testTag("library_back_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back_to_player),
                                tint = CrispIceWhite,
                            )
                        }
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.library_title),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontFamily = OrbitronFontFamily,
                                color = CrispIceWhite,
                            ),
                        )
                        if (uiState.hasAudioPermission) {
                            val count = uiState.filteredTracks.size
                            val countText = if (count == 1) {
                                stringResource(R.string.library_tracks_count_singular, count)
                            } else {
                                stringResource(R.string.library_tracks_count_plural, count)
                            }
                            Text(
                                text = countText,
                                style = MaterialTheme.typography.labelMedium,
                                color = CyanAura,
                            )
                        }
                    }
                }

                if (uiState.hasAudioPermission) {
                    AuraIconButton(
                        onClick = onRefresh,
                        size = 48.dp,
                        modifier = Modifier.testTag("refresh_library_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.cd_refresh_library),
                            tint = CyanAura,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search bar & Sort chips
            if (uiState.hasAudioPermission) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.library_search_hint),
                            color = MutedSlateText,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = CyanAura,
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChange("") },
                                modifier = Modifier.testTag("clear_search_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = stringResource(R.string.cd_clear_search),
                                    tint = CoolSilverText,
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CrispIceWhite,
                        unfocusedTextColor = CrispIceWhite,
                        focusedContainerColor = GlassSurfaceElevated,
                        unfocusedContainerColor = GlassSurfaceDark,
                        focusedBorderColor = CyanAura,
                        unfocusedBorderColor = GlassBorderBlue.copy(alpha = 0.5f),
                        cursorColor = CyanAura,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_input"),
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SortPill(
                        label = stringResource(R.string.sort_title),
                        selected = uiState.sortOption == SortOption.TITLE,
                        onClick = { onSortOptionChange(SortOption.TITLE) },
                    )
                    SortPill(
                        label = stringResource(R.string.sort_artist),
                        selected = uiState.sortOption == SortOption.ARTIST,
                        onClick = { onSortOptionChange(SortOption.ARTIST) },
                    )
                    SortPill(
                        label = stringResource(R.string.sort_recent),
                        selected = uiState.sortOption == SortOption.RECENT,
                        onClick = { onSortOptionChange(SortOption.RECENT) },
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Main Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    !uiState.hasAudioPermission -> {
                        PermissionRequiredCard(
                            hasAskedBefore = uiState.hasAskedPermission,
                            onRequestPermission = onRequestPermission,
                            onOpenSettings = onOpenSettings,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    uiState.isLoadingLibrary -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            CircularProgressIndicator(color = CyanAura)
                            Text(
                                text = stringResource(R.string.loading_library),
                                style = MaterialTheme.typography.bodyLarge,
                                color = CoolSilverText,
                            )
                        }
                    }

                    uiState.tracks.isEmpty() -> {
                        EmptyLibraryState(
                            onRefresh = onRefresh,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    uiState.filteredTracks.isEmpty() -> {
                        EmptySearchState(
                            onClear = { onSearchQueryChange("") },
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("library_tracks_list"),
                            contentPadding = PaddingValues(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(
                                items = uiState.filteredTracks,
                                key = { it.id },
                            ) { track ->
                                val isCurrent = uiState.currentTrack?.id == track.id
                                TrackListItem(
                                    track = track,
                                    isCurrent = isCurrent,
                                    isPlaying = isCurrent && uiState.isPlaying,
                                    onClick = { onTrackSelected(track) },
                                )
                            }
                        }
                    }
                }
            }
        }

        // Persistent Mini-Player bar on Compact/Medium screens
        AnimatedVisibility(
            visible = showMiniPlayer && uiState.currentTrack != null,
        ) {
            uiState.currentTrack?.let { current ->
                MiniPlayerBar(
                    track = current,
                    isPlaying = uiState.isPlaying,
                    positionMs = uiState.currentPositionMs,
                    durationMs = uiState.durationMs,
                    onClick = onMiniPlayerClick,
                    onTogglePlayPause = onTogglePlayPause,
                    onNext = onNext,
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SortPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val bgColor = if (selected) ElectricBlue.copy(alpha = 0.20f) else GlassSurfaceDark
    val borderColor = if (selected) CyanAura else Color.White.copy(alpha = 0.14f)

    Box(
        modifier = Modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) CyanAura else CoolSilverText,
        )
    }
}

@Composable
private fun TrackListItem(
    track: AudioTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "track_item_scale",
    )

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = CyanAura),
                role = Role.Button,
                onClick = onClick,
            )
            .testTag("track_item_${track.id}"),
        shape = RoundedCornerShape(18.dp),
        elevated = isCurrent,
        highlighted = isCurrent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TrackArtwork(
                albumArtUri = track.albumArtUri,
                contentDescription = track.title,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(54.dp)
                    .border(
                        width = if (isCurrent) 1.5.dp else 1.dp,
                        color = if (isCurrent) CyanAura else Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                    ),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isCurrent) CyanAura else CrispIceWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = track.artist.ifBlank { stringResource(R.string.unknown_artist) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoolSilverText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSlateText,
                    )
                    Text(
                        text = formatTrackDuration(track.durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrent) SoftVioletGlow else MutedSlateText,
                    )
                }
            }

            if (isCurrent) {
                EqualizerIndicator(
                    isPlaying = isPlaying,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun MiniPlayerBar(
    track: AudioTrack,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    GlassPanel(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("mini_player_bar"),
        shape = RoundedCornerShape(20.dp),
        elevated = true,
        highlighted = true,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TrackArtwork(
                    albumArtUri = track.albumArtUri,
                    contentDescription = track.title,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(44.dp),
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = CrispIceWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist.ifBlank { stringResource(R.string.unknown_artist) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoolSilverText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                AuraIconButton(
                    onClick = onTogglePlayPause,
                    size = 44.dp,
                    active = true,
                    modifier = Modifier.testTag("mini_play_pause_button"),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(
                            if (isPlaying) R.string.cd_pause else R.string.cd_play
                        ),
                        tint = CyanAura,
                    )
                }

                AuraIconButton(
                    onClick = onNext,
                    size = 44.dp,
                    modifier = Modifier.testTag("mini_next_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = stringResource(R.string.cd_next_track),
                        tint = CrispIceWhite,
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                color = CyanAura,
                trackColor = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
            )
        }
    }
}

@Composable
private fun PermissionRequiredCard(
    hasAskedBefore: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevated = true,
        highlighted = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(ElectricBlue.copy(alpha = 0.25f), SubtleViolet.copy(alpha = 0.25f)))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.LibraryMusic,
                    contentDescription = null,
                    tint = CyanAura,
                    modifier = Modifier.size(32.dp),
                )
            }

            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.titleLarge,
                color = CrispIceWhite,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(R.string.permission_description),
                style = MaterialTheme.typography.bodyLarge,
                color = CoolSilverText,
                textAlign = TextAlign.Center,
            )

            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue,
                    contentColor = ObsidianBlack,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("grant_permission_button"),
            ) {
                Text(
                    text = stringResource(R.string.permission_grant_button),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            if (hasAskedBefore) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAura),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_settings_button"),
                ) {
                    Text(
                        text = stringResource(R.string.permission_settings_button),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        elevated = true,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TrackArtwork(
                albumArtUri = null,
                contentDescription = null,
                shape = CircleShape,
                modifier = Modifier
                    .size(84.dp)
                    .border(1.5.dp, CyanAura.copy(alpha = 0.7f), CircleShape),
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = CyanAura,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.empty_library_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = CrispIceWhite,
                    textAlign = TextAlign.Center,
                )
            }

            Text(
                text = stringResource(R.string.empty_library_description),
                style = MaterialTheme.typography.bodyLarge,
                color = CoolSilverText,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue,
                    contentColor = ObsidianBlack,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.testTag("empty_refresh_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.empty_library_refresh),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassPanel(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.SearchOff,
                contentDescription = null,
                tint = SoftVioletGlow,
                modifier = Modifier.size(44.dp),
            )
            Text(
                text = stringResource(R.string.search_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = CrispIceWhite,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.search_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = CoolSilverText,
                textAlign = TextAlign.Center,
            )
            OutlinedButton(
                onClick = onClear,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAura),
                modifier = Modifier.testTag("empty_search_clear_button"),
            ) {
                Text(text = stringResource(R.string.search_empty_clear))
            }
        }
    }
}
