package io.github.daisukikaffuchino.nekoscript.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import io.github.daisukikaffuchino.nekoscript.engine.save.RawFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberAndroidSaveFrameCapture(): SaveFrameCapture {
    val view = LocalView.current
    return remember(view) {
        SaveFrameCapture {
            withContext(Dispatchers.Main.immediate) {
                val width = view.width
                val height = view.height
                if (width <= 0 || height <= 0) return@withContext null
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                try {
                    view.draw(Canvas(bitmap))
                    val pixels = IntArray(width * height)
                    bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
                    RawFrame(width, height, pixels)
                } finally {
                    bitmap.recycle()
                }
            }
        }
    }
}
