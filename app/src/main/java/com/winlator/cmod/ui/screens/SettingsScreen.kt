package com.winlator.cmod.ui.screens

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.winlator.cmod.container.Container

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    var activeTab by remember { mutableStateOf(0) }
    val tabs = listOf("Geral", "Gráficos", "Áudio", "Sistema", "Variáveis")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeTab == index,
                    onClick = { activeTab = index },
                    text = { Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            when (activeTab) {
                0 -> GeneralSettingsTab(prefs)
                1 -> GraphicsSettingsTab(prefs)
                2 -> AudioSettingsTab(prefs)
                3 -> SystemSettingsTab(prefs)
                4 -> EnvVarsSettingsTab(prefs)
            }
        }
    }
}

@Composable
fun GeneralSettingsTab(prefs: SharedPreferences) {
    var showFps by remember { mutableStateOf(prefs.getBoolean("show_fps", false)) }
    var useLegacyPackaging by remember { mutableStateOf(prefs.getBoolean("use_legacy_packaging", true)) }
    var vibrationFeedback by remember { mutableStateOf(prefs.getBoolean("vibration_feedback", true)) }
    var autoSaveControls by remember { mutableStateOf(prefs.getBoolean("auto_save_controls", true)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Opções do Aplicativo",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Mostrar Contador de FPS", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(text = "Mostra uma sobreposição com a taxa de quadros atual em tempo real.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = showFps, onCheckedChange = { showFps = it; prefs.edit().putBoolean("show_fps", it).apply() })
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Empacotamento de JNI Legado", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(text = "Necessário para certas GPUs compartilharem texturas nativamente.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = useLegacyPackaging, onCheckedChange = { useLegacyPackaging = it; prefs.edit().putBoolean("use_legacy_packaging", it).apply() })
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Feedback Haptic / Vibração", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(text = "Vibra levemente ao tocar nos botões virtuais da tela.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = vibrationFeedback, onCheckedChange = { vibrationFeedback = it; prefs.edit().putBoolean("vibration_feedback", it).apply() })
        }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Salvar Controles Automaticamente", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(text = "Grava as modificações de layout de botões de forma contínua.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = autoSaveControls, onCheckedChange = { autoSaveControls = it; prefs.edit().putBoolean("auto_save_controls", it).apply() })
        }
    }
}

@Composable
fun GraphicsSettingsTab(prefs: SharedPreferences) {
    var screenSize by remember { mutableStateOf(prefs.getString("global_container_screen_size", Container.DEFAULT_SCREEN_SIZE) ?: Container.DEFAULT_SCREEN_SIZE) }
    var graphicsDriver by remember { mutableStateOf(prefs.getString("global_container_graphics_driver", Container.DEFAULT_GRAPHICS_DRIVER) ?: Container.DEFAULT_GRAPHICS_DRIVER) }
    var dxWrapper by remember { mutableStateOf(prefs.getString("global_container_dxwrapper", Container.DEFAULT_DXWRAPPER) ?: Container.DEFAULT_DXWRAPPER) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(text = "Configurações Gráficas Globais", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        SettingsDropdown(
            label = "Resolução da Tela",
            options = listOf("800x600", "1024x768", "1280x720", "1366x768", "1920x1080"),
            selectedOption = screenSize,
            onOptionSelected = { screenSize = it; prefs.edit().putString("global_container_screen_size", it).apply() }
        )

        SettingsDropdown(
            label = "Driver de Vídeo",
            options = listOf("wrapper", "turnip", "virgl"),
            selectedOption = graphicsDriver,
            onOptionSelected = { graphicsDriver = it; prefs.edit().putString("global_container_graphics_driver", it).apply() }
        )

        SettingsDropdown(
            label = "DX Wrapper",
            options = listOf("dxvk+vkd3d", "wined3d"),
            selectedOption = dxWrapper,
            onOptionSelected = { dxWrapper = it; prefs.edit().putString("global_container_dxwrapper", it).apply() }
        )
    }
}

@Composable
fun AudioSettingsTab(prefs: SharedPreferences) {
    var audioDriver by remember { mutableStateOf(prefs.getString("global_container_audio_driver", Container.DEFAULT_AUDIO_DRIVER) ?: Container.DEFAULT_AUDIO_DRIVER) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(text = "Configurações de Áudio", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        SettingsDropdown(
            label = "Driver de Som",
            options = listOf("alsa", "pulseaudio"),
            selectedOption = audioDriver,
            onOptionSelected = { audioDriver = it; prefs.edit().putString("global_container_audio_driver", it).apply() }
        )
    }
}

@Composable
fun SystemSettingsTab(prefs: SharedPreferences) {
    var wineVersion by remember { mutableStateOf(prefs.getString("global_container_wine_version", "proton-9.0-x86_64") ?: "proton-9.0-x86_64") }
    var box64Preset by remember { mutableStateOf(prefs.getString("global_container_box64_preset", "compatibility") ?: "compatibility") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(text = "Sistema & Emulação", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

        SettingsDropdown(
            label = "Versão do Wine Padrão",
            options = listOf("proton-9.0-x86_64"),
            selectedOption = wineVersion,
            onOptionSelected = { wineVersion = it; prefs.edit().putString("global_container_wine_version", it).apply() }
        )

        SettingsDropdown(
            label = "Box64 Preset",
            options = listOf("compatibility", "performance"),
            selectedOption = box64Preset,
            onOptionSelected = { box64Preset = it; prefs.edit().putString("global_container_box64_preset", it).apply() }
        )
    }
}

@Composable
fun EnvVarsSettingsTab(prefs: SharedPreferences) {
    var envVars by remember { mutableStateOf(prefs.getString("global_container_env_vars", Container.DEFAULT_ENV_VARS) ?: Container.DEFAULT_ENV_VARS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Variáveis de Ambiente Globais", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            text = "Essas variáveis serão injetadas no contêiner durante a execução.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = envVars,
            onValueChange = { envVars = it; prefs.edit().putString("global_container_env_vars", it).apply() },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            label = { Text("Variáveis (espaço como separador)") },
            maxLines = 10
        )
    }
}

@Composable
fun SettingsDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(selectedOption, color = MaterialTheme.colorScheme.onSurface)
                    Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

