package com.mappoint

import android.Manifest
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BluetoothNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val grantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN
    )

    @Test
    fun navigateToBluetoothScreen_screenIsDisplayed() {
        composeTestRule.onNodeWithTag("MapScreen")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("BluetoothNavigationButton")
            .performClick()
        composeTestRule.onNodeWithTag("BluetoothScreen")
            .assertIsDisplayed()
    }

    @Test
    fun bluetoothScreen_switchBetweenTabs() {
        composeTestRule.onNodeWithTag("MapScreen")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("BluetoothNavigationButton")
            .performClick()
        composeTestRule.onNodeWithTag("DevicesTab")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("ChatTabButton")
            .performClick()
        composeTestRule.onNodeWithTag("ChatTab")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("DevicesTabButton")
            .performClick()
        composeTestRule.onNodeWithTag("DevicesTab")
            .assertIsDisplayed()
    }

    @Test
    fun bluetoothScreen_navigateBackToMap() {
        composeTestRule.onNodeWithTag("MapScreen")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("BluetoothNavigationButton")
            .performClick()
        composeTestRule.onNodeWithTag("BluetoothBackButton")
            .performClick()
        composeTestRule.onNodeWithTag("MapScreen")
            .assertIsDisplayed()
    }
}