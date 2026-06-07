package com.winlator.cmod.ui.screens

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.winlator.cmod.box64.Box64Preset
import com.winlator.cmod.container.Container
import com.winlator.cmod.container.ContainerManager
import com.winlator.cmod.contents.ContentProfile
import com.winlator.cmod.contents.ContentsManager
import com.winlator.cmod.core.DefaultVersion
import com.winlator.cmod.core.WineInfo
import com.winlator.cmod.core.WineThemeManager
import com.winlator.cmod.fexcore.FEXCorePreset
import com.winlator.cmod.midi.MidiManager
import com.winlator.cmod.winhandler.WinHandler
import com.winlator.cmod.xserver.XKeycode
import org.json.JSONObject
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerDetailScreen(
    container: Container?,
    containerManager: ContainerManager,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val context = containerManager.context
    val contentsManager = remember {
        ContentsManager(context).apply { syncContents() }
    }

    // 1. Wine Versions List
    val wineVersions = remember {
        val list = mutableListOf<String>()
        val staticVersions = context.resources.getStringArray(com.winlator.cmod.R.array.wine_entries)
        list.addAll(staticVersions)
        for (profile in contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_WINE)) {
            if (profile.remoteUrl != null) continue
            list.add(ContentsManager.getEntryName(profile))
        }
        for (profile in contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_PROTON)) {
            if (profile.remoteUrl != null) continue
            list.add(ContentsManager.getEntryName(profile))
        }
        if (list.isEmpty()) {
            list.add("proton-9.0-x86_64")
        }
        list
    }

    // 2. States for Fields
    var name by remember { mutableStateOf(container?.getName() ?: "Container-${containerManager.getNextContainerId()}") }
    var wineVersion by remember { mutableStateOf(container?.getWineVersion() ?: wineVersions.first()) }
    var screenSize by remember { mutableStateOf(container?.getScreenSize() ?: Container.DEFAULT_SCREEN_SIZE) }
    var graphicsDriver by remember { mutableStateOf(container?.getGraphicsDriver() ?: Container.DEFAULT_GRAPHICS_DRIVER) }
    var graphicsDriverConfig by remember { mutableStateOf(container?.getGraphicsDriverConfig() ?: Container.DEFAULT_GRAPHICSDRIVERCONFIG) }
    var dxwrapper by remember { mutableStateOf(container?.getDXWrapper() ?: Container.DEFAULT_DXWRAPPER) }
    var dxwrapperConfig by remember { mutableStateOf(container?.getDXWrapperConfig() ?: Container.DEFAULT_DXWRAPPERCONFIG) }
    var audioDriver by remember { mutableStateOf(container?.getAudioDriver() ?: Container.DEFAULT_AUDIO_DRIVER) }
    var envVars by remember { mutableStateOf(container?.getEnvVars() ?: Container.DEFAULT_ENV_VARS) }
    var showFPS by remember { mutableStateOf(container?.isShowFPS() ?: true) }
    var fullscreenStretched by remember { mutableStateOf(container?.isFullscreenStretched() ?: false) }
    var exclusiveXInput by remember { mutableStateOf(container?.isExclusiveXInput() ?: true) }
    
    val inputType = container?.getInputType() ?: WinHandler.DEFAULT_INPUT_TYPE.toInt()
    var enableXInput by remember { mutableStateOf((inputType and WinHandler.FLAG_INPUT_TYPE_XINPUT.toInt()) == WinHandler.FLAG_INPUT_TYPE_XINPUT.toInt()) }
    var enableDInput by remember { mutableStateOf((inputType and WinHandler.FLAG_INPUT_TYPE_DINPUT.toInt()) == WinHandler.FLAG_INPUT_TYPE_DINPUT.toInt()) }
    var lc_all by remember { mutableStateOf(container?.getLC_ALL() ?: "pt_BR.UTF-8") }

    // Advanced & Emulator settings
    val wi = remember(wineVersion) { WineInfo.fromIdentifier(context, contentsManager, wineVersion) }
    val isArm64EC = wi.isArm64EC()

    var emulator by remember { mutableStateOf(container?.getEmulator() ?: if (isArm64EC) "FEXCore" else "Box64") }
    var startupSelection by remember { mutableStateOf(container?.getStartupSelection()?.toInt() ?: Container.STARTUP_SELECTION_ESSENTIAL.toInt()) }
    var desktopTheme by remember { mutableStateOf(container?.getDesktopTheme() ?: WineThemeManager.DEFAULT_DESKTOP_THEME) }
    var midiSoundFont by remember { mutableStateOf(container?.getMIDISoundFont() ?: "") }

    // CPU Affinity Cores Setup
    val numCores = remember { Runtime.getRuntime().availableProcessors().coerceAtLeast(8) }
    val initialCpuList = remember {
        val rawList = if (container != null) container.getCPUList(true) else Container.getFallbackCPUList()
        parseCpuList(rawList, numCores)
    }
    val cpuCoresState = remember { mutableStateListOf<Boolean>().apply { addAll(initialCpuList) } }

    val initialCpuListWoW64 = remember {
        val rawList = if (container != null) container.getCPUListWoW64(true) else Container.getFallbackCPUListWoW64()
        parseCpuList(rawList, numCores)
    }
    val cpuCoresWoW64State = remember { mutableStateListOf<Boolean>().apply { addAll(initialCpuListWoW64) } }

    // Presets & Versions of emulators
    var box64Preset by remember { mutableStateOf(container?.getBox64Preset() ?: Box64Preset.COMPATIBILITY) }
    
    // Dynamic Box64 versions
    val box64Versions = remember(isArm64EC) {
        val set = mutableSetOf<String>()
        set.add(if (isArm64EC) DefaultVersion.WOWBOX64 else DefaultVersion.BOX64)
        if (container != null && container.getBox64Version() != null && container.getBox64Version().isNotEmpty()) {
            set.add(container.getBox64Version())
        }
        val type = if (isArm64EC) ContentProfile.ContentType.CONTENT_TYPE_WOWBOX64 else ContentProfile.ContentType.CONTENT_TYPE_BOX64
        for (profile in contentsManager.getProfiles(type)) {
            if (profile.remoteUrl != null) continue
            val entryName = ContentsManager.getEntryName(profile)
            val dashIdx = entryName.indexOf('-')
            if (dashIdx != -1) {
                set.add(entryName.substring(dashIdx + 1))
            }
        }
        set.toList()
    }
    var box64Version by remember(box64Versions) { mutableStateOf(container?.getBox64Version() ?: box64Versions.first()) }

    var fexcorePreset by remember { mutableStateOf(container?.getFEXCorePreset() ?: FEXCorePreset.INTERMEDIATE) }

    // Dynamic FEXCore versions
    val fexcoreVersions = remember {
        val set = mutableSetOf<String>()
        set.add(DefaultVersion.FEXCORE)
        if (container != null && container.getFEXCoreVersion() != null && container.getFEXCoreVersion().isNotEmpty()) {
            set.add(container.getFEXCoreVersion())
        }
        for (profile in contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_FEXCORE)) {
            if (profile.remoteUrl != null) continue
            val entryName = ContentsManager.getEntryName(profile)
            val dashIdx = entryName.indexOf('-')
            if (dashIdx != -1) {
                set.add(entryName.substring(dashIdx + 1))
            }
        }
        set.toList()
    }
    var fexcoreVersion by remember(fexcoreVersions) { mutableStateOf(container?.getFEXCoreVersion() ?: fexcoreVersions.first()) }

    // WinComponents Configuration
    val initialWinComponents = remember { container?.getWinComponents() ?: Container.DEFAULT_WINCOMPONENTS }
    val winComponentsMap = remember {
        val map = parseWinComponents(initialWinComponents)
        mutableStateMapOf<String, Int>().apply { putAll(map) }
    }

    // Mapped Drives Configuration
    val initialDrives = remember { container?.getDrives() ?: Container.DEFAULT_DRIVES }
    val drivesList = remember {
        val parsed = parseDrives(initialDrives)
        mutableStateListOf<Pair<String, String>>().apply { addAll(parsed) }
    }

    // XR Controls Mapping Configuration
    var primaryController by remember { mutableStateOf(container?.getPrimaryController() ?: 1) }
    val defaultKeys = remember {
        mapOf(
            Container.XrControllerMapping.BUTTON_A to XKeycode.KEY_A,
            Container.XrControllerMapping.BUTTON_B to XKeycode.KEY_B,
            Container.XrControllerMapping.BUTTON_X to XKeycode.KEY_X,
            Container.XrControllerMapping.BUTTON_Y to XKeycode.KEY_Y,
            Container.XrControllerMapping.BUTTON_GRIP to XKeycode.KEY_SPACE,
            Container.XrControllerMapping.BUTTON_TRIGGER to XKeycode.KEY_ENTER,
            Container.XrControllerMapping.THUMBSTICK_UP to XKeycode.KEY_UP,
            Container.XrControllerMapping.THUMBSTICK_DOWN to XKeycode.KEY_DOWN,
            Container.XrControllerMapping.THUMBSTICK_LEFT to XKeycode.KEY_LEFT,
            Container.XrControllerMapping.THUMBSTICK_RIGHT to XKeycode.KEY_RIGHT
        )
    }
    val selectedMappings = remember {
        val map = mutableStateMapOf<Container.XrControllerMapping, XKeycode>()
        Container.XrControllerMapping.values().forEach { mapping ->
            val defaultVal = defaultKeys[mapping] ?: XKeycode.KEY_NONE
            val keycodeId = if (container != null) container.getControllerMapping(mapping) else defaultVal.id
            val keycode = XKeycode.values().find { it.id == keycodeId } ?: defaultVal
            map[mapping] = keycode
        }
        map
    }

    // UI Tab Navigation
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Geral",
        "Avançado & CPU",
        "Componentes do Wine",
        "Variáveis",
        "Drives",
        "XR Controles"
    )

    // SoundFonts setup
    val soundFontOptions = remember {
        val list = mutableListOf<String>()
        list.add("-- Desativado --")
        list.add(MidiManager.DEFAULT_SF2_FILE)
        val sfDir = MidiManager.getSoundFontDir(context)
        if (sfDir.exists()) {
            val files = sfDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isFile && file.name.endsWith(".sf2", ignoreCase = true)) {
                        list.add(file.name)
                    }
                }
            }
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 3. Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (container == null) "Criar Container" else "Editar Container",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = {
                    val data = JSONObject()
                    data.put("name", name)
                    data.put("screenSize", screenSize)
                    data.put("graphicsDriver", graphicsDriver)
                    data.put("graphicsDriverConfig", graphicsDriverConfig)
                    data.put("dxwrapper", dxwrapper)
                    data.put("dxwrapperConfig", dxwrapperConfig)
                    data.put("audioDriver", audioDriver)
                    data.put("showFPS", showFPS)
                    data.put("fullscreenStretched", fullscreenStretched)
                    data.put("exclusiveXInput", exclusiveXInput)

                    var finalInputType = 0
                    if (enableXInput) finalInputType = finalInputType or WinHandler.FLAG_INPUT_TYPE_XINPUT.toInt()
                    if (enableDInput) finalInputType = finalInputType or WinHandler.FLAG_INPUT_TYPE_DINPUT.toInt()
                    data.put("inputType", finalInputType)

                    data.put("lc_all", lc_all)
                    data.put("emulator", emulator)
                    data.put("box64Version", box64Version)
                    data.put("box64Preset", box64Preset)
                    data.put("fexcoreVersion", fexcoreVersion)
                    data.put("fexcorePreset", fexcorePreset)
                    data.put("desktopTheme", desktopTheme)
                    data.put("startupSelection", startupSelection)
                    data.put("midiSoundFont", if (midiSoundFont == "-- Desativado --") "" else midiSoundFont)
                    data.put("primaryController", primaryController)

                    // Format Controller Mapping
                    val charArray = CharArray(Container.XrControllerMapping.values().size)
                    Container.XrControllerMapping.values().forEach { mapping ->
                        val keycode = selectedMappings[mapping] ?: defaultKeys[mapping] ?: XKeycode.KEY_NONE
                        charArray[mapping.ordinal] = keycode.id.toInt().toChar()
                    }
                    data.put("controllerMapping", String(charArray))

                    // Format CPU list
                    val formattedCpuList = formatCpuList(cpuCoresState)
                    val formattedCpuListWoW64 = formatCpuList(cpuCoresWoW64State)
                    data.put("cpuList", formattedCpuList)
                    data.put("cpuListWoW64", formattedCpuListWoW64)

                    // Format Wine Components & Drives
                    data.put("wincomponents", formatWinComponents(winComponentsMap))
                    data.put("drives", formatDrives(drivesList))
                    data.put("envVars", envVars)
                    data.put("wineVersion", wineVersion)

                    if (container == null) {
                        containerManager.createContainerAsync(data, contentsManager) {
                            onSave()
                        }
                    } else {
                        container.setName(name)
                        container.setScreenSize(screenSize)
                        container.setGraphicsDriver(graphicsDriver)
                        container.setGraphicsDriverConfig(graphicsDriverConfig)
                        container.setDXWrapper(dxwrapper)
                        container.setDXWrapperConfig(dxwrapperConfig)
                        container.setAudioDriver(audioDriver)
                        container.setShowFPS(showFPS)
                        container.setFullscreenStretched(fullscreenStretched)
                        container.setExclusiveXInput(exclusiveXInput)
                        container.setInputType(finalInputType)
                        container.setLC_ALL(lc_all)
                        container.setEmulator(emulator)
                        container.setBox64Version(box64Version)
                        container.setBox64Preset(box64Preset)
                        container.setFEXCoreVersion(fexcoreVersion)
                        container.setFEXCorePreset(fexcorePreset)
                        container.setDesktopTheme(desktopTheme)
                        container.setStartupSelection(startupSelection.toByte())
                        container.setMidiSoundFont(if (midiSoundFont == "-- Desativado --") "" else midiSoundFont)
                        container.setPrimaryController(primaryController)
                        container.setControllerMapping(String(charArray))
                        container.setCPUList(formattedCpuList)
                        container.setCPUListWoW64(formattedCpuListWoW64)
                        container.setWinComponents(formatWinComponents(winComponentsMap))
                        container.setDrives(formatDrives(drivesList))
                        container.setEnvVars(envVars)
                        container.setWineVersion(wineVersion)
                        container.saveData()
                        onSave()
                    }
                }) {
                    Text("Salvar")
                }
            }
        }

        // 4. M3 Navigation Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 16.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant)

        // 5. Tab Contents
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (selectedTab) {
                0 -> { // Geral
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nome do Container") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        M3DropdownSelector(
                            label = "Versão do Wine",
                            options = wineVersions,
                            selectedOption = wineVersion,
                            onOptionSelected = { wineVersion = it }
                        )

                        M3DropdownSelector(
                            label = "Resolução da Tela",
                            options = listOf("800x600", "1024x768", "1280x720", "1600x900", "1920x1080"),
                            selectedOption = screenSize,
                            onOptionSelected = { screenSize = it }
                        )

                        M3DropdownSelector(
                            label = "Driver Gráfico (GPU)",
                            options = listOf("wrapper", "turnip", "virgl", "llvmpipe"),
                            selectedOption = graphicsDriver,
                            onOptionSelected = { graphicsDriver = it }
                        )

                        M3DropdownSelector(
                            label = "DX Wrapper",
                            options = listOf("dxvk+vkd3d", "wined3d", "virgl-wrapper"),
                            selectedOption = dxwrapper,
                            onOptionSelected = { dxwrapper = it }
                        )

                        M3DropdownSelector(
                            label = "Driver de Áudio",
                            options = listOf("alsa", "pulse"),
                            selectedOption = audioDriver,
                            onOptionSelected = { audioDriver = it }
                        )

                        OutlinedTextField(
                            value = lc_all,
                            onValueChange = { lc_all = it },
                            label = { Text("Idioma do Sistema (LC_ALL)") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedCard(
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Ajustes & Controles", style = MaterialTheme.typography.titleMedium)
                                
                                SettingsSwitchRow(
                                    label = "Mostrar Medidor de FPS",
                                    checked = showFPS,
                                    onCheckedChange = { showFPS = it }
                                )

                                SettingsSwitchRow(
                                    label = "Tela Cheia Esticada",
                                    checked = fullscreenStretched,
                                    onCheckedChange = { fullscreenStretched = it }
                                )

                                SettingsSwitchRow(
                                    label = "XInput Habilitado",
                                    checked = enableXInput,
                                    onCheckedChange = { 
                                        enableXInput = it 
                                        if (exclusiveXInput && it && enableDInput) enableDInput = false
                                    }
                                )

                                SettingsSwitchRow(
                                    label = "DInput Habilitado",
                                    checked = enableDInput,
                                    onCheckedChange = { 
                                        enableDInput = it 
                                        if (exclusiveXInput && it && enableXInput) enableXInput = false
                                    }
                                )

                                SettingsSwitchRow(
                                    label = "XInput Exclusivo",
                                    checked = exclusiveXInput,
                                    onCheckedChange = { 
                                        exclusiveXInput = it 
                                        if (it) {
                                            if (enableXInput && enableDInput) enableDInput = false
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> { // Avançado & CPU
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        M3DropdownSelector(
                            label = "Emulador de CPU",
                            options = if (isArm64EC) listOf("Box64", "FEXCore") else listOf("Box64"),
                            selectedOption = emulator,
                            onOptionSelected = { emulator = it }
                        )

                        M3DropdownSelector(
                            label = "Startup Selection (Seleção de Inicialização)",
                            options = listOf("Normal", "Essencial", "Agressiva"),
                            selectedOption = when (startupSelection) {
                                0 -> "Normal"
                                1 -> "Essencial"
                                else -> "Agressiva"
                            },
                            onOptionSelected = { selected ->
                                startupSelection = when (selected) {
                                    "Normal" -> 0
                                    "Essencial" -> 1
                                    else -> 2
                                }
                            }
                        )

                        M3DropdownSelector(
                            label = "Tema do Desktop",
                            options = listOf("Light", "Dark"),
                            selectedOption = desktopTheme.replaceFirstChar { it.uppercase() },
                            onOptionSelected = { desktopTheme = it.lowercase() }
                        )

                        M3DropdownSelector(
                            label = "Música/Fonte de Som MIDI (Midi SoundFont)",
                            options = soundFontOptions,
                            selectedOption = if (midiSoundFont.isEmpty()) "-- Desativado --" else midiSoundFont,
                            onOptionSelected = { midiSoundFont = it }
                        )

                        if (emulator == "Box64") {
                            M3DropdownSelector(
                                label = "Box64 Preset",
                                options = listOf("STABILITY", "COMPATIBILITY", "INTERMEDIATE", "PERFORMANCE", "CUSTOM"),
                                selectedOption = box64Preset,
                                onOptionSelected = { box64Preset = it }
                            )

                            M3DropdownSelector(
                                label = "Box64 Versão",
                                options = box64Versions,
                                selectedOption = box64Version,
                                onOptionSelected = { box64Version = it }
                            )
                        } else {
                            M3DropdownSelector(
                                label = "FEXCore Preset",
                                options = listOf("STABILITY", "COMPATIBILITY", "INTERMEDIATE", "PERFORMANCE", "CUSTOM"),
                                selectedOption = fexcorePreset,
                                onOptionSelected = { fexcorePreset = it }
                            )

                            M3DropdownSelector(
                                label = "FEXCore Versão",
                                options = fexcoreVersions,
                                selectedOption = fexcoreVersion,
                                onOptionSelected = { fexcoreVersion = it }
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        OutlinedCard(
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Afinidade de Núcleos CPU (Normal)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
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
                                                        onCheckedChange = { cpuCoresState[coreIdx] = it }
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

                        OutlinedCard(
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Afinidade de Núcleos CPU (WoW64)",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
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
                                                        onCheckedChange = { cpuCoresWoW64State[coreIdx] = it }
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
                }
                2 -> { // Componentes do Wine
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Ajustes de Componentes do Windows (Wine)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Escolha entre Builtin (Wine integrado) ou Native (Código nativo do Windows). Algumas DLLs nativas corrigem problemas específicos de áudio ou gráficos em certos jogos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        winComponentsMap.keys.sorted().forEach { component ->
                            val value = winComponentsMap[component] ?: 0
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth()
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
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (value == 1) "Native" else "Builtin",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (value == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = value == 1,
                                            onCheckedChange = { checked ->
                                                winComponentsMap[component] = if (checked) 1 else 0
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> { // Variáveis
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Variáveis de Ambiente",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Adicione chaves de configuração personalizadas separadas por espaço (Ex: KEY=value KEY2=value2) para alterar o comportamento da engine de emulação.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = envVars,
                            onValueChange = { envVars = it },
                            label = { Text("Configurações das Variáveis de Ambiente") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 8,
                            maxLines = 15,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
                4 -> { // Drives
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Drives Mapeados",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            IconButton(onClick = {
                                // Add a new custom drive
                                val letters = ('E'..'Z').map { it.toString() }
                                val used = drivesList.map { it.first }
                                val available = letters.filter { !used.contains(it) }
                                if (available.isNotEmpty()) {
                                    drivesList.add(available.first() to "/storage/emulated/0")
                                }
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Adicionar Drive")
                            }
                        }

                        drivesList.forEachIndexed { index, drive ->
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Drive ${drive.first}:",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (drive.first != "C" && drive.first != "D") {
                                            IconButton(onClick = {
                                                drivesList.removeAt(index)
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Remover Drive")
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = drive.second,
                                        onValueChange = { newPath ->
                                            drivesList[index] = drive.first to newPath
                                        },
                                        label = { Text("Caminho do Diretório") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
                5 -> { // XR / Controles
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Configurações de Controles XR",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        M3DropdownSelector(
                            label = "Controle Primário",
                            options = listOf("Controle 1", "Controle 2"),
                            selectedOption = "Controle $primaryController",
                            onOptionSelected = {
                                primaryController = if (it == "Controle 1") 1 else 2
                            }
                        )

                        Divider(color = MaterialTheme.colorScheme.outlineVariant)

                        Text(
                            text = "Mapeamento dos Botões VR/XR",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Container.XrControllerMapping.values().forEach { mapping ->
                            val selectedKey = selectedMappings[mapping] ?: XKeycode.KEY_NONE
                            
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = mapping.name.replace("_", " "),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    M3DropdownSelector(
                                        label = "Mapear para Tecla",
                                        options = XKeycode.values().map { it.name },
                                        selectedOption = selectedKey.name,
                                        onOptionSelected = { newKeyName ->
                                            val keycode = XKeycode.values().find { it.name == newKeyName } ?: XKeycode.KEY_NONE
                                            selectedMappings[mapping] = keycode
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helpers
@Composable
fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3DropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = selectedOption,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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

// Helper methods for parsing CPU lists
private fun parseCpuList(cpuListStr: String?, numCores: Int): List<Boolean> {
    val activeList = MutableList(numCores) { true } // default all active
    if (!cpuListStr.isNullOrEmpty()) {
        activeList.fill(false)
        cpuListStr.split(",").forEach {
            val idx = it.toIntOrNull()
            if (idx != null && idx in 0 until numCores) {
                activeList[idx] = true
            }
        }
    }
    return activeList
}

private fun formatCpuList(cpuCoresList: List<Boolean>): String {
    return cpuCoresList.mapIndexedNotNull { idx, active -> if (active) idx.toString() else null }.joinToString(",")
}

// Helper methods for parsing wincomponents
private fun parseWinComponents(winComponentsStr: String): Map<String, Int> {
    val map = mutableMapOf<String, Int>()
    if (winComponentsStr.isNotEmpty()) {
        winComponentsStr.split(",").forEach {
            val parts = it.split("=")
            if (parts.size == 2) {
                val value = parts[1].toIntOrNull() ?: 0
                map[parts[0]] = value
            }
        }
    }
    return map
}

private fun formatWinComponents(map: Map<String, Int>): String {
    return map.entries.joinToString(",") { "${it.key}=${it.value}" }
}

// Helper methods for parsing drives
private fun parseDrives(drivesStr: String): List<Pair<String, String>> {
    val list = mutableListOf<Pair<String, String>>()
    var i = drivesStr.indexOf(":")
    while (i != -1) {
        val letter = drivesStr[i - 1].toString()
        val nextColon = drivesStr.indexOf(":", i + 1)
        val path = if (nextColon != -1) {
            drivesStr.substring(i + 1, nextColon - 1)
        } else {
            drivesStr.substring(i + 1)
        }
        list.add(letter to path)
        i = nextColon
    }
    return list
}

private fun formatDrives(list: List<Pair<String, String>>): String {
    return list.joinToString("") { "${it.first}:${it.second}" }
}
