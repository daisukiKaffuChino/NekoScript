package io.github.daisukikaffuchino.nekoscript

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import io.github.daisukikaffuchino.nekoscript.engine.save.AndroidPreferencesSaveStorage
import io.github.daisukikaffuchino.nekoscript.ui.rememberAndroidSaveFrameCapture

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(
                saveStorage = remember { AndroidPreferencesSaveStorage(this) },
                timestampProvider = System::currentTimeMillis,
                frameCapture = rememberAndroidSaveFrameCapture(),
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    AppPreview()
}
