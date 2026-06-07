package com.winlator.cmod.ui.screens

import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.winlator.cmod.container.Container
import com.winlator.cmod.container.ContainerManager
import com.winlator.cmod.xenvironment.ImageFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    containerManager: ContainerManager,
    onRunExe: (Container, String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentDir by remember {
        val defaultDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        mutableStateOf(if (defaultDir.exists()) defaultDir else Environment.getExternalStorageDirectory())
    }

    val filesList = remember { mutableStateListOf<File>() }
    var isLoading by remember { mutableStateOf(true) }
    var refreshTrigger by remember { mutableStateOf(0) }

    fun loadDirectory(dir: File) {
        currentDir = dir
        refreshTrigger++
    }

    LaunchedEffect(currentDir, refreshTrigger) {
        isLoading = true
        val list = withContext(Dispatchers.IO) {
            currentDir.listFiles()?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        }
        filesList.clear()
        filesList.addAll(list)
        isLoading = false
    }

    // Exe Selection States
    var selectedExeFile by remember { mutableStateOf<File?>(null) }
    var showContainerSelectDialog by remember { mutableStateOf(false) }
    var isShortcutCreation by remember { mutableStateOf(false) }

    // Dialog for creating folders/files
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                IconButton(
                    onClick = {
                        val parent = currentDir.parentFile
                        if (parent != null && parent.canRead()) {
                            loadDirectory(parent)
                        }
                    },
                    enabled = currentDir.parentFile != null
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentDir.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1
                )
            }
            
            // Drive Quick Selector
            var expDrives by remember { mutableStateOf(false) }
            Box {
                Button(onClick = { expDrives = true }) {
                    Text("Drives")
                }
                DropdownMenu(expanded = expDrives, onDismissRequest = { expDrives = false }) {
                    DropdownMenuItem(
                        text = { Text("Drive D: (Downloads)") },
                        onClick = {
                            expDrives = false
                            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            loadDirectory(if (downloadDir.exists()) downloadDir else Environment.getExternalStorageDirectory())
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Drive C: (Wine)") },
                        onClick = {
                            expDrives = false
                            val rootDir = ImageFs.find(context).rootDir
                            val wineC = File(rootDir, "home/xuser/drive_c")
                            if (wineC.exists()) {
                                loadDirectory(wineC)
                            } else {
                                Toast.makeText(context, "Instale o Wine primeiro", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Files List
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isLoading) {
                // Empty placeholder or simple indicator to avoid jank
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filesList, key = { it.absolutePath }) { file ->
                        FileListItem(
                            file = file,
                            onClick = {
                                if (file.isDirectory) {
                                    loadDirectory(file)
                                } else if (file.name.endsWith(".exe", ignoreCase = true)) {
                                    selectedExeFile = file
                                    isShortcutCreation = false
                                    showContainerSelectDialog = true
                                }
                            },
                            onDelete = {
                                scope.launch(Dispatchers.IO) {
                                    file.deleteRecursively()
                                    withContext(Dispatchers.Main) {
                                        refreshTrigger++
                                    }
                                }
                            },
                            onCreateShortcut = {
                                selectedExeFile = file
                                isShortcutCreation = true
                                showContainerSelectDialog = true
                            }
                        )
                    }
                }
            }
        }

        // Dialog to select Container for Exe Execution/Shortcut
        if (showContainerSelectDialog && selectedExeFile != null) {
            val containers = containerManager.containers
            var selectedContainer by remember { mutableStateOf(if (containers.isNotEmpty()) containers[0] else null) }
            var expandedContainerSelection by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showContainerSelectDialog = false },
                title = { Text(if (isShortcutCreation) "Criar Atalho para Jogo" else "Executar com Container") },
                text = {
                    Column {
                        Text(
                            text = "Arquivo: ${selectedExeFile?.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (containers.isEmpty()) {
                            Text(
                                text = "Nenhum container criado. Crie um container primeiro.",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = "Selecione o Container:",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            ExposedDropdownMenuBox(
                                expanded = expandedContainerSelection,
                                onExpandedChange = { expandedContainerSelection = !expandedContainerSelection }
                            ) {
                                TextField(
                                    readOnly = true,
                                    value = selectedContainer?.name ?: "Nenhum",
                                    onValueChange = {},
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedContainerSelection) },
                                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = expandedContainerSelection,
                                    onDismissRequest = { expandedContainerSelection = false }
                                ) {
                                    containers.forEach { container ->
                                        DropdownMenuItem(
                                            text = { Text(container.name) },
                                            onClick = {
                                                selectedContainer = container
                                                expandedContainerSelection = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = selectedContainer != null,
                        onClick = {
                            val container = selectedContainer
                            val file = selectedExeFile
                            if (container != null && file != null) {
                                if (isShortcutCreation) {
                                    scope.launch(Dispatchers.IO) {
                                        // Create desktop shortcut logic
                                        createShortcut(container, file)
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Atalho criado com sucesso!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    onRunExe(container, file.absolutePath)
                                }
                                showContainerSelectDialog = false
                            }
                        }
                    ) {
                        Text("Confirmar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showContainerSelectDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

// Local helper function to create shortcuts
private fun createShortcut(container: Container, exeFile: File) {
    val desktopDir = container.desktopDir
    if (!desktopDir.exists()) desktopDir.mkdirs()
    
    val baseName = exeFile.nameWithoutExtension
    val shortcutFile = File(desktopDir, "$baseName.desktop")
    
    try {
        PrintWriter(FileWriter(shortcutFile)).use { writer ->
            writer.println("[Desktop Entry]")
            writer.println("Type=Application")
            writer.println("Name=$baseName")
            writer.println("Exec=wine \"${exeFile.absolutePath}\"")
            writer.println("Icon=wine")
            writer.println("StartupWMClass=$baseName")
            writer.println("[Extra Data]")
            writer.println("container_id=${container.id}")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun FileListItem(
    file: File,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onCreateShortcut: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    imageVector = if (file.isDirectory) Icons.Default.Home else Icons.Default.Build,
                    contentDescription = if (file.isDirectory) "Pasta" else "Arquivo",
                    tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!file.isDirectory) {
                        val sizeKb = file.length() / 1024
                        Text(
                            text = "$sizeKb KB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { expandedMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu de arquivo"
                    )
                }
                DropdownMenu(expanded = expandedMenu, onDismissRequest = { expandedMenu = false }) {
                    if (file.name.endsWith(".exe", ignoreCase = true)) {
                        DropdownMenuItem(
                            text = { Text("Criar Atalho") },
                            onClick = {
                                expandedMenu = false
                                onCreateShortcut()
                            },
                            leadingIcon = { Icon(Icons.Default.Star, contentDescription = "Atalho") }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Excluir") },
                        onClick = {
                            expandedMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = "Excluir") }
                    )
                }
            }
        }
    }
}
