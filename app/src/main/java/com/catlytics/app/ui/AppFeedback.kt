package com.catlytics.app.ui

import android.content.Context
import android.content.res.Resources
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import com.catlytics.core.designsystem.text.UiText
import com.catlytics.core.designsystem.text.resolve

internal class AppFeedback(
    private val context: Context,
    private val resources: Resources,
) {
    fun show(message: UiText) {
        Toast.makeText(context, message.resolve(resources), Toast.LENGTH_SHORT).show()
    }
}

@Composable
internal fun rememberAppFeedback(): AppFeedback {
    val context = LocalContext.current
    val resources = LocalResources.current
    return remember(context, resources) { AppFeedback(context, resources) }
}
