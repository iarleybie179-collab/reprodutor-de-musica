package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.MusicViewModel
import com.example.ui.components.EnergyBackground
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlayerScreen
import com.example.ui.theme.GlassBorderBlue
import com.example.ui.theme.ReprodutorDeMusicaTheme
import kotlinx.serialization.Serializable

@Serializable
object PlayerRoute

@Serializable
object LibraryRoute

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

            ReprodutorDeMusicaTheme {
                MusicPlayerApp(isExpandedScreen = isExpanded)
            }
        }
    }
}

@Composable
private fun MusicPlayerApp(
    isExpandedScreen: Boolean,
    viewModel: MusicViewModel = viewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var initialPromptLaunched by rememberSaveable { mutableStateOf(false) }

    val requiredPermissions = rememberRequiredPermissions()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        viewModel.onPermissionStatusChanged(
            granted = hasAudioReadPermission(context),
            asked = true,
        )
    }

    LaunchedEffect(Unit) {
        val granted = hasAudioReadPermission(context)
        viewModel.onPermissionStatusChanged(granted = granted, asked = false)
        if (!granted && !initialPromptLaunched) {
            initialPromptLaunched = true
            permissionLauncher.launch(requiredPermissions)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onPermissionStatusChanged(
                    granted = hasAudioReadPermission(context),
                    asked = false,
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val openSettings = {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    val requestAudioPermission = {
        permissionLauncher.launch(requiredPermissions)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        EnergyBackground(
            dimIntensity = if (isExpandedScreen) 0.58f else 0.50f,
        )

        if (isExpandedScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                PlayerScreen(
                    uiState = uiState,
                    showLibraryButton = false,
                    onOpenLibrary = {},
                    onRequestPermission = requestAudioPermission,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onPrevious = viewModel::skipToPrevious,
                    onNext = viewModel::skipToNext,
                    onSeekTo = viewModel::seekTo,
                    onToggleShuffle = viewModel::toggleShuffle,
                    onCycleRepeat = viewModel::cycleRepeatMode,
                    onVolumeChange = viewModel::setVolume,
                    modifier = Modifier.weight(1f),
                )

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(GlassBorderBlue.copy(alpha = 0.45f))
                )

                LibraryScreen(
                    uiState = uiState,
                    showBackButton = false,
                    showMiniPlayer = false,
                    onBack = {},
                    onTrackSelected = { track -> viewModel.playTrack(track) },
                    onSearchQueryChange = viewModel::updateSearchQuery,
                    onSortOptionChange = viewModel::updateSortOption,
                    onRefresh = viewModel::refreshLibrary,
                    onRequestPermission = requestAudioPermission,
                    onOpenSettings = openSettings,
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onNext = viewModel::skipToNext,
                    onMiniPlayerClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = PlayerRoute,
                enterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(280),
                    ) + fadeIn(animationSpec = tween(220))
                },
                exitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(280),
                    ) + fadeOut(animationSpec = tween(200))
                },
                popEnterTransition = {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(280),
                    ) + fadeIn(animationSpec = tween(220))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(280),
                    ) + fadeOut(animationSpec = tween(200))
                },
            ) {
                composable<PlayerRoute> {
                    PlayerScreen(
                        uiState = uiState,
                        showLibraryButton = true,
                        onOpenLibrary = {
                            navController.navigate(LibraryRoute) {
                                launchSingleTop = true
                            }
                        },
                        onRequestPermission = requestAudioPermission,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onPrevious = viewModel::skipToPrevious,
                        onNext = viewModel::skipToNext,
                        onSeekTo = viewModel::seekTo,
                        onToggleShuffle = viewModel::toggleShuffle,
                        onCycleRepeat = viewModel::cycleRepeatMode,
                        onVolumeChange = viewModel::setVolume,
                    )
                }

                composable<LibraryRoute> {
                    LibraryScreen(
                        uiState = uiState,
                        showBackButton = true,
                        showMiniPlayer = true,
                        onBack = { navController.popBackStack() },
                        onTrackSelected = { track -> viewModel.playTrack(track) },
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        onSortOptionChange = viewModel::updateSortOption,
                        onRefresh = viewModel::refreshLibrary,
                        onRequestPermission = requestAudioPermission,
                        onOpenSettings = openSettings,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onNext = viewModel::skipToNext,
                        onMiniPlayerClick = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

private fun rememberRequiredPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.POST_NOTIFICATIONS,
        )
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
}

private fun hasAudioReadPermission(context: Context): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
