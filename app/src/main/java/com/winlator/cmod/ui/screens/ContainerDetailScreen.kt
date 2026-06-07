package com.winlator.cmod.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.winlator.cmod.container.Container
import com.winlator.cmod.container.ContainerManager
import com.winlator.cmod.contents.ContentsManager
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContainerDetailScreen(
    container: Container?,
    containerManager: ContainerManager,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    var name by remember { mutableStateOf(container?.name ?: "Container Novo") }
    var screenSize by remember { mutableStateOf(container?.screenSize ?: Container.DEFAULT_SCREEN_SIZE) }
    var graphicsDriver by remember { mutableStateOf(container?.graphicsDriver ?: Container.DEFAULT_GRAPHICS_DRIVER) }
    var dxwrapper by remember { mutableStateOf(container?.dxwrapper ?: Container.DEFAULT_DXWRAPPER) }
    var wineVersion by remember { mutableStateOf(container?.wineVersion ?: "9.0") }
    var envVars by remember { mutableStateOf(container?.envVars ?: Container.DEFAULT_ENV_VARS) }
    
    // CPU Affinity Cores (8-core typical configuration)
    val cpuCoresState = remember {
        val list = mutableListOf<Boolean>()
        for (i in 0 until 8) {
            list.add(true) // default all active
        }
        list
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Geral", "Processador", "Variáveis de Ambiente", "Drives")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val data = JSONObject()
                    data.put("name", name)
                    data.put("screenSize", screenSize)
                    data.put("graphicsDriver", graphicsDriver)
                    data.put("dxwrapper", dxwrapper)
                    data.put("wineVersion", wineVersion)
                    data.put("envVars", envVars)
                    
                    if (container == null) {
                        // Create
                        val contentsManager = ContentsManager(containerManager.context)
                        containerManager.createContainerAsync(data, contentsManager) {
                            onSave()
                        }
                    } else {
                        // Update
                        container.name = name
                        container.screenSize = screenSize
                        container.graphicsDriver = graphicsDriver
                        container.dxwrapper = dxwrapper
                        container.wineVersion = wineVersion
                        container.envVars = envVars
                        container.saveData(data)
                        onSave()
                    }
                }) {
                    Text("Salvar")
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (selectedTab) {
                0 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nome do Container") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Resolution Selector
                        var expScreenSize by remember { mutableStateOf(false) }
                        val screenSizes = listOf("800x600", "1024x768", "1280x720", "1600x900", "1920x1080")
                        ExposedDropdownMenuBox(
                            expanded = expScreenSize,
                            onExpandedChange = { expScreenSize = !expScreenSize }
                        ) {
                            TextField(
                                readOnly = true,
                                value = screenSize,
                                onValueChange = {},
                                label = { Text("Resolução da Tela") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expScreenSize) },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expScreenSize,
                                onDismissRequest = { expScreenSize = false }
                            ) {
                                screenSizes.forEach { selection ->
                                    DropdownMenuItem(
                                        text = { Text(selection) },
                                        onClick = {
                                            screenSize = selection
                                            expScreenSize = false
                                        }
                                    )
                                }
                            }
                        }

                        // Graphics Driver Selector
                        var expGraphicsDriver by remember { mutableStateOf(false) }
                        val drivers = listOf("wrapper", "turnip", "virgl", "llvmpipe")
                        ExposedDropdownMenuBox(
                            expanded = expGraphicsDriver,
                            onExpandedChange = { expGraphicsDriver = !expGraphicsDriver }
                        ) {
                            TextField(
                                readOnly = true,
                                value = graphicsDriver,
                                onValueChange = {},
                                label = { Text("Driver Gráfico (GPU)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expGraphicsDriver) },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expGraphicsDriver,
                                onDismissRequest = { expGraphicsDriver = false }
                            ) {
                                drivers.forEach { selection ->
                                    DropdownMenuItem(
                                        text = { Text(selection) },
                                        onClick = {
                                            graphicsDriver = selection
                                            expGraphicsDriver = false
                                        }
                                    )
                                }
                            }
                        }

                        // Wine DX Wrapper Selector
                        var expDxwrapper by remember { mutableStateOf(false) }
                        val wrappers = listOf("dxvk+vkd3d", "wined3d", "virgl-wrapper")
                        ExposedDropdownMenuBox(
                            expanded = expDxwrapper,
                            onExpandedChange = { expDxwrapper = !expDxwrapper }
                        ) {
                            TextField(
                                readOnly = true,
                                value = dxwrapper,
                                onValueChange = {},
                                label = { Text("DX Wrapper") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expDxwrapper) },
                                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expDxwrapper,
                                onDismissRequest = { expDxwrapper = false }
                            ) {
                                wrappers.forEach { selection ->
                                    DropdownMenuItem(
                                        text = { Text(selection) },
                                        onClick = {
                                            dxwrapper = selection
                                            expDxwrapper = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    Column {
                        Text(
                            text = "Afinidade de Núcleos (CPU Affinity)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Selecione quais núcleos da CPU o container deve usar. Desmarcar núcleos lentos ou de baixa performance (Little Cores) pode otimizar drasticamente o desempenho de certos jogos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        for (i in 0 until 8) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = cpuCoresState[i],
                                    onCheckedChange = { cpuCoresState[i] = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "CPU Núcleo $i",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
                2 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Variáveis de Ambiente",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        OutlinedTextField(
                            value = envVars,
                            onValueChange = { envVars = it },
                            label = { Text("Configurações das Variáveis de Ambiente") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 6
                        )
                    }
                }
                3 -> {
                    Column {
                        Text(
                            text = "Drives Mapeados",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Drive C:",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "/home/wine/drive_c",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Drive D: (Downloads)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "/storage/emulated/0/Download",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
