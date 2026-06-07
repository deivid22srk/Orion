package com.winlator.cmod.ui

import android.content.Context
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.winlator.cmod.R
import com.winlator.cmod.XServerDisplayActivity
import com.winlator.cmod.core.AppUtils
import com.winlator.cmod.core.ProcessHelper
import com.winlator.cmod.widget.WinlatorHUD
import com.winlator.cmod.winhandler.TaskManagerDialog
import com.winlator.cmod.ui.theme.OrionTheme

object SidebarComposeHelper {
    @JvmStatic
    fun init(composeView: ComposeView, activity: XServerDisplayActivity) {
        composeView.setContent {
            OrionTheme {
                SidebarView(activity = activity)
            }
        }
    }
}

@Composable
fun SidebarView(activity: XServerDisplayActivity) {
    val context = LocalContext.current

    // Sidebar states
    var isPaused by remember { mutableStateOf(activity.isPaused) }
    var relativeMouse by remember { mutableStateOf(activity.isRelativeMouseMovement) }
    var disableMouse by remember { mutableStateOf(activity.isMouseDisabled) }

    // Section visibility states
    var inputExpanded by remember { mutableStateOf(false) }
    var mouseExpanded by remember { mutableStateOf(false) }
    var hudExpanded by remember { mutableStateOf(false) }
    var graphicsExpanded by remember { mutableStateOf(false) }
    var screenExpanded by remember { mutableStateOf(false) }

    // HUD settings states
    val hudPrefs = remember { context.getSharedPreferences("winlator_hud", Context.MODE_PRIVATE) }
    var hudEnabled by remember { mutableStateOf(activity.frameRating?.visibility == View.VISIBLE) }
    
    val initialMask = hudPrefs.getInt("hud_show", 0x6F)
    var cbFpsChecked by remember { mutableStateOf((initialMask and WinlatorHUD.SHOW_FPS) != 0) }
    var cbGpuChecked by remember { mutableStateOf((initialMask and WinlatorHUD.SHOW_GPU) != 0) }
    var cbCpuChecked by remember { mutableStateOf((initialMask and WinlatorHUD.SHOW_CPU) != 0) }
    var cbRamChecked by remember { mutableStateOf((initialMask and WinlatorHUD.SHOW_RAM) != 0) }
    var cbBattChecked by remember { mutableStateOf((initialMask and WinlatorHUD.SHOW_BATT) != 0) }
    var cbGraphChecked by remember { mutableStateOf((initialMask and WinlatorHUD.SHOW_GRAPH) != 0) }
    var cbRendererChecked by remember { mutableStateOf((initialMask and WinlatorHUD.SHOW_RENDERER) != 0) }

    var hudScale by remember { mutableStateOf(hudPrefs.getFloat("hud_scale", 1f)) }
    var hudAlpha by remember { mutableStateOf(hudPrefs.getInt("hud_alpha_int", 100) / 100f) }

    // Graphics settings states
    val renderer = activity.xServerView?.renderer
    val initialFpsLimit = renderer?.fpsLimit ?: 0
    val initialEffectId = renderer?.effectId ?: 0
    val initialSharpness = renderer?.sharpness ?: 0.5f

    val fpsOptions = listOf(0, 30, 45, 60, 90, 120)
    val fpsLabels = listOf("Unlimited", "30 FPS", "45 FPS", "60 FPS", "90 FPS", "120 FPS")
    var fpsLimit by remember { mutableStateOf(initialFpsLimit) }

    var fsrEnabled by remember {
        mutableStateOf(
            initialEffectId == com.winlator.cmod.widget.VulkanRenderer.EFFECT_FSR ||
            initialEffectId == com.winlator.cmod.widget.VulkanRenderer.EFFECT_DLS
        )
    }

    val upscalerValues = listOf(com.winlator.cmod.widget.VulkanRenderer.EFFECT_FSR, com.winlator.cmod.widget.VulkanRenderer.EFFECT_DLS)
    val upscalerLabels = listOf("CAS", "DLS")
    var selectedUpscalerIndex by remember {
        val idx = upscalerValues.indexOf(initialEffectId)
        mutableStateOf(if (idx != -1) idx else 0)
    }

    val colorValues = listOf(
        com.winlator.cmod.widget.VulkanRenderer.EFFECT_NONE,
        com.winlator.cmod.widget.VulkanRenderer.EFFECT_CRT,
        com.winlator.cmod.widget.VulkanRenderer.EFFECT_HDR,
        com.winlator.cmod.widget.VulkanRenderer.EFFECT_NATURAL
    )
    val colorLabels = listOf("None", "CRT", "HDR", "Natural")
    var selectedColorIndex by remember {
        val idx = colorValues.indexOf(initialEffectId)
        mutableStateOf(if (idx != -1) idx else 0)
    }

    var sharpness by remember { mutableStateOf(initialSharpness) }

    // Helper functions to apply and save settings
    val applyGraphicsSettings = {
        val r = activity.xServerView?.renderer
        if (r != null) {
            val selectedColorEffect = colorValues[selectedColorIndex]
            val effectId = if (selectedColorEffect != com.winlator.cmod.widget.VulkanRenderer.EFFECT_NONE) {
                selectedColorEffect
            } else if (fsrEnabled) {
                upscalerValues[selectedUpscalerIndex]
            } else {
                com.winlator.cmod.widget.VulkanRenderer.EFFECT_NONE
            }
            r.setFpsLimit(fpsLimit)
            r.setEffect(effectId, sharpness)

            val s = activity.shortcut
            if (s != null) {
                s.putExtra("sidebarFpsLimit", fpsLimit.toString())
                s.saveData()
            }
        }
    }

    val saveGraphicsPreset = {
        val s = activity.shortcut
        if (s == null) {
            AppUtils.showToast(activity, "Open a game shortcut to save this preset")
        } else {
            s.putExtra("sidebarUpscalerIndex", selectedUpscalerIndex.toString())
            s.putExtra("sidebarEffectIndex", selectedColorIndex.toString())
            s.putExtra("sidebarSharpness", Math.round(sharpness * 100).toString())
            s.putExtra("sidebarSuperResolution", if (fsrEnabled) "1" else "0")
            s.putExtra("sidebarFpsLimit", fpsLimit.toString())
            s.saveData()
            AppUtils.showToast(activity, "Graphics preset saved to shortcut")
        }
    }

    val updateHudSettings = {
        val h = activity.frameRating
        if (h != null) {
            if (hudEnabled) {
                h.enableByUser()
                activity.hudDataSource?.start()
            } else {
                h.disableByUser()
            }
            h.resetFromContainer()
            if (hudEnabled) {
                h.toggleElement(0, cbFpsChecked)
                h.toggleElement(2, cbGpuChecked)
                h.toggleElement(3, cbCpuChecked)
                h.toggleElement(4, cbBattChecked)
                h.toggleElement(5, cbGraphChecked)
                h.toggleElement(6, cbRendererChecked)
                h.toggleElement(7, cbRamChecked)
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_settings),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Orion",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Session Settings",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }

            // Pause / Resume
            item {
                SidebarActionButton(
                    icon = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    label = if (isPaused) "Resume Session" else "Pause Session",
                    color = if (isPaused) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                ) {
                    isPaused = !isPaused
                    activity.isPaused = isPaused
                    if (isPaused) ProcessHelper.pauseAllWineProcesses()
                    else ProcessHelper.resumeAllWineProcesses()
                }
            }

            // Keyboard
            item {
                SidebarActionButton(
                    icon = Icons.Default.Keyboard,
                    label = "Keyboard"
                ) {
                    AppUtils.showKeyboard(activity)
                    activity.drawerLayout.closeDrawers()
                }
            }

            // Input profile section
            item {
                SidebarSectionHeader(
                    icon = Icons.Default.Gamepad,
                    label = "Input Profile",
                    isExpanded = inputExpanded,
                    onToggle = { inputExpanded = !inputExpanded }
                )
                AnimatedVisibility(
                    visible = inputExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SidebarSubItemButton(label = "Input Controls") {
                            activity.showInputControlsDialog()
                            activity.drawerLayout.closeDrawers()
                        }
                        SidebarSubItemButton(label = "Vibration Settings") {
                            activity.showVibrationDialog()
                            activity.drawerLayout.closeDrawers()
                        }
                    }
                }
            }

            // Mouse settings section
            item {
                SidebarSectionHeader(
                    icon = Icons.Default.Mouse,
                    label = "Mouse Settings",
                    isExpanded = mouseExpanded,
                    onToggle = { mouseExpanded = !mouseExpanded }
                )
                AnimatedVisibility(
                    visible = mouseExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Relative Mouse", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = relativeMouse,
                                onCheckedChange = { checked ->
                                    relativeMouse = checked
                                    activity.isRelativeMouseMovement = checked
                                    activity.xServer.setRelativeMouseMovement(checked)
                                }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Disable Mouse", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = disableMouse,
                                onCheckedChange = { checked ->
                                    disableMouse = checked
                                    activity.isMouseDisabled = checked
                                    activity.touchpadView.setMouseEnabled(!checked)
                                }
                            )
                        }
                    }
                }
            }

            // HUD monitor section
            item {
                SidebarSectionHeader(
                    icon = Icons.Default.Visibility,
                    label = "HUD Monitor",
                    isExpanded = hudExpanded,
                    onToggle = { hudExpanded = !hudExpanded }
                )
                AnimatedVisibility(
                    visible = hudExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Enable HUD", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Switch(
                                checked = hudEnabled,
                                onCheckedChange = { checked ->
                                    hudEnabled = checked
                                    updateHudSettings()
                                }
                            )
                        }

                        if (hudEnabled) {
                            Text("Metrics", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            
                            // Checkboxes Grid
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = cbFpsChecked, onCheckedChange = { cbFpsChecked = it; updateHudSettings() })
                                        Text("FPS", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = cbGpuChecked, onCheckedChange = { cbGpuChecked = it; updateHudSettings() })
                                        Text("GPU", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = cbCpuChecked, onCheckedChange = { cbCpuChecked = it; updateHudSettings() })
                                        Text("CPU", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = cbRamChecked, onCheckedChange = { cbRamChecked = it; updateHudSettings() })
                                        Text("RAM", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = cbBattChecked, onCheckedChange = { cbBattChecked = it; updateHudSettings() })
                                        Text("Batt", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = cbGraphChecked, onCheckedChange = { cbGraphChecked = it; updateHudSettings() })
                                        Text("Graph", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Checkbox(checked = cbRendererChecked, onCheckedChange = { cbRendererChecked = it; updateHudSettings() })
                                        Text("Renderer", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            // Sliders
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("HUD Size: ${String.format("%.1f", hudScale)}x", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(
                                    value = hudScale,
                                    onValueChange = {
                                        hudScale = it
                                        activity.frameRating?.setHudScale(it)
                                    },
                                    valueRange = 0.5f..2.5f
                                )

                                Text("HUD Alpha: ${Math.round(hudAlpha * 100)}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(
                                    value = hudAlpha,
                                    onValueChange = {
                                        hudAlpha = it
                                        activity.frameRating?.setHudAlpha(it)
                                    },
                                    valueRange = 0.1f..1.0f
                                )
                            }

                            // Reset HUD Action
                            Button(
                                onClick = {
                                    activity.frameRating?.forceReset()
                                    hudEnabled = true
                                    cbFpsChecked = true
                                    cbGpuChecked = true
                                    cbCpuChecked = true
                                    cbRamChecked = true
                                    cbBattChecked = true
                                    cbGraphChecked = true
                                    cbRendererChecked = true
                                    hudScale = 1.0f
                                    hudAlpha = 1.0f
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Reset HUD", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Graphic Performance section
            item {
                SidebarSectionHeader(
                    icon = Icons.Default.Settings,
                    label = "Graphic Engine",
                    isExpanded = graphicsExpanded,
                    onToggle = { graphicsExpanded = !graphicsExpanded }
                )
                AnimatedVisibility(
                    visible = graphicsExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // FPS limit Dropdown
                        Text("GAME FPS LIMIT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        var fpsMenuExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { fpsMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                val currentLabel = fpsLabels[fpsOptions.indexOf(fpsLimit).coerceIn(0, fpsLabels.size - 1)]
                                Text(currentLabel)
                            }
                            DropdownMenu(
                                expanded = fpsMenuExpanded,
                                onDismissRequest = { fpsMenuExpanded = false }
                            ) {
                                fpsLabels.forEachIndexed { index, label ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            fpsLimit = fpsOptions[index]
                                            applyGraphicsSettings()
                                            fpsMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Super Resolution (FSR/DLS)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SUPER RESOLUTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Switch(
                                checked = fsrEnabled,
                                onCheckedChange = { checked ->
                                    fsrEnabled = checked
                                    applyGraphicsSettings()
                                }
                            )
                        }

                        if (fsrEnabled) {
                            var upscalerMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { upscalerMenuExpanded = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(upscalerLabels[selectedUpscalerIndex])
                                }
                                DropdownMenu(
                                    expanded = upscalerMenuExpanded,
                                    onDismissRequest = { upscalerMenuExpanded = false }
                                ) {
                                    upscalerLabels.forEachIndexed { index, label ->
                                        DropdownMenuItem(
                                            text = { Text(label) },
                                            onClick = {
                                                selectedUpscalerIndex = index
                                                applyGraphicsSettings()
                                                upscalerMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Sharpness Slider
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Sharpness Strength: ${Math.round(sharpness * 100)}%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Slider(
                                    value = sharpness,
                                    onValueChange = {
                                        sharpness = it
                                        applyGraphicsSettings()
                                    },
                                    valueRange = 0.0f..1.0f
                                )
                            }
                        }

                        // Color Effects
                        Text("EFFECTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        var colorMenuExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { colorMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text(colorLabels[selectedColorIndex])
                            }
                            DropdownMenu(
                                expanded = colorMenuExpanded,
                                onDismissRequest = { colorMenuExpanded = false }
                            ) {
                                colorLabels.forEachIndexed { index, label ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedColorIndex = index
                                            applyGraphicsSettings()
                                            colorMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Save Preset Button
                        Button(
                            onClick = { saveGraphicsPreset() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Preset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Screen controls section
            item {
                SidebarSectionHeader(
                    icon = Icons.Default.Tv,
                    label = "Screen Controls",
                    isExpanded = screenExpanded,
                    onToggle = { screenExpanded = !screenExpanded }
                )
                AnimatedVisibility(
                    visible = screenExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SidebarSubItemButton(label = "PiP Mode") {
                            activity.enterPictureInPictureMode()
                            activity.drawerLayout.closeDrawers()
                        }
                        SidebarSubItemButton(label = "Toggle Fullscreen") {
                            activity.xServerView?.renderer?.toggleFullscreen()
                            activity.touchpadView?.toggleFullscreen()
                            activity.drawerLayout.closeDrawers()
                        }
                        SidebarSubItemButton(label = "Magnifier") {
                            val mag = activity.magnifierView
                            if (mag != null) {
                                mag.visibility = if (mag.visibility == View.VISIBLE) View.GONE else View.VISIBLE
                            }
                            activity.drawerLayout.closeDrawers()
                        }
                    }
                }
            }

            // Task Manager
            item {
                SidebarActionButton(
                    icon = Icons.Default.Dns,
                    label = "Task Manager"
                ) {
                    TaskManagerDialog(activity).show()
                    activity.drawerLayout.closeDrawers()
                }
            }

            // Logs
            item {
                val enableLogs = activity.preferences?.getBoolean("enable_wine_debug", false) == true ||
                        activity.preferences?.getBoolean("enable_box64_logs", false) == true
                if (enableLogs) {
                    SidebarActionButton(
                        icon = Icons.Default.Info,
                        label = "Logs"
                    ) {
                        activity.debugDialog?.show()
                        activity.drawerLayout.closeDrawers()
                    }
                }
            }

            // Exit
            item {
                SidebarActionButton(
                    icon = Icons.Default.ExitToApp,
                    label = "Exit Session",
                    color = MaterialTheme.colorScheme.error
                ) {
                    activity.drawerLayout.closeDrawers()
                    activity.exit()
                }
            }
        }
    }
}

@Composable
fun SidebarActionButton(
    icon: ImageVector,
    label: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color.copy(alpha = 0.8f),
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun SidebarSectionHeader(
    icon: ImageVector,
    label: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SidebarSubItemButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
