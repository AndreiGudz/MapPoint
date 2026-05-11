package com.mappoint.utils

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

fun ComposeContentTestRule.addTestPoint() {
    onNodeWithTag("OpenAddPointButton")
        .performClick()

    onNodeWithTag("LatitudeField")
        .performTextInput("55.7558")

    onNodeWithTag("LongitudeField")
        .performTextInput("37.6176")

    onNodeWithTag("TitleField")
        .performTextInput("Test")

    onNodeWithTag("DescriptionField")
        .performTextInput("Test description")

    onNodeWithTag("SavePointButton")
        .performClick()
}