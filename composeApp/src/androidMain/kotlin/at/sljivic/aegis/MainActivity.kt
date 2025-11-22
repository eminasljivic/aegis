package at.sljivic.aegis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val filePicker = AndroidFilePicker(
            activity = this,
            onFilePicked = { uri ->
                if (uri.path != null) {
                    println(uri.path)
                    getSyscallList(this);
                }
            }
        )

        setContent {
            App(filePicker)
        }
    }
}