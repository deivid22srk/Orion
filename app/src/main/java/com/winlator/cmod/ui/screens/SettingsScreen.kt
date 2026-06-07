package com.winlator.cmod.ui.screens

import android.content.SharedPreferences
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.preference.PreferenceManager
import com.winlator.cmod.container.Container
import com.winlator.cmod.contents.ContentProfile
import com.winlator.cmod.contents.ContentsManager
import com.winlator.cmod.core.WineInfo
import com.winlator.cmod.core.WineThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

enum class SettingsScreenType {
    MAIN,
    GENERAL,
    GRAPHICS,
    AUDIO,
    SYSTEM,
    ENV_VARS,
    WIN_COMPONENTS,
    DRIVES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    var currentScreen by remember { mutableStateOf(SettingsScreenType.MAIN) }

    Scaffold(
        topBar = {
            if (currentScreen != SettingsScreenType.MAIN) {
                val title = when (currentScreen) {
                    SettingsScreenType.GENERAL -> "Geral"
                    SettingsScreenType.GRAPHICS -> "Gráficos & Renderização"
                    SettingsScreenType.AUDIO -> "Áudio & Som"
                    SettingsScreenType.SYSTEM -> "Sistema & Emulação"
                    SettingsScreenType.ENV_VARS -> "Variáveis de Ambiente"
                    SettingsScreenType.WIN_COMPONENTS -> "Componentes do Windows"
                    SettingsScreenType.DRIVES -> "Drives Mapeados"
                    else -> "Configurações"
                }
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { currentScreen = SettingsScreenType.MAIN }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (targetState == SettingsScreenType.MAIN) {
                        (slideInHorizontally { width -> -width / 2 } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> width / 2 } + fadeOut())
                    } else {
                        (slideInHorizontally { width -> width / 2 } + fadeIn())
                            .togetherWith(slideOutHorizontally { width -> -width / 2 } + fadeOut())
                    }
                },
                label = "settings_subscreen_transition",
                modifier = Modifier.fillMaxSize()
            ) { screen ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (screen) {
                        SettingsScreenType.MAIN -> SettingsMainScreen(onNavigate = { currentScreen = it })
                        SettingsScreenType.GENERAL -> GeneralSettingsScreen(prefs)
                        SettingsScreenType.GRAPHICS -> GraphicsSettingsScreen(prefs)
                        SettingsScreenType.AUDIO -> AudioSettingsScreen(prefs)
                        SettingsScreenType.SYSTEM -> SystemSettingsScreen(prefs)
                        SettingsScreenType.ENV_VARS -> EnvVarsSettingsScreen(prefs)
                        SettingsScreenType.WIN_COMPONENTS -> WinComponentsSettingsScreen(prefs)
                        SettingsScreenType.DRIVES -> DrivesSettingsScreen(prefs)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsMainScreen(onNavigate: (SettingsScreenType) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Configurações Globais",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Text(
            text = "Defina as configurações de compatibilidade unificadas que serão herdadas por todos os contêineres do Orion.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        CategoryItem(
            title = "Geral",
            description = "Contador de FPS, empacotamento JNI, feedback e controles.",
            icon = Icons.Default.Settings,
            onClick = { onNavigate(SettingsScreenType.GENERAL) }
        )

        CategoryItem(
            title = "Gráficos & Renderização",
            description = "Resolução, driver de vídeo, DX Wrapper, renderizador nativo, limite de FPS.",
            icon = Icons.Default.Edit,
            onClick = { onNavigate(SettingsScreenType.GRAPHICS) }
        )

        CategoryItem(
            title = "Áudio & Som",
            description = "Driver de áudio padrão (alsa ou pulseaudio).",
            icon = Icons.Default.PlayArrow,
            onClick = { onNavigate(SettingsScreenType.AUDIO) }
        )

        CategoryItem(
            title = "Sistema & Emulação",
            description = "Versão do Wine, presets do Box64, emulador padrão e núcleos de CPU.",
            icon = Icons.Default.MoreVert,
            onClick = { onNavigate(SettingsScreenType.SYSTEM) }
        )

        CategoryItem(
            title = "Variáveis de Ambiente",
            description = "Modificar variáveis de ambiente globais injetadas por padrão.",
            icon = Icons.Default.Edit,
            onClick = { onNavigate(SettingsScreenType.ENV_VARS) }
        )

        CategoryItem(
            title = "Componentes do Windows",
            description = "Definir se DLLs específicas do Windows serão Builtin ou Native.",
            icon = Icons.Default.Settings,
            onClick = { onNavigate(SettingsScreenType.WIN_COMPONENTS) }
        )

        CategoryItem(
            title = "Drives Mapeados",
            description = "Mapear letras de drives para diretórios locais do Android.",
            icon = Icons.Default.Add,
            onClick = { onNavigate(SettingsScreenType.DRIVES) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Text(
                text = "→",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun GeneralSettingsScreen(prefs: SharedPreferences) {
    var showFps by remember { mutableStateOf(prefs.getBoolean("show_fps", false)) }
    var useLegacyPackaging by remember { mutableStateOf(prefs.getBoolean("use_legacy_packaging", true)) }
    var vibrationFeedback by remember { mutableStateOf(prefs.getBoolean("vibration_feedback", true)) }
    var autoSaveControls by remember { mutableStateOf(prefs.getBoolean("auto_save_controls", true)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SettingsSwitchRow(
            label = "Mostrar Contador de FPS",
            description = "Mostra uma sobreposição com a taxa de quadros atual em tempo real.",
            checked = showFps,
            onCheckedChange = { showFps = it; prefs.edit().putBoolean("show_fps", it).apply() }
        )

        HorizontalDivider()

        SettingsSwitchRow(
            label = "Empacotamento de JNI Legado",
            description = "Necessário para certas GPUs compartilharem texturas nativamente.",
            checked = useLegacyPackaging,
            onCheckedChange = { useLegacyPackaging = it; prefs.edit().putBoolean("use_legacy_packaging", it).apply() }
        )

        HorizontalDivider()

        SettingsSwitchRow(
            label = "Feedback Haptic / Vibração",
            description = "Vibra levemente ao tocar nos botões virtuais da tela.",
            checked = vibrationFeedback,
            onCheckedChange = { vibrationFeedback = it; prefs.edit().putBoolean("vibration_feedback", it).apply() }
        )

        HorizontalDivider()

        SettingsSwitchRow(
            label = "Salvar Controles Automaticamente",
            description = "Grava as modificações de layout de botões de forma contínua.",
            checked = autoSaveControls,
            onCheckedChange = { autoSaveControls = it; prefs.edit().putBoolean("auto_save_controls", it).apply() }
        )
    }
}

@Composable
fun GraphicsSettingsScreen(prefs: SharedPreferences) {
    var screenSize by remember { mutableStateOf(prefs.getString("global_container_screen_size", Container.DEFAULT_SCREEN_SIZE) ?: Container.DEFAULT_SCREEN_SIZE) }
    var graphicsDriver by remember { mutableStateOf(prefs.getString("global_container_graphics_driver", Container.DEFAULT_GRAPHICS_DRIVER) ?: Container.DEFAULT_GRAPHICS_DRIVER) }
    var dxWrapper by remember { mutableStateOf(prefs.getString("global_container_dxwrapper", Container.DEFAULT_DXWRAPPER) ?: Container.DEFAULT_DXWRAPPER) }
    var rendererNative by remember { mutableStateOf(prefs.getBoolean("global_container_renderer_native", false)) }
    var rendererPresentMode by remember { mutableStateOf(prefs.getString("global_container_renderer_present_mode", "fifo") ?: "fifo") }
    var rendererDriverId by remember { mutableStateOf(prefs.getString("global_container_renderer_driver_id", "") ?: "") }
    var rendererRefreshRateLimit by remember { mutableStateOf(prefs.getString("global_container_renderer_refresh_rate_limit", "0") ?: "0") }
    var fullscreenStretched by remember { mutableStateOf(prefs.getBoolean("global_container_fullscreen_stretched", false)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsDropdown(
            label = "Resolução da Tela",
            options = listOf("800x600", "1024x768", "1280x720", "1366x768", "1600x900", "1920x1080"),
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
            options = listOf("dxvk+vkd3d", "wined3d", "dxvk", "vkd3d", "cnc-ddraw"),
            selectedOption = dxWrapper,
            onOptionSelected = { dxWrapper = it; prefs.edit().putString("global_container_dxwrapper", it).apply() }
        )

        SettingsSwitchRow(
            label = "Renderizador Nativo",
            description = "Ativa o renderizador do sistema nativo ao invés do software fallback.",
            checked = rendererNative,
            onCheckedChange = { rendererNative = it; prefs.edit().putBoolean("global_container_renderer_native", it).apply() }
        )

        SettingsDropdown(
            label = "Modo de Apresentação do Renderizador",
            options = listOf("fifo", "mailbox", "immediate", "relaxed"),
            selectedOption = rendererPresentMode,
            onOptionSelected = { rendererPresentMode = it; prefs.edit().putString("global_container_renderer_present_mode", it).apply() }
        )

        OutlinedTextField(
            value = rendererDriverId,
            onValueChange = { rendererDriverId = it; prefs.edit().putString("global_container_renderer_driver_id", it).apply() },
            label = { Text("ID do Driver do Renderizador") },
            placeholder = { Text("Ex: 0, 1") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = rendererRefreshRateLimit,
            onValueChange = { 
                rendererRefreshRateLimit = it
                val limit = it.toIntOrNull() ?: 0
                prefs.edit().putString("global_container_renderer_refresh_rate_limit", limit.toString()).apply()
            },
            label = { Text("Limite de FPS (0 = Ilimitado)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        SettingsSwitchRow(
            label = "Tela Cheia Esticada (Stretched)",
            description = "Força a exibição a preencher toda a tela do celular ignorando aspect ratio.",
            checked = fullscreenStretched,
            onCheckedChange = { fullscreenStretched = it; prefs.edit().putBoolean("global_container_fullscreen_stretched", it).apply() }
        )
    }
}

@Composable
fun AudioSettingsScreen(prefs: SharedPreferences) {
    var audioDriver by remember { mutableStateOf(prefs.getString("global_container_audio_driver", Container.DEFAULT_AUDIO_DRIVER) ?: Container.DEFAULT_AUDIO_DRIVER) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SettingsDropdown(
            label = "Driver de Som Padrão",
            options = listOf("alsa", "pulseaudio"),
            selectedOption = audioDriver,
            onOptionSelected = { audioDriver = it; prefs.edit().putString("global_container_audio_driver", it).apply() }
        )
    }
}

@Composable
fun SystemSettingsScreen(prefs: SharedPreferences) {
    val context = LocalContext.current
    val contentsManager = remember { ContentsManager(context) }
    val wineVersions = remember { mutableStateListOf<String>() }
    var wineVersion by remember { mutableStateOf(prefs.getString("global_container_wine_version", "proton-9.0-x86_64") ?: "proton-9.0-x86_64") }
    var box64Preset by remember { mutableStateOf(prefs.getString("global_container_box64_preset", "compatibility") ?: "compatibility") }
    var emulator by remember { mutableStateOf(prefs.getString("global_container_emulator", "Box64") ?: "Box64") }
    var desktopTheme by remember { mutableStateOf(prefs.getString("global_container_desktop_theme", WineThemeManager.DEFAULT_DESKTOP_THEME) ?: WineThemeManager.DEFAULT_DESKTOP_THEME) }
    var startupSelection by remember { mutableStateOf(prefs.getInt("global_container_startup_selection", Container.STARTUP_SELECTION_ESSENTIAL.toInt())) }

    // CPU Affinity Cores Setup
    val numCores = remember { Runtime.getRuntime().availableProcessors().coerceAtLeast(8) }
    val initialCpuList = remember {
        val rawList = prefs.getString("global_container_cpu_list", null) ?: Container.getFallbackCPUList()
        parseCpuList(rawList, numCores)
    }
    val cpuCoresState = remember { mutableStateListOf<Boolean>().apply { addAll(initialCpuList) } }

    val initialCpuListWoW64 = remember {
        val rawList = prefs.getString("global_container_cpu_list_wow64", null) ?: Container.getFallbackCPUListWoW64()
        parseCpuList(rawList, numCores)
    }
    val cpuCoresWoW64State = remember { mutableStateListOf<Boolean>().apply { addAll(initialCpuListWoW64) } }

    fun saveCpuList() {
        prefs.edit().putString("global_container_cpu_list", formatCpuList(cpuCoresState)).apply()
    }

    fun saveCpuListWoW64() {
        prefs.edit().putString("global_container_cpu_list_wow64", formatCpuList(cpuCoresWoW64State)).apply()
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val wineList = mutableListOf<String>()
            try {
                val staticVersions = context.resources.getStringArray(com.winlator.cmod.R.array.wine_entries)
                wineList.addAll(staticVersions)
            } catch (e: Exception) {}
            
            try {
                for (profile in contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_WINE)) {
                    if (profile.remoteUrl != null) continue
                    wineList.add(ContentsManager.getEntryName(profile))
                }
                for (profile in contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_PROTON)) {
                    if (profile.remoteUrl != null) continue
                    wineList.add(ContentsManager.getEntryName(profile))
                }
            } catch (e: Exception) {}

            if (wineList.isEmpty()) {
                wineList.add("proton-9.0-x86_64")
            }
            
            withContext(Dispatchers.Main) {
                wineVersions.clear()
                wineVersions.addAll(wineList)
                if (!wineVersions.contains(wineVersion) && wineVersions.isNotEmpty()) {
                    wineVersion = wineVersions.first()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (wineVersions.isNotEmpty()) {
            SettingsDropdown(
                label = "Versão do Wine Padrão",
                options = wineVersions,
                selectedOption = wineVersion,
                onOptionSelected = { wineVersion = it; prefs.edit().putString("global_container_wine_version", it).apply() }
            )
        }

        SettingsDropdown(
            label = "Preset do Box64",
            options = listOf("compatibility", "performance"),
            selectedOption = box64Preset,
            onOptionSelected = { box64Preset = it; prefs.edit().putString("global_container_box64_preset", it).apply() }
        )

        SettingsDropdown(
            label = "Emulador",
            options = listOf("Box64", "FEXCore"),
            selectedOption = emulator,
            onOptionSelected = { emulator = it; prefs.edit().putString("global_container_emulator", it).apply() }
        )

        SettingsDropdown(
            label = "Tema do Desktop",
            options = listOf("classic", "dark", "light"),
            selectedOption = desktopTheme,
            onOptionSelected = { desktopTheme = it; prefs.edit().putString("global_container_desktop_theme", it).apply() }
        )

        val startupOptions = listOf("Essencial", "Normal", "Completo")
        val selectedStartupLabel = when (startupSelection) {
            Container.STARTUP_SELECTION_ESSENTIAL.toInt() -> "Essencial"
            Container.STARTUP_SELECTION_NORMAL.toInt() -> "Normal"
            Container.STARTUP_SELECTION_FULL.toInt() -> "Completo"
            else -> "Essencial"
        }
        SettingsDropdown(
            label = "Seleção de Inicialização",
            options = startupOptions,
            selectedOption = selectedStartupLabel,
            onOptionSelected = { label ->
                val code = when (label) {
                    "Essencial" -> Container.STARTUP_SELECTION_ESSENTIAL.toInt()
                    "Normal" -> Container.STARTUP_SELECTION_NORMAL.toInt()
                    "Completo" -> Container.STARTUP_SELECTION_FULL.toInt()
                    else -> Container.STARTUP_SELECTION_ESSENTIAL.toInt()
                }
                startupSelection = code
                prefs.edit().putInt("global_container_startup_selection", code).apply()
            }
        )

        HorizontalDivider()

        // CPU Affinity Cores Setup UI
        Text(
            text = "Afinidade de Núcleos CPU (Normal)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val cols = 4
            val rows = (numCores + cols - 1) / cols
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in 0 until cols) {
                        val coreIdx = r * cols + c
                        if (coreIdx < numCores) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = cpuCoresState[coreIdx],
                                    onCheckedChange = { checked ->
                                        cpuCoresState[coreIdx] = checked
                                        saveCpuList()
                                    }
                                )
                                Text("Core $coreIdx", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        Text(
            text = "Afinidade de Núcleos CPU (WoW64)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val cols = 4
            val rows = (numCores + cols - 1) / cols
            for (r in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in 0 until cols) {
                        val coreIdx = r * cols + c
                        if (coreIdx < numCores) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Checkbox(
                                    checked = cpuCoresWoW64State[coreIdx],
                                    onCheckedChange = { checked ->
                                        cpuCoresWoW64State[coreIdx] = checked
                                        saveCpuListWoW64()
                                    }
                                )
                                Text("Core $coreIdx", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnvVarsSettingsScreen(prefs: SharedPreferences) {
    var envVars by remember { mutableStateOf(prefs.getString("global_container_env_vars", Container.DEFAULT_ENV_VARS) ?: Container.DEFAULT_ENV_VARS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Defina as variáveis de ambiente padrões separadas por espaço (Ex: KEY=value KEY2=value2).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = envVars,
            onValueChange = { 
                envVars = it
                prefs.edit().putString("global_container_env_vars", it).apply()
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            label = { Text("Variáveis de Ambiente") },
            maxLines = 15
        )
    }
}

@Composable
fun WinComponentsSettingsScreen(prefs: SharedPreferences) {
    val winCompStr = prefs.getString("global_container_wincomponents", Container.DEFAULT_WINCOMPONENTS) ?: Container.DEFAULT_WINCOMPONENTS
    val winComponentsMap = remember { mutableStateMapOf<String, Int>().apply { putAll(parseWinComponents(winCompStr)) } }

    fun saveWinComponents() {
        prefs.edit().putString("global_container_wincomponents", formatWinComponents(winComponentsMap)).apply()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Escolha se componentes ou DLLs específicas usarão a implementação Builtin (integrada do Wine) ou Native (nativa do Windows).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        winComponentsMap.keys.sorted().forEach { component ->
            val value = winComponentsMap[component] ?: 0
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = component.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (value == 1) "Native" else "Builtin",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (value == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = value == 1,
                            onCheckedChange = { checked ->
                                winComponentsMap[component] = if (checked) 1 else 0
                                saveWinComponents()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DrivesSettingsScreen(prefs: SharedPreferences) {
    val drivesStr = prefs.getString("global_container_drives", Container.DEFAULT_DRIVES) ?: Container.DEFAULT_DRIVES
    val drivesList = remember { mutableStateListOf<Pair<String, String>>().apply { addAll(parseDrives(drivesStr)) } }

    fun saveDrives() {
        prefs.edit().putString("global_container_drives", formatDrives(drivesList)).apply()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Unidades de Armazenamento",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = {
                    val letters = ('E'..'Z').map { it.toString() }
                    val used = drivesList.map { it.first }
                    val available = letters.filter { !used.contains(it) }
                    if (available.isNotEmpty()) {
                        drivesList.add(available.first() to "/storage/emulated/0")
                        saveDrives()
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Adicionar")
            }
        }

        if (drivesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhuma unidade mapeada.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        drivesList.forEachIndexed { index, drive ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Unidade ${drive.first}:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (drive.first != "C" && drive.first != "D") {
                            IconButton(
                                onClick = {
                                    drivesList.removeAt(index)
                                    saveDrives()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remover Drive",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = drive.second,
                        onValueChange = { newPath ->
                            drivesList[index] = drive.first to newPath
                            saveDrives()
                        },
                        label = { Text("Caminho no Android") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// Helpers
@Composable
fun SettingsSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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

// Parsing Helpers
private fun parseCpuList(cpuListStr: String, numCores: Int): List<Boolean> {
    val list = MutableList(numCores) { false }
    if (cpuListStr.isEmpty()) return list
    cpuListStr.split(",").forEach {
        val core = it.toIntOrNull()
        if (core != null && core in 0 until numCores) {
            list[core] = true
        }
    }
    return list
}

private fun formatCpuList(cpuList: List<Boolean>): String {
    val list = mutableListOf<Int>()
    cpuList.forEachIndexed { index, enabled ->
        if (enabled) {
            list.add(index)
        }
    }
    return list.joinToString(",")
}

private fun parseWinComponents(winCompStr: String): Map<String, Int> {
    val map = mutableMapOf<String, Int>()
    val separator = if (winCompStr.contains(",")) "," else ";"
    winCompStr.split(separator).forEach {
        val parts = it.split("=")
        if (parts.size == 2) {
            map[parts[0]] = parts[1].toIntOrNull() ?: 1
        }
    }
    return map
}

private fun formatWinComponents(map: Map<String, Int>): String {
    return map.entries.joinToString(",") { "${it.key}=${it.value}" }
}

private fun parseDrives(drivesStr: String): List<Pair<String, String>> {
    val list = mutableListOf<Pair<String, String>>()
    if (drivesStr.isEmpty()) return list
    val parts = drivesStr.split(":")
    for (i in 0 until parts.size - 1 step 2) {
        list.add(Pair(parts[i], parts[i + 1]))
    }
    return list
}

private fun formatDrives(list: List<Pair<String, String>>): String {
    return list.joinToString(":") { "${it.first}:${it.second}" }
}
