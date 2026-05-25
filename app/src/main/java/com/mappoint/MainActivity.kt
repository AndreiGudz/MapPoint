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
/*
Based on OSMDroid (Apache 2.0 license)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация osmdroid
        Configuration.getInstance().userAgentValue = packageName
        setupOsmdroidPaths()

        // Настройки кеширования
        // By default 600 Mb
        Configuration.getInstance().tileFileSystemCacheMaxBytes = 1024 * 1024 * 1000 // 1000 MB
        // By default 500 Mb
        Configuration.getInstance().tileFileSystemCacheTrimBytes = 1024 * 1024 * 900  // 900 MB
        Configuration.getInstance().tileDownloadMaxQueueSize = 1000
        // Просят не использовать более 2х потоков загрузки тайлов
        Configuration.getInstance().tileDownloadThreads = 2

        // Настройка для оффлайн режима (происзодит также при возобновлении)
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )



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
        // путь к папке по умолчанию для osmdroid
        val osmdroidPath = File(getExternalFilesDir(null), "osmdroid")
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