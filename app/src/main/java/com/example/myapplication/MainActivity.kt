package com.example.myapplication

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.DataBackupAppTheme
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.component1
import com.google.firebase.storage.component2
import com.google.firebase.storage.storage
import java.io.File


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DataBackupAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}


@Composable
fun MainScreen() {
    var upDown by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Text(
            text = "Data Backup App",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (upDown) {
                Text(
                    text = "Upload",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary


                )
            } else {
                Text(
                    text = "Download",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = upDown,
                onCheckedChange = { upDown = it }
            )
        }

        if (upDown) {
            UploadScreen()

        } else {
            DownloadScreen()
        }

    }
}

@Composable
fun DownloadScreen() {
    val storage = Firebase.storage
    val listRef = storage.reference
    var listItems by remember { mutableStateOf(listOf<String>()) }
    var id by remember { mutableStateOf("") }

    // Recursive Function to iterate over all files in the database
    fun listFiles(ref: StorageReference) {
        ref.listAll()
            .addOnSuccessListener { (items, prefixes) ->
                listItems += items.map { it.name }
                prefixes.forEach { listFiles(it) }
            }
            .addOnFailureListener {
                listItems = listOf("Failed to list files")
            }
    }

    listFiles(listRef)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Download Files",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(listItems) { item ->
                Button(onClick = { id = downloadFile(item) }) {
                    Text(text = item)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Text(text = id)
    }
}fun scanAndUploadOldFiles(context: Context, uris: List<Uri>) {
    uris.forEach { uri ->
        // Process each URI, check conditions, and create new URIs as needed
        val lastModified = File(uri.path ?: "").lastModified()
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - lastModified
        val daysDifference = timeDifference / (1000 * 60 * 60 * 24) // Convert milliseconds to days

        if (daysDifference >= 0) {
            // Upload the file to Firebase Storage using its URI
            uploadFile(uri, context)

        }
    }
}

@Composable
fun UploadScreen() {
    val context = LocalContext.current
    val selectedUris = remember { mutableStateListOf<Uri>() }

    val filePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetMultipleContents()) { uris: List<Uri>? ->
        uris?.let {
            selectedUris.addAll(it)
            scanAndUploadOldFiles(context, selectedUris)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Button(onClick = { filePickerLauncher.launch("*/*") }) {
            Text("Select File(s) to Upload")
        }
    }
}

fun downloadFile(url: String): String {
    var result = ""
    val cloudStorage = Firebase.storage.reference
    val storageRef = cloudStorage.child(url)
    val fileName = url.split("/").last()

    val downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val file = File(downloadPath, fileName)

    storageRef.getFile(file)
        .addOnSuccessListener {
            // File has been downloaded and saved to the Downloads folder
            result = "File downloaded successfully to ${file.absolutePath}"
        }
        .addOnFailureListener {
            result = "Failed to download file"
        }

    return result
}



fun uploadFile(uri: Uri, context: Context) {
    val storage = Firebase.storage
    val storageRef = storage.reference
    val fileRef = storageRef.child("Backup/${uri.lastPathSegment}")

    val inputStream = context.contentResolver.openInputStream(uri)
    inputStream?.let { stream ->
        fileRef.putStream(stream)
            .addOnSuccessListener {
                // File uploaded successfully
                println("File uploaded successfully: ${uri.lastPathSegment}")
            }
            .addOnFailureListener { exception ->
                // Handle unsuccessful upload
                println("Failed to upload file: ${uri.lastPathSegment}, Error: $exception")
            }
    }
}




@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DataBackupAppTheme {
        MainScreen()
    }
}