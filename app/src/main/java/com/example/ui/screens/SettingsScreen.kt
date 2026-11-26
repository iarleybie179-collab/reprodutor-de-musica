package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.data.local.AppSettings
import com.example.data.model.EqPreset
import com.example.data.model.SortOrder
import com.example.data.model.VisualEffectStyle
import com.example.player.EqualizerState
import com.example.ui.components.GlassCard
import com.example.ui.theme.AccentThemePreset
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  settings: AppSettings,
  equalizerState: EqualizerState,
  allFolders: List<Pair<String, String>>,
  onPickWallpaper: (Uri) -> Unit,
  onRestoreDefaultWallpaper: () -> Unit,
  onDimChange: (Float) -> Unit,
  onBlurChange: (Float) -> Unit,
  onAccentChange: (AccentThemePreset) -> Unit,
  onVisualEffectChange: (VisualEffectStyle) -> Unit,
  onAnimationsChange: (Boolean) -> Unit,
  onParticlesChange: (Boolean) -> Unit,
  onIntroEnabledChange: (Boolean) -> Unit,
  onIntroVoiceChange: (Boolean) -> Unit,
  onIntroSubtitleChange: (Boolean) -> Unit,
  onIntroDurationChange: (Float) -> Unit,
  onPreviewIntro: () -> Unit,
  onAutoplayChange: (Boolean) -> Unit,
  onShuffleChange: (Boolean) -> Unit,
  onRepeatModeChange: (Int) -> Unit,
  onResumePlaybackChange: (Boolean) -> Unit,
  onCrossfadeChange: (Boolean) -> Unit,
  onBackgroundPlaybackChange: (Boolean) -> Unit,
  onEqEnabledChange: (Boolean) -> Unit,
  onEqPresetChange: (EqPreset) -> Unit,
  onEqBandsChange: (Float, Float, Float) -> Unit,
  onRefreshLibrary: () -> Unit,
  onDefaultSortChange: (SortOrder) -> Unit,
  onToggleFolderHidden: (String) -> Unit,
  onHideShortClipsChange: (Boolean) -> Unit,
  onBatterySaverChange: (Boolean) -> Unit,
  onReduceAnimationsChange: (Boolean) -> Unit,
  onReduceEffectsChange: (Boolean) -> Unit,
) {
  val wallpaperPicker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { uri ->
    if (uri != null) {
      onPickWallpaper(uri)
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 140.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      Text(
        text = "Configurações",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }

    // 1. PERSONALIZAÇÃO
    item {
      SettingsSectionCard(icon = Icons.Filled.Palette, title = "Personalização") {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = {
              wallpaperPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.weight(1f).testTag("pick_wallpaper_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Escolher Fundo", maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          OutlinedButton(
            onClick = onRestoreDefaultWallpaper,
            enabled = settings.customWallpaperPath != null,
            modifier = Modifier.weight(1f).testTag("restore_wallpaper_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.Filled.Restore, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Restaurar Padrão", maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }

        SettingsSliderRow(
          title = "Escurecimento do fundo",
          valueText = "${(settings.wallpaperDimIntensity * 100).toInt()}%",
          value = settings.wallpaperDimIntensity,
          range = 0.1f..0.9f,
          onValueChange = onDimChange,
        )

        SettingsSliderRow(
          title = "Desfoque do plano de fundo",
          valueText = "${settings.wallpaperBlurRadius.toInt()} dp",
          value = settings.wallpaperBlurRadius,
          range = 0f..24f,
          onValueChange = onBlurChange,
        )

        Text("Cores da interface", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          AccentThemePreset.entries.forEach { preset ->
            FilterChip(
              selected = settings.accentPreset == preset,
              onClick = { onAccentChange(preset) },
              leadingIcon = {
                Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(preset.primary))
              },
              label = { Text(preset.label) },
            )
          }
        }

        Text("Efeitos visuais", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          VisualEffectStyle.entries.forEach { style ->
            FilterChip(
              selected = settings.visualEffectStyle == style,
              onClick = { onVisualEffectChange(style) },
              label = { Text(style.label) },
            )
          }
        }

        SettingsSwitchRow(
          title = "Ativar animações",
          subtitle = "Transições suaves e pulso no player",
          checked = settings.animationsEnabled,
          onCheckedChange = onAnimationsChange,
        )
        SettingsSwitchRow(
          title = "Partículas luminosas de Ki",
          subtitle = "Partículas flutuantes no plano de fundo",
          checked = settings.particlesEnabled,
          onCheckedChange = onParticlesChange,
        )
      }
    }

    // 2. TELA DE ABERTURA
    item {
      SettingsSectionCard(icon = Icons.Filled.Movie, title = "Tela de Abertura") {
        SettingsSwitchRow(
          title = "Exibir abertura ao iniciar",
          subtitle = "Animação cinematográfica Dragon Ball Z",
          checked = settings.introEnabled,
          onCheckedChange = onIntroEnabledChange,
        )
        SettingsSwitchRow(
          title = "Voz na abertura",
          subtitle = "Narração da frase \"Não seja um beta.\"",
          checked = settings.introVoiceEnabled,
          onCheckedChange = onIntroVoiceChange,
        )
        SettingsSwitchRow(
          title = "Legenda animada",
          subtitle = "Exibir legenda sincronizada com brilho neon",
          checked = settings.introSubtitleEnabled,
          onCheckedChange = onIntroSubtitleChange,
        )
        SettingsSliderRow(
          title = "Duração da abertura",
          valueText = String.format("%.1fs", settings.introDurationSec),
          value = settings.introDurationSec,
          range = 3.0f..6.0f,
          onValueChange = onIntroDurationChange,
        )
        OutlinedButton(
          onClick = onPreviewIntro,
          modifier = Modifier.fillMaxWidth().testTag("preview_intro_button"),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
        ) {
          Text("Testar Abertura Agora")
        }
      }
    }

    // 3. REPRODUÇÃO
    item {
      SettingsSectionCard(icon = Icons.Filled.PlayCircle, title = "Reprodução") {
        SettingsSwitchRow(
          title = "Reprodução automática",
          subtitle = "Avançar automaticamente para a próxima faixa",
          checked = settings.autoplayEnabled,
          onCheckedChange = onAutoplayChange,
        )
        SettingsSwitchRow(
          title = "Ordem aleatória padrão",
          checked = settings.shuffleEnabled,
          onCheckedChange = onShuffleChange,
        )
        Text("Modo de repetição", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(
            Player.REPEAT_MODE_OFF to "Desativado",
            Player.REPEAT_MODE_ALL to "Repetir Tudo",
            Player.REPEAT_MODE_ONE to "Repetir Faixa",
          ).forEach { (mode, label) ->
            FilterChip(
              selected = settings.repeatMode == mode,
              onClick = { onRepeatModeChange(mode) },
              label = { Text(label) },
            )
          }
        }
        SettingsSwitchRow(
          title = "Continuar de onde parou",
          subtitle = "Restaurar última música e posição ao abrir",
          checked = settings.resumeLastPlayback,
          onCheckedChange = onResumePlaybackChange,
        )
        SettingsSwitchRow(
          title = "Transição suave entre músicas",
          subtitle = "Entrada gradual de volume ao trocar de faixa",
          checked = settings.crossfadeEnabled,
          onCheckedChange = onCrossfadeChange,
        )
        SettingsSwitchRow(
          title = "Reprodução em segundo plano",
          subtitle = "Manter controles ativos na notificação do sistema",
          checked = settings.backgroundPlaybackEnabled,
          onCheckedChange = onBackgroundPlaybackChange,
        )
      }
    }

    // 4. ÁUDIO
    item {
      SettingsSectionCard(icon = Icons.Filled.Equalizer, title = "Áudio e Equalizador") {
        EqualizerSheetContent(
          equalizerState = equalizerState,
          settings = settings,
          onEqEnabledChange = onEqEnabledChange,
          onEqPresetChange = onEqPresetChange,
          onEqBandsChange = onEqBandsChange,
        )
      }
    }

    // 5. BIBLIOTECA
    item {
      SettingsSectionCard(icon = Icons.Filled.LibraryMusic, title = "Biblioteca") {
        Button(
          onClick = onRefreshLibrary,
          modifier = Modifier.fillMaxWidth().testTag("settings_rescan_button"),
          shape = RoundedCornerShape(14.dp),
        ) {
          Icon(Icons.Filled.Refresh, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Atualizar e Reexaminar Músicas")
        }

        Text("Ordenação padrão", style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          SortOrder.entries.forEach { order ->
            FilterChip(
              selected = settings.defaultSortOrder == order,
              onClick = { onDefaultSortChange(order) },
              label = { Text(order.label) },
            )
          }
        }

        SettingsSwitchRow(
          title = "Ocultar áudios muito curtos (< 5s)",
          subtitle = "Evita toques e notificações curtas na biblioteca",
          checked = settings.hideShortClips,
          onCheckedChange = onHideShortClipsChange,
        )

        if (allFolders.isNotEmpty()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.FolderOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Pastas visíveis na biblioteca", style = MaterialTheme.typography.titleSmall)
          }
          allFolders.forEach { (path, name) ->
            val visible = !settings.hiddenFolders.contains(path)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { onToggleFolderHidden(path) }
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Checkbox(checked = visible, onCheckedChange = { onToggleFolderHidden(path) })
              Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(path, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
            }
          }
        }
      }
    }

    // 6. DESEMPENHO
    item {
      SettingsSectionCard(icon = Icons.Filled.BatterySaver, title = "Desempenho") {
        SettingsSwitchRow(
          title = "Modo economia de bateria",
          subtitle = "Desativa partículas e efeitos visuais intensos",
          checked = settings.batterySaverMode,
          onCheckedChange = onBatterySaverChange,
        )
        SettingsSwitchRow(
          title = "Reduzir animações",
          checked = settings.reduceAnimations,
          onCheckedChange = onReduceAnimationsChange,
        )
        SettingsSwitchRow(
          title = "Reduzir efeitos visuais",
          checked = settings.reduceEffects,
          onCheckedChange = onReduceEffectsChange,
        )
      }
    }

    // 7. SOBRE
    item {
      SettingsSectionCard(icon = Icons.Filled.Info, title = "Sobre") {
        Text("DBZ Ki Player", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Text("Versão 2.0", style = MaterialTheme.typography.titleSmall)
        Text(
          text = "Reprodutor de música local com motor Media3 ExoPlayer, equalização por hardware, playlists persistentes em Room e personalização visual inspirada em Dragon Ball Z.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
        Text(
          text = "Desenvolvedor: Equipe DBZ Ki Player • Áudio local e privacidade em primeiro lugar.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondary,
        )
      }
    }
  }
}

@Composable
private fun SettingsSectionCard(
  icon: ImageVector,
  title: String,
  content: @Composable () -> Unit,
) {
  GlassCard(modifier = Modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
      }
      content()
    }
  }
}

@Composable
private fun SettingsSwitchRow(
  title: String,
  subtitle: String? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable { onCheckedChange(!checked) }
      .padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.bodyLarge)
      if (subtitle != null) {
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
      }
    }
    Spacer(modifier = Modifier.width(12.dp))
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
private fun SettingsSliderRow(
  title: String,
  valueText: String,
  value: Float,
  range: ClosedFloatingPointRange<Float>,
  onValueChange: (Float) -> Unit,
) {
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(valueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
    Slider(value = value, onValueChange = onValueChange, valueRange = range)
  }
}
