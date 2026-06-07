package com.winlator.cmod

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.winlator.cmod.container.ContainerManager
import com.winlator.cmod.ui.OrionApp
import com.winlator.cmod.ui.theme.OrionTheme
import com.winlator.cmod.xenvironment.ImageFsInstaller
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter

class MainActivity : ComponentActivity() {

    private lateinit var containerManager: ContainerManager
    private lateinit var sharedPreferences: SharedPreferences

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val writeGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
        val readGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        if (writeGranted || readGranted) {
            ImageFsInstaller.installIfNeeded(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        
        // Ensure standard winlator directory exists
        val winlatorDir = File(Environment.getExternalStorageDirectory(), "Winlator")
        if (!winlatorDir.exists()) {
            winlatorDir.mkdirs()
        }

        containerManager = ContainerManager(this)

        // Request notifications permission on Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
            }
        }

        checkAndRequestPermissions()

        setContent {
            OrionTheme {
                OrionApp(
                    containerManager = containerManager,
                    onStartContainer = { container ->
                        containerManager.activateContainer(container)
                        val intent = Intent(this, XServerDisplayActivity::class.java)
                        intent.putExtra("container_id", container.id)
                        startActivity(intent)
                    },
                    onRunExe = { container, exePath ->
                        try {
                            val tempShortcut = File(cacheDir, "temp_run.desktop")
                            PrintWriter(FileWriter(tempShortcut)).use { writer ->
                                writer.println("[Desktop Entry]")
                                writer.println("Name=" + File(exePath).name)
                                writer.println("Exec=env WINEPREFIX=\"/home/xuser/.wine\" wine \"$exePath\"")
                                writer.println("Type=Application")
                                writer.println("container_id:${container.id}")
                            }
                            containerManager.activateContainer(container)
                            val intent = Intent(this, XServerDisplayActivity::class.java)
                            intent.putExtra("container_id", container.id)
                            intent.putExtra("shortcut_path", tempShortcut.absolutePath)
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Erro ao iniciar executável: ${e.message}", Toast.LENGTH_LONG).show()
                            e.printStackTrace()
                        }
                    },
                    onStartShortcut = { shortcut ->
                        containerManager.activateContainer(shortcut.container)
                        val intent = Intent(this, XServerDisplayActivity::class.java)
                        intent.putExtra("container_id", shortcut.container.id)
                        intent.putExtra("shortcut_path", shortcut.file.absolutePath)
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } else {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = android.net.Uri.parse("package:$packageName")
                    startActivity(intent)
                }
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            ImageFsInstaller.installIfNeeded(this)
        }
    }
}
