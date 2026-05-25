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
class PermissionNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    // Не выдаём разрешения принудительно, чтобы проверить экран разрешений
    // Разрешения будут запрошены через UI

    @Test
    fun permissionScreen_toMapScreen_navigationWorks() {
        composeTestRule.onNodeWithTag("PermissionScreen")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("ContinueWithoutPermissionButton")
            .performClick()
        composeTestRule.onNodeWithTag("MapScreen")
            .assertIsDisplayed()
    }
}