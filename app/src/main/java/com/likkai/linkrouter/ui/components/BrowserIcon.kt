package com.likkai.linkrouter.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun BrowserIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val context = LocalContext.current
    val icon: Drawable? = remember(packageName) {
        try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (e: Exception) {
            null
        }
    }

    if (icon != null) {
        val bitmap = remember(icon) {
            icon.toBitmap(width = 96, height = 96).asImageBitmap()
        }
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.size(size)
        )
    }
}
