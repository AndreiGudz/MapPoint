package com.mappoint

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.mappoint.ui.screens.OfflineMapApp
import com.mappoint.ui.theme.MapPointTheme
import org.osmdroid.config.Configuration
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация osmdroid
        Configuration.getInstance().userAgentValue = packageName
        setupOsmdroidPaths()

        // Настройки кеширования
        Configuration.getInstance().tileFileSystemCacheMaxBytes = 1024 * 1024 * 100 // 100 MB
        Configuration.getInstance().tileFileSystemCacheTrimBytes = 1024 * 1024 * 10  // 10 MB
        Configuration.getInstance().tileDownloadMaxQueueSize = 1000
        Configuration.getInstance().tileDownloadThreads = 8

        // Настройка для оффлайн режима
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )

        // Настройка тайлов по умолчанию
        Configuration.getInstance().setUserAgentValue(packageName)

        setContent {
            MapPointTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OfflineMapApp()
                }
            }
        }
    }

    private fun setupOsmdroidPaths() {

        val osmdroidPath = File(
            getExternalFilesDir(null),
            "osmdroid"
        )

        osmdroidPath.mkdirs()
        Configuration.getInstance().osmdroidBasePath = osmdroidPath

        // Путь для кеша тайлов
        val tileCachePath = File(osmdroidPath, "tiles")
        tileCachePath.mkdirs()
        Configuration.getInstance().osmdroidTileCache = tileCachePath
    }

    override fun onResume() {
        super.onResume()
        Configuration.getInstance().load(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
    }

    override fun onPause() {
        super.onPause()
        Configuration.getInstance().save(
            this,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
    }
}