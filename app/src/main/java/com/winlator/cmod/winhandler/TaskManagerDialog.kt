package com.winlator.cmod.winhandler

import android.app.ActivityManager
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window as PhoneWindow
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cpu
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.winlator.cmod.R
import com.winlator.cmod.XServerDisplayActivity
import com.winlator.cmod.contentdialog.ContentDialog
import com.winlator.cmod.core.CPUStatus
import com.winlator.cmod.core.FileUtils
import com.winlator.cmod.core.ProcessHelper
import com.winlator.cmod.core.StringUtils
import com.winlator.cmod.ui.theme.OrionTheme
import com.winlator.cmod.xenvironment.ImageFs
import com.winlator.cmod.xserver.Window
import com.winlator.cmod.xserver.XLock
import com.winlator.cmod.xserver.XServer
import kotlinx.coroutines.delay
import java.io.File

class TaskManagerDialog(private val activity: XServerDisplayActivity) : Dialog(activity, R.style.ContentDialog), OnGetProcessInfoListener {
    private val processesList = mutableStateListOf<ProcessInfo>()
    private val tempProcesses = mutableListOf<ProcessInfo>()
    private val lock = Any()

    // Status states
    private var cpuUsagePercent by mutableStateOf(0)
    private var cpuClockSpeeds = mutableStateListOf<String>()
    private var memoryUsagePercent by mutableStateOf(0)
    private var memoryInfoText by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)

        val composeView = ComposeView(context).apply {
            setContent {
                OrionTheme {
                    TaskManagerView()
                }
            }
        }
        setContentView(composeView)

        // Configura dimensões do Dialog
        window?.let { w ->
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            w.setGravity(Gravity.CENTER)
            w.setBackgroundDrawableResource(android.R.color.transparent)
        }

        FileUtils.clear(getIconDir(activity))
    }

    override fun show() {
        super.show()
        updateSystemInfo()
        activity.winHandler.setOnGetProcessInfoListener(this)
        activity.winHandler.listProcesses()
    }

    override fun dismiss() {
        activity.winHandler.setOnGetProcessInfoListener(null)
        super.dismiss()
    }

    override fun onGetProcessInfo(index: Int, numProcesses: Int, processInfo: ProcessInfo?) {
        activity.runOnUiThread {
            synchronized(lock) {
                if (numProcesses == 0) {
                    processesList.clear()
                    return@runOnUiThread
                }
                if (index == 0) {
                    tempProcesses.clear()
                }
                if (processInfo != null) {
                    tempProcesses.add(processInfo)
                }
                if (index == numProcesses - 1) {
                    processesList.clear()
                    processesList.addAll(tempProcesses)
                }
            }
        }
    }

    private fun updateSystemInfo() {
        // CPU Info
        val clockSpeeds = CPUStatus.getCurrentClockSpeeds()
        var totalClockSpeed = 0
        var maxClockSpeed: Short = 0
        cpuClockSpeeds.clear()

        for (i in clockSpeeds.indices) {
            val maxSpeed = CPUStatus.getMaxClockSpeed(i)
            cpuClockSpeeds.add("${clockSpeeds[i]}/${maxSpeed} MHz")
            totalClockSpeed += clockSpeeds[i].toInt()
            if (maxSpeed > maxClockSpeed) {
                maxClockSpeed = maxSpeed
            }
        }

        val avgClockSpeed = totalClockSpeed / clockSpeeds.size
        cpuUsagePercent = ((avgClockSpeed.toFloat() / maxClockSpeed) * 100).toInt().coerceIn(0, 100)

        // Memory Info
        val activityManager = activity.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val usedMem = memoryInfo.totalMem - memoryInfo.availMem
        memoryUsagePercent = ((usedMem.toDouble() / memoryInfo.totalMem) * 100).toInt().coerceIn(0, 100)
        memoryInfoText = "${StringUtils.formatBytes(usedMem, false)}/${StringUtils.formatBytes(memoryInfo.totalMem)}"
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TaskManagerView() {
        var showAffinityDialogFor by remember { mutableStateOf<ProcessInfo?>(null) }
        var showConfirmEndProcessFor by remember { mutableStateOf<ProcessInfo?>(null) }

        // Loop de atualização automática a cada 1 segundo
        LaunchedEffect(Unit) {
            while (true) {
                updateSystemInfo()
                activity.winHandler.listProcesses()
                delay(1000)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_task_manager),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Task Manager",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Button(
                        onClick = {
                            dismiss()
                            ContentDialog.prompt(
                                activity,
                                R.string.new_task,
                                "taskmgr.exe"
                            ) { command ->
                                activity.winHandler.exec(command)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Task", fontSize = 12.sp)
                    }
                }

                // Performance Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // CPU Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("CPU ($cpuUsagePercent%)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            LinearProgressIndicator(
                                progress = { cpuUsagePercent / 100f },
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (cpuClockSpeeds.isNotEmpty()) cpuClockSpeeds[0] else "",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Memory Card
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Memory ($memoryUsagePercent%)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            LinearProgressIndicator(
                                progress = { memoryUsagePercent / 100f },
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = memoryInfoText,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Process Table Headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Process Name", modifier = Modifier.weight(1.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("PID", modifier = Modifier.weight(0.7f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("RAM", modifier = Modifier.weight(0.9f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(36.dp)) // Espaço para o menu
                }

                // Process List
                if (processesList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No active processes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(processesList, key = { it.pid }) { process ->
                            ProcessRow(
                                process = process,
                                onShowAffinity = { showAffinityDialogFor = process },
                                onBringToFront = {
                                    activity.winHandler.bringToFront(process.name)
                                    dismiss()
                                },
                                onEndProcess = { showConfirmEndProcessFor = process }
                            )
                        }
                    }
                }

                // Footer Info & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Processes: ${processesList.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { dismiss() }) {
                        Text("Close")
                    }
                }
            }
        }

        // Dialog de Afinidade de CPU
        showAffinityDialogFor?.let { process ->
            ProcessorAffinityDialog(
                processInfo = process,
                activity = activity,
                onDismiss = { showAffinityDialogFor = null }
            )
        }

        // Diálogo de confirmação de término de processo
        showConfirmEndProcessFor?.let { process ->
            AlertDialog(
                onDismissRequest = { showConfirmEndProcessFor = null },
                title = { Text("End Process") },
                text = { Text("Do you want to end ${process.name}?") },
                confirmButton = {
                    Button(
                        onClick = {
                            activity.winHandler.killProcess(process.name)
                            showConfirmEndProcessFor = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("End Process")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmEndProcessFor = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    @Composable
    fun ProcessRow(
        process: ProcessInfo,
        onShowAffinity: () -> Unit,
        onBringToFront: () -> Unit,
        onEndProcess: () -> Unit
    ) {
        var menuExpanded by remember { mutableStateOf(false) }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Name & Icon
                Row(
                    modifier = Modifier.weight(1.8f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProcessIcon(processInfo = process, activity = activity)
                    Text(
                        text = "${process.name}${if (process.wow64Process) " *32" else ""}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // PID
                Text(
                    text = process.pid.toString(),
                    modifier = Modifier.weight(0.7f),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Memory
                Text(
                    text = process.formattedMemoryUsage,
                    modifier = Modifier.weight(0.9f),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Actions Button
                Box {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Processor Affinity...") },
                            onClick = {
                                menuExpanded = false
                                onShowAffinity()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Bring to Front") },
                            onClick = {
                                menuExpanded = false
                                onBringToFront()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("End Process", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onEndProcess()
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ProcessIcon(processInfo: ProcessInfo, activity: XServerDisplayActivity) {
        val xServer = activity.xServer
        val window = remember(processInfo.pid) {
            try {
                xServer.lock(XServer.Lockable.WINDOW_MANAGER).use {
                    xServer.windowManager.findWindowWithProcessId(processInfo.pid)
                }
            } catch (e: Exception) {
                null
            }
        }

        val bitmap = remember(window) {
            if (window != null) {
                try {
                    xServer.pixmapManager.getWindowIcon(window)
                } catch (e: Exception) {
                    null
                }
            } else null
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.taskmgr_process),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.size(22.dp)
            )
        }
    }

    @Composable
    fun ProcessorAffinityDialog(
        processInfo: ProcessInfo,
        activity: XServerDisplayActivity,
        onDismiss: () -> Unit
    ) {
        val numCpus = remember { CPUStatus.getCurrentClockSpeeds().size }
        val checkedCpus = remember {
            val list = mutableStateListOf<Boolean>()
            val rawList = processInfo.cpuList
            val activeIndices = if (rawList != null && rawList.isNotEmpty()) {
                rawList.split(",").mapNotNull { it.trim().toIntOrNull() }
            } else emptyList()

            for (i in 0 until numCpus) {
                list.add(activeIndices.contains(i) || activeIndices.isEmpty())
            }
            list
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_cpu),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(processInfo.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Select processors allowed to run this task:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items((0 until numCpus).toList()) { i ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { checkedCpus[i] = !checkedCpus[i] }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checkedCpus[i],
                                    onCheckedChange = { checkedCpus[i] = it }
                                )
                                Text("CPU $i", fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cpuArray = BooleanArray(numCpus) { checkedCpus[it] }
                        val mask = ProcessHelper.getAffinityMask(cpuArray)
                        activity.winHandler.setProcessAffinity(processInfo.pid, mask)
                        onDismiss()
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    companion object {
        @JvmStatic
        fun getIconDir(context: Context): File {
            val iconDir = File(ImageFs.find(context).rootDir, "home/xuser/.local/share/icons/taskmgr")
            if (!iconDir.isDirectory) {
                iconDir.mkdirs()
            }
            return iconDir
        }
    }
}
