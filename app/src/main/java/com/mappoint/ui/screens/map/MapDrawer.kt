package com.mappoint.ui.screens.map

import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

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
                modifier = Modifier
                    .width(360.dp)
                    .testTag("DrawerPanel")
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