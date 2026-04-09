package com.example.urbaneye.ui.screens.agent

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.urbaneye.ui.theme.UrbanEyeColors
import com.example.urbaneye.ui.utils.SetStatusBarColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

data class ChatMessage(val text: String, val isUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var input by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    var isListening by remember { mutableStateOf(false) }

    // Unique session ID to maintain conversation state
    val sessionId = remember { UUID.randomUUID().toString() }

    val messages = remember {
        mutableStateListOf(
            ChatMessage("Hello Harsh! I'm your UrbanEye AI. Tap the mic to speak or type your report below.", false)
        )
    }

    // --- Speech Recognition Logic ---
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val speechIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    fun send(textToSend: String) {
        if (textToSend.isBlank() || isTyping) return
        messages.add(ChatMessage(textToSend.trim(), isUser = true))
        input = ""
        isTyping = true
        isListening = false
        scope.launch {
            delay(100)
            listState.animateScrollToItem(messages.lastIndex)

            val reply = getChatBotResponse(textToSend, sessionId)

            isTyping = false
            messages.add(ChatMessage(reply, isUser = false))
            delay(100)
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val recognitionListener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) { isListening = false }
            override fun onResults(results: Bundle?) {
                val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val recognizedText = data?.get(0) ?: ""
                if (recognizedText.isNotBlank()) {
                    input = recognizedText
                    send(recognizedText)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val data = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                input = data?.get(0) ?: ""
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose { speechRecognizer.destroy() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListening = true
            speechRecognizer.startListening(speechIntent)
        }
    }

    fun handleVoiceClick() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        } else {
            val permissionCheckResult = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
                isListening = true
                speechRecognizer.startListening(speechIntent)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    SetStatusBarColor(backgroundColor = MaterialTheme.colorScheme.background, darkIcons = !com.example.urbaneye.ui.theme.LocalIsDarkTheme.current)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopBarUI(onClear = {
                messages.clear()
                messages.add(ChatMessage("Conversation reset. How can I help?", false))
            })
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(messages) { msg -> ChatBubble(msg) }
                    if (isTyping) item { TypingIndicator() }
                }

                InputAreaUI(
                    input = input,
                    onValueChange = { input = it },
                    isTyping = isTyping,
                    isListening = isListening,
                    onSend = { send(input) },
                    onVoiceClick = { handleVoiceClick() }
                )
            }

            AnimatedVisibility(
                visible = isListening,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                VoicePulseOverlay(onClose = {
                    speechRecognizer.stopListening()
                    isListening = false
                })
            }
        }
    }
}

/**
 * ChatBot.com API Implementation
 */
private suspend fun getChatBotResponse(queryText: String, sessionId: String): String = withContext(Dispatchers.IO) {
    // TODO: Paste your Developer Access Token here
    val developerAccessToken = "ANnP2_ZzaqFuMi0f8FhSHbX0HY3eINqo"

    // TODO: Paste your Bot ID here (Found in your Bot Settings or URL: app.chatbot.com/bot/{BOT_ID}/stories)
    val botId = "69d81cb40ee62a000891445b"

    val url = "https://api.chatbot.com/v2/query"

    // Basic verification to help user avoid obvious errors
    if (developerAccessToken.contains("YOUR") || botId.contains("YOUR")) {
        return@withContext "Configuration Error: Please set your Developer Access Token and Bot ID in the code."
    }

    try {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Authorization", "Bearer $developerAccessToken")
            setRequestProperty("X-Bot-Id", botId) // CRITICAL: Required for Developer Tokens
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            doOutput = true
            connectTimeout = 10000
            readTimeout = 15000
        }

        val body = JSONObject().apply {
            put("query", queryText)
            put("sessionId", sessionId)
        }

        conn.outputStream.use { it.write(body.toString().toByteArray()) }

        val responseCode = conn.responseCode
        if (responseCode == 200) {
            val responseText = conn.inputStream.bufferedReader().readText()
            val jsonResponse = JSONObject(responseText)

            // Extracting response from result.responses array
            val result = jsonResponse.optJSONObject("result")
            val responses = result?.optJSONArray("responses")

            if (responses != null && responses.length() > 0) {
                // V2 responses can be multiple, we take the first text content
                val firstResponse = responses.getJSONObject(0)
                firstResponse.optString("content", "I processed your request, but the response format was unexpected.")
            } else {
                "The bot is reachable, but didn't return a message. Please verify your Bot Stories."
            }
        } else {
            val errorResponse = conn.errorStream?.bufferedReader()?.readText() ?: "No error body"
            when (responseCode) {
                401 -> "Authentication Failed: Check if your Developer Token is still valid."
                404 -> "Error 404: Bot Not Found. 1. Check your Bot ID. 2. Ensure your bot is 'Published'."
                else -> "Neural Link Error ($responseCode): $errorResponse"
            }
        }
    } catch (e: Exception) {
        "Signal Lost: ${e.localizedMessage}. Check your internet connection."
    }
}

@Composable
fun TopBarUI(onClear: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(listOf(UrbanEyeColors.ElectricCyan, UrbanEyeColors.MagentaPop))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "UrbanEye AI",
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Neural Link Active",
                    style = MaterialTheme.typography.labelSmall,
                    color = UrbanEyeColors.ElectricCyan
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = onClear,
                modifier = Modifier.background(MaterialTheme.colorScheme.outline, CircleShape).size(36.dp)
            ) {
                Icon(Icons.Rounded.Refresh, "Reset", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun InputAreaUI(
    input: String,
    onValueChange: (String) -> Unit,
    isTyping: Boolean,
    isListening: Boolean,
    onSend: () -> Unit,
    onVoiceClick: () -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 12.dp,
            shape = RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp),
        ) {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) UrbanEyeColors.SunsetCrimson
                            else UrbanEyeColors.IndigoPunch
                        )
                        .clickable { onVoiceClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        contentDescription = "Voice",
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                OutlinedTextField(
                    value = input,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    enabled = !isTyping && !isListening,
                    shape = CircleShape,
                    placeholder = { Text("Ask anything...", color = UrbanEyeColors.Gray600) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                        focusedBorderColor = UrbanEyeColors.ElectricCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    maxLines = 1
                )

                if (input.isNotBlank() && !isListening) {
                    Spacer(Modifier.width(10.dp))
                    IconButton(
                        onClick = onSend,
                        enabled = !isTyping,
                        modifier = Modifier
                            .size(48.dp)
                            .background(UrbanEyeColors.ElectricCyan, CircleShape)
                    ) {
                        Icon(Icons.Rounded.Send, "Send", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
    }
}

@Composable
fun VoicePulseOverlay(onClose: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearOutSlowInEasing), RepeatMode.Restart), label = ""
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearOutSlowInEasing), RepeatMode.Restart), label = ""
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(Modifier.size(140.dp).scale(scale).background(UrbanEyeColors.ElectricCyan.copy(alpha = alpha), CircleShape))

            Surface(
                onClick = onClose,
                shape = CircleShape,
                color = UrbanEyeColors.ElectricCyan,
                shadowElevation = 20.dp,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Mic, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(48.dp))
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        Surface(
            color = MaterialTheme.colorScheme.outline,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, UrbanEyeColors.ElectricCyan.copy(alpha = 0.3f))
        ) {
            Text(
                "Listening to Harsh Deep...",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(UrbanEyeColors.ElectricCyan, UrbanEyeColors.IndigoPunch))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
        }

        Surface(
            color = if (isUser) UrbanEyeColors.IndigoPunch else MaterialTheme.colorScheme.surface,
            border = if (!isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    lineHeight = 22.sp
                )
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    val anim = rememberInfiniteTransition(label = "")
    val alpha by anim.animateFloat(0.3f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "")

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 46.dp)) {
        Box(Modifier.size(6.dp).background(UrbanEyeColors.ElectricCyan, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            "AI is processing...",
            style = MaterialTheme.typography.labelSmall,
            color = UrbanEyeColors.ElectricCyan.copy(alpha = alpha)
        )
    }
}