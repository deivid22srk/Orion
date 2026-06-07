package com.winlator.cmod.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.winlator.cmod.container.Container
import com.winlator.cmod.container.ContainerManager
import com.winlator.cmod.container.Shortcut
import com.winlator.cmod.ui.screens.ContainersScreen
import com.winlator.cmod.ui.screens.FileManagerScreen
import com.winlator.cmod.ui.screens.InputControlsScreen
import com.winlator.cmod.ui.screens.SettingsScreen
import com.winlator.cmod.ui.screens.ShortcutsScreen

enum class Screen(val title: String, val shortTitle: String, val icon: ImageVector) {
    Containers("Containers", "Containers", Icons.Default.Home),
    Shortcuts("Meus Jogos (Atalhos)", "Atalhos", Icons.Default.Star),
    FileManager("Gerenciador de Arquivos", "Arquivos", Icons.Default.Folder),
    InputControls("Controles de Entrada", "Controles", Icons.Default.Build),
    Settings("Configurações", "Configs", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrionTopBar(title: String) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun OrionNavigationBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        tonalElevation = 8.dp
    ) {
        Screen.values().forEach { screen ->
            val isSelected = currentScreen == screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onScreenSelected(screen) },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = screen.shortTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrionApp(
    containerManager: ContainerManager,
    onStartContainer: (Container) -> Unit,
    onRunExe: (Container, String) -> Unit,
    onStartShortcut: (Shortcut) -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.Containers) }

    Scaffold(
        topBar = {
            OrionTopBar(title = currentScreen.title)
        },
        bottomBar = {
            OrionNavigationBar(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally { width -> direction * width / 2 } + fadeIn())
                        .togetherWith(slideOutHorizontally { width -> -direction * width / 2 } + fadeOut())
                },
                label = "screen_transition",
                modifier = Modifier.fillMaxSize()
            ) { screen ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (screen) {
                        Screen.Containers -> ContainersScreen(
                            containerManager = containerManager,
                            onStartContainer = onStartContainer
                        )
                        Screen.Shortcuts -> ShortcutsScreen(
                            containerManager = containerManager,
                            onStartShortcut = onStartShortcut
                        )
                        Screen.FileManager -> FileManagerScreen(
                            containerManager = containerManager,
                            onRunExe = onRunExe
                        )
                        Screen.InputControls -> InputControlsScreen(
                            containerManager = containerManager
                        )
                        Screen.Settings -> SettingsScreen()
                    }
                }
            }
        }
    }
}

