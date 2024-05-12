package com.example.myapplication
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    BackupApp()
                }
            }
        }
    }
}

@Composable
fun BackupApp() {
    val context = LocalContext.current
    var backupStatus by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Backup App")
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val filesList = context.filesDir.listFiles()?.toList() ?: emptyList()
            backupStatus = "Starting backup..."
            try {
                backupFiles(filesList)
                backupStatus = "Backup completed successfully."
            } catch (e: Exception) {
                backupStatus = "Error during backup: ${e.message}"
                e.printStackTrace()
            }
        }) {
            Text(text = "Backup Files")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = backupStatus)
    }
}

private fun backupFiles(filesList: List<File>) {
    val backupFolder = File("C:/Backup") // Update with your desired backup folder path
    backupFolder.mkdirs()

    filesList.filter { it.extension.equals("txt", ignoreCase = true) }.forEach { file ->
        val destFile = File(backupFolder, file.name)
        try {
            file.copyTo(destFile, overwrite = true)
            println("Copied file: ${file.name} to $destFile")
        } catch (e: Exception) {
            println("Error copying file: ${file.name}, ${e.message}")
            e.printStackTrace()
        }
    }
}
