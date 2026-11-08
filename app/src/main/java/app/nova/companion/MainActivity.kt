package app.nova.companion

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File

private val Navy = Color(0xFF101A2B)
private val Teal = Color(0xFF70C8C0)

class MainActivity : ComponentActivity() {
    private lateinit var dao: NovaDao
    private lateinit var brain: LocalBrain
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dao = NovaDatabase.get(this).dao()
        brain = LocalBrain(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Teal, background = Navy, surface = Color(0xFF1A2940))) {
                NovaScreen(dao, brain, { uri -> copyModel(uri) }, { brain.load() }, { text -> sendToNova(text) })
            }
        }
    }
    private fun copyModel(uri: Uri) {
        contentResolver.openInputStream(uri)?.use { input ->
            File(filesDir, "nova-model.litertlm").outputStream().use { output -> input.copyTo(output) }
        }
    }
    private suspend fun sendToNova(text: String): String {
        dao.addMessage(ChatMessage(role = "user", content = text))
        val answer = brain.reply(text, dao.memoryContext())
        dao.addMessage(ChatMessage(role = "nova", content = answer))
        brain.extractMemory("User: $text\\nNova: $answer").forEach {
            dao.addMemory(Memory(text = it.text, category = it.category, confidence = it.confidence))
        }
        return answer
    }
    override fun onDestroy() { brain.close(); super.onDestroy() }
}

@Composable
private fun NovaScreen(
    dao: NovaDao, brain: LocalBrain, onImportModel: (Uri) -> Unit,
    onLoadModel: suspend () -> String, onSend: suspend (String) -> String
) {
    var tab by remember { mutableStateOf(0) }
    var input by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Import a compatible local model to begin.") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val messages by dao.recentMessages().collectAsState(initial = emptyList())
    val memories by dao.allMemories().collectAsState(initial = emptyList())
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { onImportModel(uri); status = "Model copied. Tap Load model." }
    }
    Column(Modifier.fillMaxSize().background(Navy).padding(16.dp)) {
        Text("NOVA", style = MaterialTheme.typography.headlineLarge, color = Teal)
        Text("Your private, local companion", color = Color.LightGray)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { picker.launch(arrayOf("*/*")) }) { Text("Import model") }
            Button(enabled = !busy, onClick = {
                busy = true
                scope.launch { status = try { onLoadModel() } catch (e: Exception) { "Model error: ${e.message}" }; busy = false }
            }) { Text("Load model") }
        }
        Text(status, color = Teal, modifier = Modifier.padding(vertical = 8.dp))
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Chat") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Memories (${memories.size})") })
        }
        if (tab == 0) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), reverseLayout = true) {
                items(messages) { m ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)
                        .background(if (m.role == "nova") Color(0xFF20364A) else Color(0xFF263047), RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Text(if (m.role == "nova") "Nova" else "Void", color = Teal)
                        Text(m.content, color = Color.White)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BasicTextField(value = input, onValueChange = { input = it },
                    modifier = Modifier.weight(1f).background(Color(0xFF263047), RoundedCornerShape(12.dp)).padding(12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    decorationBox = { inner -> if (input.isEmpty()) Text("Talk to Nova…", color = Color.Gray); inner() })
                Button(enabled = !busy && input.isNotBlank(), onClick = {
                    val prompt = input.trim(); input = ""; busy = true
                    scope.launch {
                        try { onSend(prompt); status = "Ready." }
                        catch (e: Exception) { status = "Chat error: ${e.message}" }
                        finally { busy = false }
                    }
                }) { Text("Send") }
            }
        } else {
            LazyColumn(Modifier.weight(1f)) {
                items(memories) { memory ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp).background(Color(0xFF20364A), RoundedCornerShape(10.dp)).padding(12.dp)) {
                        Text(memory.category.uppercase(), color = Teal, style = MaterialTheme.typography.labelSmall)
                        Text(memory.text, color = Color.White)
                        Text("${memory.confidence} • memory suggestion", color = Color.LightGray, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
