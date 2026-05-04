package com.mappoint.ui.screens.map

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

enum class DrawerMode {
    MANAGE, ADD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapDrawer(
    drawerState: DrawerState,
    mode: DrawerMode,
    viewModel: MapViewModel,
    onCloseDrawer: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(360.dp)
            ) {
                when (mode) {
                    DrawerMode.MANAGE -> ManageDrawerContent(viewModel, onCloseDrawer)
                    DrawerMode.ADD -> AddDrawerContent(viewModel, onCloseDrawer)
                }
            }
        }
    ) {
        content()
    }
}