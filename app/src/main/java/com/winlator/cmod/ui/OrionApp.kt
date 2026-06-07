package com.winlator.cmod.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.winlator.cmod.container.Container
import com.winlator.cmod.container.ContainerManager
import com.winlator.cmod.container.Shortcut
import com.winlator.cmod.ui.screens.ContainersScreen
import com.winlator.cmod.ui.screens.FileManagerScreen
import com.winlator.cmod.ui.screens.InputControlsScreen
import com.winlator.cmod.ui.screens.SettingsScreen
import com.winlator.cmod.ui.screens.ShortcutsScreen
import kotlinx.coroutines.launch

enum class Screen(val title: String, val icon: ImageVector) {
    Containers("Containers", Icons.Default.Home),
    Shortcuts("Meus Jogos (Atalhos)", Icons.Default.Star),
    FileManager("Gerenciador de Arquivos", Icons.Default.Folder),
    InputControls("Controles de Entrada", Icons.Default.Build),
    Settings("Configurações", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrionApp(
    containerManager: ContainerManager,
    onStartContainer: (Container) -> Unit,
    onRunExe: (Container, String) -> Unit,
    onStartShortcut: (Shortcut) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(Screen.Containers) }
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.85f),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Orion Emulator",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Screen.values().forEach { screen ->
                    NavigationDrawerItem(
                        label = { Text(text = screen.title) },
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            unselectedContainerColor = MaterialTheme.colorScheme.surface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = currentScreen.title) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu Lateral"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (currentScreen) {
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
