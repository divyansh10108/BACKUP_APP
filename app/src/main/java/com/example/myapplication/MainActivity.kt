package com.example.myapplication
import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Collections
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var mDriveService: Drive

    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DriveUploaderApp()
        }
        requestSignIn()
    }


    private fun requestSignIn() {
        val clientId = "865701038288-7s9ri2j341h5htkafc29556gg2p73sd3.apps.googleusercontent.com"
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .requestIdToken(clientId) // Use your client ID here
            .build()
        val client: GoogleSignInClient = GoogleSignIn.getClient(this, signInOptions)

        val account = GoogleSignIn.getLastSignedInAccount(this)
        account?.let { handleSignInResult(it) } ?: startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    private fun handleSignInResult(account: GoogleSignInAccount) {
        val credential = com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential.usingOAuth2(
            this, Collections.singleton(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        mDriveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential
        )
            .setApplicationName("Drive API Migration")
            .build()
    }

    @Composable
    @ExperimentalMaterialApi
    fun DriveUploaderApp() {
        MaterialTheme {
            Surface {
                DriveUploadButton()
            }
        }
    }

    @Composable
    fun DriveUploadButton() {
        val context = LocalContext.current
        Button(
            onClick = {
                uploadAllTextFilesToDrive(context)
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Upload Text Files to Drive")
        }
    }

    private fun uploadAllTextFilesToDrive(context: android.content.Context) {
        val contentResolver = context.contentResolver
        val textFiles = mutableListOf<Uri>()

        // Fetch all available text files
        val cursor = contentResolver.query(
            android.provider.MediaStore.Files.getContentUri("external"),
            null,
            null,
            null,
            null
        )
        cursor?.use { c ->
            val mimeTypeColumnIndex =
                c.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.MIME_TYPE)
            val dataColumnIndex =
                c.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA)

            while (c.moveToNext()) {
                val mimeType = c.getString(mimeTypeColumnIndex)
                val filePath = c.getString(dataColumnIndex)
                if (mimeType != null && mimeType.startsWith("text/") && filePath != null) {
                    textFiles.add(Uri.parse("file://$filePath"))
                }
            }
        }

        // Upload each text file to Google Drive
        textFiles.forEach { uri ->
            val name = getFileName(contentResolver, uri)
            val content = readTextFile(contentResolver, uri)
            saveFile(null, name, content)
                .addOnSuccessListener {
                    showToast("Uploaded $name successfully!")
                }
                .addOnFailureListener { exception ->
                    showToast("Error uploading $name: ${exception.message}")
                }
        }
    }

    private fun getFileName(contentResolver: ContentResolver, uri: Uri): String {
        var fileName = "Untitled"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex =
                cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            fileName = cursor.getString(nameIndex)
        }
        return fileName
    }

    private fun readTextFile(contentResolver: ContentResolver, uri: Uri): String {
        val stringBuilder = StringBuilder()
        contentResolver.openInputStream(uri)?.use { inputStream ->
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String? = reader.readLine()
            while (line != null) {
                stringBuilder.append(line).append("\n")
                line = reader.readLine()
            }
        }
        return stringBuilder.toString()
    }

    private fun saveFile(fileId: String?, name: String, content: String): Task<Void> {
        return Tasks.call(Executors.newSingleThreadExecutor(), {
            val metadata = File().setName(name)
            val contentStream = ByteArrayContent.fromString("text/plain", content)
            if (fileId == null) {
                mDriveService.files().create(metadata, contentStream).execute()
            } else {
                mDriveService.files().update(fileId, metadata, contentStream).execute()
            }
            null
        })
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val REQUEST_CODE_SIGN_IN = 1
    }
}
