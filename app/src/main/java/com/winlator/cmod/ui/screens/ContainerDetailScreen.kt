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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.winlator.cmod.box64.Box64Preset
import com.winlator.cmod.container.Container
import com.winlator.cmod.container.ContainerManager
import com.winlator.cmod.contents.AdrenotoolsManager
import com.winlator.cmod.contents.ContentProfile
import com.winlator.cmod.contents.ContentsManager
import com.winlator.cmod.core.DefaultVersion
import com.winlator.cmod.core.FileUtils
import com.winlator.cmod.core.GPUInformation
import com.winlator.cmod.core.WineInfo
import com.winlator.cmod.core.WineThemeManager
import com.winlator.cmod.fexcore.FEXCorePreset
import com.winlator.cmod.midi.MidiManager
import com.winlator.cmod.winhandler.WinHandler
import com.winlator.cmod.xserver.XKeycode
import org.json.JSONArray
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

    // Dialog flags
    var showDXVKDialog by remember { mutableStateOf(false) }
    var showWineD3DDialog by remember { mutableStateOf(false) }
    var showGraphicsDriverDialog by remember { mutableStateOf(false) }

    // DXVK Versions
    val dxvkVersions = remember(isArm64EC) {
        val list = mutableListOf<String>()
        val originalItems = context.resources.getStringArray(com.winlator.cmod.R.array.dxvk_version_entries)
        list.addAll(originalItems)
        for (profile in contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_DXVK)) {
            if (profile.remoteUrl != null) continue
            val entryName = ContentsManager.getEntryName(profile)
            val firstDashIndex = entryName.indexOf('-')
            if (firstDashIndex != -1) {
                list.add(entryName.substring(firstDashIndex + 1))
            }
        }
        list.filter { version ->
            isArm64EC || !version.lowercase().contains("arm64ec")
        }
    }

    // VKD3D Versions
    val vkd3dVersions = remember(isArm64EC) {
        val list = mutableListOf<String>()
        val originalItems = context.resources.getStringArray(com.winlator.cmod.R.array.vkd3d_version_entries)
        for (version in originalItems) {
            if (isArm64EC || !version.lowercase().contains("arm64ec")) {
                list.add(version)
            }
        }
        for (profile in contentsManager.getProfiles(ContentProfile.ContentType.CONTENT_TYPE_VKD3D)) {
            if (profile.remoteUrl != null) continue
            val displayName = profile.verName
            val versionCode = profile.verCode
            if (isArm64EC || !displayName.lowercase().contains("arm64ec")) {
                list.add("$displayName-$versionCode")
            }
        }
        list
    }

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

    // Graphics Driver Versions
    val graphicsDriverVersions = remember {
        val list = mutableListOf<String>()
        val defaultVersions = context.resources.getStringArray(com.winlator.cmod.R.array.wrapper_graphics_driver_version_entries)
        for (version in defaultVersions) {
            if (GPUInformation.isDriverSupported(version, context)) {
                list.add(version)
            }
        }
        val adrenotoolsManager = AdrenotoolsManager(context)
        list.addAll(adrenotoolsManager.enumarateInstalledDrivers())
        if (list.isEmpty()) {
            list.add("System")
        }
        list
    }

    // GPU names list
    val gpuNames = remember {
        val list = mutableListOf<String>()
        list.add("Device")
        try {
            val gpuNameList = FileUtils.readString(context, "gpu_cards.json")
            val jarray = JSONArray(gpuNameList)
            for (i in 0 until jarray.length()) {
                val jobj = jarray.getJSONObject(i)
                list.add(jobj.getString("name"))
            }
        } catch (e: Exception) {}
        list
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

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                M3DropdownSelector(
                                    label = "Driver Gráfico (GPU)",
                                    options = listOf("wrapper", "turnip", "virgl", "llvmpipe"),
                                    selectedOption = graphicsDriver,
                                    onOptionSelected = { graphicsDriver = it }
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledIconButton(
                                onClick = { showGraphicsDriverDialog = true },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Configurar Driver Gráfico"
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                M3DropdownSelector(
                                    label = "DX Wrapper",
                                    options = listOf("dxvk+vkd3d", "wined3d", "virgl-wrapper"),
                                    selectedOption = dxwrapper,
                                    onOptionSelected = { dxwrapper = it }
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledIconButton(
                                onClick = {
                                    if (dxwrapper.contains("dxvk")) {
                                        showDXVKDialog = true
                                    } else {
                                        showWineD3DDialog = true
                                    }
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Configurar DX Wrapper"
                                )
                            }
                        }

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

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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

    // Advanced Dialog for DXVK configuration
    if (showDXVKDialog) {
        val dxConfig = remember { parseDxwrapperConfig(dxwrapperConfig).toMutableMap() }
        var selectedVersion by remember { mutableStateOf(dxConfig["version"] ?: DefaultVersion.DXVK) }
        var selectedVkd3dVersion by remember { mutableStateOf(dxConfig["vkd3dVersion"] ?: "None") }
        var selectedFramerate by remember { mutableStateOf(dxConfig["framerate"] ?: "0") }
        var selectedVkd3dLevel by remember { mutableStateOf(dxConfig["vkd3dLevel"] ?: "12_1") }
        var selectedDdrawrapper by remember { mutableStateOf(dxConfig["ddrawrapper"] ?: "none") }
        var asyncEnabled by remember { mutableStateOf(dxConfig["async"] == "1") }
        var asyncCacheEnabled by remember { mutableStateOf(dxConfig["asyncCache"] == "1") }

        val filteredDxvkVersions = remember(selectedVkd3dVersion, dxvkVersions) {
            if (selectedVkd3dVersion != "None") {
                dxvkVersions.filter { ver ->
                    val major = tryGetMajor(ver)
                    major == null || major >= 2
                }
            } else {
                dxvkVersions
            }
        }

        LaunchedEffect(filteredDxvkVersions) {
            if (selectedVersion !in filteredDxvkVersions) {
                val major = tryGetMajor(selectedVersion)
                if (major == null || major < 2) {
                    selectedVersion = DefaultVersion.DXVK
                }
            }
        }

        val selectedVersionLower = selectedVersion.lowercase()
        val isGplAsync = selectedVersionLower.contains("gplasync")
        val isAsync = selectedVersionLower.contains("async") && !isGplAsync
        val showAsync = isGplAsync || isAsync
        val showAsyncCache = isGplAsync

        AlertDialog(
            onDismissRequest = { showDXVKDialog = false },
            title = { Text("Configuração Avançada do DXVK") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    M3DropdownSelector(
                        label = "Versão do DXVK",
                        options = filteredDxvkVersions,
                        selectedOption = selectedVersion,
                        onOptionSelected = { selectedVersion = it }
                    )

                    M3DropdownSelector(
                        label = "Versão do VKD3D",
                        options = vkd3dVersions,
                        selectedOption = selectedVkd3dVersion,
                        onOptionSelected = { selectedVkd3dVersion = it }
                    )

                    M3DropdownSelector(
                        label = "Limite de FPS",
                        options = listOf("0", "20", "30", "40", "50", "60", "70", "80", "90", "100", "120", "144"),
                        selectedOption = selectedFramerate,
                        onOptionSelected = { selectedFramerate = it }
                    )

                    M3DropdownSelector(
                        label = "VKD3D Feature Level",
                        options = listOf("12_0", "12_1", "12_2", "11_1", "11_0", "10_1", "10_0", "9_3", "9_2", "9_1"),
                        selectedOption = selectedVkd3dLevel,
                        onOptionSelected = { selectedVkd3dLevel = it }
                    )

                    M3DropdownSelector(
                        label = "DirectDraw Wrapper",
                        options = listOf("none", "cnc", "tsgrack", "ddwrapper"),
                        selectedOption = selectedDdrawrapper,
                        onOptionSelected = { selectedDdrawrapper = it }
                    )

                    if (showAsync) {
                        SettingsSwitchRow(
                            label = "Compilação Assíncrona de Shaders (Async)",
                            checked = asyncEnabled,
                            onCheckedChange = { asyncEnabled = it }
                        )
                    }

                    if (showAsyncCache) {
                        SettingsSwitchRow(
                            label = "Cache de Shaders Assíncrono",
                            checked = asyncCacheEnabled,
                            onCheckedChange = { asyncCacheEnabled = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    dxConfig["version"] = selectedVersion
                    dxConfig["vkd3dVersion"] = selectedVkd3dVersion
                    dxConfig["framerate"] = selectedFramerate
                    dxConfig["vkd3dLevel"] = selectedVkd3dLevel
                    dxConfig["ddrawrapper"] = selectedDdrawrapper
                    dxConfig["async"] = if (showAsync && asyncEnabled) "1" else "0"
                    dxConfig["asyncCache"] = if (showAsyncCache && asyncCacheEnabled) "1" else "0"

                    dxwrapperConfig = formatDxwrapperConfig(dxConfig)
                    showDXVKDialog = false
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDXVKDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Advanced Dialog for WineD3D configuration
    if (showWineD3DDialog) {
        val w3dConfig = remember { parseDxwrapperConfig(dxwrapperConfig).toMutableMap() }
        val wineD3DGpuNames = remember(gpuNames) { gpuNames.filter { it != "Device" } }

        AlertDialog(
            onDismissRequest = { showWineD3DDialog = false },
            title = { Text("Configuração Avançada do WineD3D") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    var csmtEnabled by remember { mutableStateOf(w3dConfig["csmt"] == "3") }
                    SettingsSwitchRow(
                        label = "CSMT (Multi-threaded Command Stream)",
                        checked = csmtEnabled,
                        onCheckedChange = {
                            csmtEnabled = it
                            w3dConfig["csmt"] = if (it) "3" else "0"
                        }
                    )

                    var strictShaderMath by remember { mutableStateOf(w3dConfig["strict_shader_math"] == "1") }
                    SettingsSwitchRow(
                        label = "Strict Shader Math",
                        checked = strictShaderMath,
                        onCheckedChange = {
                            strictShaderMath = it
                            w3dConfig["strict_shader_math"] = if (it) "1" else "0"
                        }
                    )

                    var offscreenMode by remember { mutableStateOf(w3dConfig["OffscreenRenderingMode"] ?: "fbo") }
                    M3DropdownSelector(
                        label = "Offscreen Rendering Mode",
                        options = listOf("fbo", "backbuffer"),
                        selectedOption = offscreenMode,
                        onOptionSelected = {
                            offscreenMode = it
                            w3dConfig["OffscreenRenderingMode"] = it
                        }
                    )

                    var selectedGpuName by remember { mutableStateOf(w3dConfig["gpuName"] ?: "NVIDIA GeForce GTX 480") }
                    M3DropdownSelector(
                        label = "Nome da GPU",
                        options = wineD3DGpuNames,
                        selectedOption = selectedGpuName,
                        onOptionSelected = {
                            selectedGpuName = it
                            w3dConfig["gpuName"] = it
                        }
                    )

                    var selectedVram by remember { mutableStateOf(w3dConfig["videoMemorySize"] ?: "2048") }
                    M3DropdownSelector(
                        label = "Tamanho da Memória de Vídeo (VRAM)",
                        options = listOf("32", "64", "128", "256", "512", "1024", "2048", "4096"),
                        selectedOption = selectedVram,
                        onOptionSelected = {
                            selectedVram = it
                            w3dConfig["videoMemorySize"] = it
                        }
                    )

                    var selectedRenderer by remember { mutableStateOf(w3dConfig["renderer"] ?: "gl") }
                    M3DropdownSelector(
                        label = "Renderizador",
                        options = listOf("gl", "vulkan", "gdi"),
                        selectedOption = selectedRenderer,
                        onOptionSelected = {
                            selectedRenderer = it
                            w3dConfig["renderer"] = it
                        }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    dxwrapperConfig = formatDxwrapperConfig(w3dConfig)
                    showWineD3DDialog = false
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWineD3DDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Advanced Dialog for Graphics Driver configuration
    if (showGraphicsDriverDialog) {
        val gdConfig = remember { parseGraphicsDriverConfig(graphicsDriverConfig).toMutableMap() }
        var selectedVersion by remember { mutableStateOf(gdConfig["version"] ?: "System") }
        val availableExtensions = remember(selectedVersion) {
            GPUInformation.enumerateExtensions(selectedVersion, context) ?: emptyArray()
        }
        val blacklistedList = remember(selectedVersion) {
            val bl = if (selectedVersion == (parseGraphicsDriverConfig(graphicsDriverConfig)["version"] ?: "System")) {
                parseGraphicsDriverConfig(graphicsDriverConfig)["blacklistedExtensions"] ?: ""
            } else {
                ""
            }
            bl.split(",").filter { it.isNotEmpty() }.toMutableStateList()
        }

        var selectedVulkanVersion by remember { mutableStateOf(gdConfig["vulkanVersion"] ?: "1.3") }
        var selectedGpuName by remember { mutableStateOf(gdConfig["gpuName"] ?: "Device") }
        var selectedMemory by remember { mutableStateOf(gdConfig["maxDeviceMemory"] ?: "0") }
        var selectedPresentMode by remember { mutableStateOf(gdConfig["presentMode"] ?: "mailbox") }
        var selectedResourceType by remember { mutableStateOf(gdConfig["resourceType"] ?: "auto") }
        var selectedBCn by remember { mutableStateOf(gdConfig["bcnEmulation"] ?: "auto") }
        var selectedBCnType by remember { mutableStateOf(gdConfig["bcnEmulationType"] ?: "compute") }
        var bcnEmulationCacheEnabled by remember { mutableStateOf(gdConfig["bcnEmulationCache"] == "1") }
        var syncFrameEnabled by remember { mutableStateOf(gdConfig["syncFrame"] == "1") }
        var disablePresentWaitEnabled by remember { mutableStateOf(gdConfig["disablePresentWait"] == "1") }

        AlertDialog(
            onDismissRequest = { showGraphicsDriverDialog = false },
            title = { Text("Configuração Avançada do Driver Gráfico") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    M3DropdownSelector(
                        label = "Versão do Driver",
                        options = graphicsDriverVersions,
                        selectedOption = selectedVersion,
                        onOptionSelected = { selectedVersion = it }
                    )

                    M3DropdownSelector(
                        label = "Versão do Vulkan",
                        options = listOf("1.1", "1.2", "1.3"),
                        selectedOption = selectedVulkanVersion,
                        onOptionSelected = { selectedVulkanVersion = it }
                    )

                    M3DropdownSelector(
                        label = "Nome da GPU Emulada",
                        options = gpuNames,
                        selectedOption = selectedGpuName,
                        onOptionSelected = { selectedGpuName = it }
                    )

                    M3DropdownSelector(
                        label = "Memória de Vídeo Máxima (VRAM)",
                        options = listOf("0 (Default)", "512 MB", "1024 MB", "2048 MB", "4096 MB", "8192 MB", "12288 MB", "16384 MB"),
                        selectedOption = if (selectedMemory == "0") "0 (Default)" else "$selectedMemory MB",
                        onOptionSelected = { selectedMemory = it.replace(" MB", "").replace(" (Default)", "") }
                    )

                    M3DropdownSelector(
                        label = "Modo de Apresentação (Vsync)",
                        options = listOf("mailbox", "fifo", "immediate", "relaxed"),
                        selectedOption = selectedPresentMode,
                        onOptionSelected = { selectedPresentMode = it }
                    )

                    M3DropdownSelector(
                        label = "Tipo de Recurso",
                        options = listOf("auto", "dmabuf", "ahb", "opaque"),
                        selectedOption = selectedResourceType,
                        onOptionSelected = { selectedResourceType = it }
                    )

                    M3DropdownSelector(
                        label = "Emulação de BCn",
                        options = listOf("none", "partial", "full", "auto"),
                        selectedOption = selectedBCn,
                        onOptionSelected = { selectedBCn = it }
                    )

                    M3DropdownSelector(
                        label = "Tipo de Emulação de BCn",
                        options = listOf("software", "compute"),
                        selectedOption = selectedBCnType,
                        onOptionSelected = { selectedBCnType = it }
                    )

                    SettingsSwitchRow(
                        label = "Cache de Emulação de BCn",
                        checked = bcnEmulationCacheEnabled,
                        onCheckedChange = { bcnEmulationCacheEnabled = it }
                    )

                    SettingsSwitchRow(
                        label = "Forçar Sync Frame (syncFrame)",
                        checked = syncFrameEnabled,
                        onCheckedChange = { syncFrameEnabled = it }
                    )

                    SettingsSwitchRow(
                        label = "Desabilitar Present Wait",
                        checked = disablePresentWaitEnabled,
                        onCheckedChange = { disablePresentWaitEnabled = it }
                    )

                    if (availableExtensions.isNotEmpty()) {
                        Text("Blacklist de Extensões Vulkan", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                        Text("Extensões marcadas serão desativadas:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        availableExtensions.forEach { ext ->
                            val isBlacklisted = blacklistedList.contains(ext)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = isBlacklisted,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            blacklistedList.add(ext)
                                        } else {
                                            blacklistedList.remove(ext)
                                        }
                                    }
                                )
                                Text(ext, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    gdConfig["version"] = selectedVersion
                    gdConfig["vulkanVersion"] = selectedVulkanVersion
                    gdConfig["gpuName"] = selectedGpuName
                    gdConfig["maxDeviceMemory"] = selectedMemory
                    gdConfig["presentMode"] = selectedPresentMode
                    gdConfig["resourceType"] = selectedResourceType
                    gdConfig["bcnEmulation"] = selectedBCn
                    gdConfig["bcnEmulationType"] = selectedBCnType
                    gdConfig["bcnEmulationCache"] = if (bcnEmulationCacheEnabled) "1" else "0"
                    gdConfig["syncFrame"] = if (syncFrameEnabled) "1" else "0"
                    gdConfig["disablePresentWait"] = if (disablePresentWaitEnabled) "1" else "0"
                    gdConfig["blacklistedExtensions"] = blacklistedList.joinToString(",")

                    graphicsDriverConfig = formatGraphicsDriverConfig(gdConfig)
                    showGraphicsDriverDialog = false
                }) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGraphicsDriverDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
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

// Helper methods for parsing wincomponents
private fun parseWinComponents(winCompStr: String): Map<String, Int> {
    val map = mutableMapOf<String, Int>()
    winCompStr.split(";").forEach {
        val parts = it.split("=")
        if (parts.size == 2) {
            map[parts[0]] = parts[1].toIntOrNull() ?: 1
        }
    }
    return map
}

private fun formatWinComponents(map: Map<String, Int>): String {
    return map.entries.joinToString(";") { "${it.key}=${it.value}" }
}

// Helper methods for parsing drives
private fun parseDrives(drivesStr: String): List<Pair<String, String>> {
    val list = mutableListOf<Pair<String, String>>()
    val parts = drivesStr.split(":")
    for (i in 0 until parts.size - 1 step 2) {
        list.add(Pair(parts[i], parts[i + 1]))
    }
    return list
}

private fun formatDrives(list: List<Pair<String, String>>): String {
    return list.joinToString(":") { "${it.first}:${it.second}" }
}

// Helper methods for parsing dxwrapperConfig
private fun parseDxwrapperConfig(configStr: String): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val defaultVal = Container.DEFAULT_DXWRAPPERCONFIG
    val data = if (configStr.isNotEmpty()) configStr else defaultVal
    data.split(",").forEach {
        val parts = it.split("=")
        if (parts.size == 2) {
            map[parts[0]] = parts[1]
        }
    }
    return map
}

private fun formatDxwrapperConfig(map: Map<String, String>): String {
    return map.entries.joinToString(",") { "${it.key}=${it.value}" }
}

// Helper methods for parsing graphicsDriverConfig
private fun parseGraphicsDriverConfig(configStr: String): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val defaultVal = Container.DEFAULT_GRAPHICSDRIVERCONFIG
    val data = if (configStr.isNotEmpty()) configStr else defaultVal
    data.split(";").forEach {
        val parts = it.split("=")
        if (parts.size == 2) {
            map[parts[0]] = parts[1]
        } else if (parts.size == 1 && parts[0].isNotEmpty()) {
            map[parts[0]] = ""
        }
    }
    return map
}

private fun formatGraphicsDriverConfig(map: Map<String, String>): String {
    return map.entries.joinToString(";") { "${it.key}=${it.value}" }
}

private val SEMVER = java.util.regex.Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?")
private fun tryGetMajor(s: String?): Int? {
    if (s == null) return null
    val m = SEMVER.matcher(s)
    if (!m.find()) return null
    return try {
        m.group(1).toInt()
    } catch (e: NumberFormatException) {
        null
    }
}
