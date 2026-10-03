package com.example.jieshuovoiceassistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var prefs: SharedPreferences
    private lateinit var keyInput: EditText
    private lateinit var result: EditText
    private lateinit var status: TextView
    private lateinit var tts: TextToSpeech
    private var recognizer: SpeechRecognizer? = null
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        tts = TextToSpeech(this, this)
        buildUi()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 10)
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }
        val title = TextView(this).apply {
            text = "Jieshuo AI Voice Assistant"
            textSize = 24f
        }
        status = TextView(this).apply {
            text = "Ready"
            textSize = 16f
        }
        keyInput = EditText(this).apply {
            hint = "Gemini API key (keep private)"
            inputType = 0x81
            setText(prefs.getString("api_key", ""))
        }
        val saveKey = Button(this).apply {
            text = "Save API Key"
            setOnClickListener {
                prefs.edit().putString("api_key", keyInput.text.toString().trim()).apply()
                status.text = "API key saved on this device."
            }
        }
        val listen = Button(this).apply {
            text = "🎙 Speak"
            setOnClickListener { startListening() }
        }
        val format = Button(this).apply {
            text = "✨ Improve with Gemini"
            setOnClickListener { improveWithGemini() }
        }
        val speak = Button(this).apply {
            text = "🔊 Read result"
            setOnClickListener { speakResult() }
        }
        val copy = Button(this).apply {
            text = "📋 Copy"
            setOnClickListener { copyText() }
        }
        val paste = Button(this).apply {
            text = "⌨ Paste into focused field"
            setOnClickListener { requestPaste() }
        }
        result = EditText(this).apply {
            hint = "Recognized / prepared text"
            minLines = 5
            gravity = android.view.Gravity.TOP
        }
        root.addView(title)
        root.addView(status)
        root.addView(keyInput)
        root.addView(saveKey)
        root.addView(listen)
        root.addView(format)
        root.addView(speak)
        root.addView(copy)
        root.addView(paste)
        root.addView(result, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Speech recognition is not available on this phone."
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer!!.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { status.text = "Listening…" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { status.text = "Processing speech…" }
            override fun onError(error: Int) { status.text = "Speech error: $error" }
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                result.setText(text)
                status.text = "Speech recognized. You can improve it with Gemini."
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        recognizer!!.startListening(intent)
    }

    private fun improveWithGemini() {
        val apiKey = prefs.getString("api_key", "").orEmpty()
        val input = result.text.toString().trim()
        if (apiKey.isBlank()) { status.text = "Enter and save your Gemini API key first."; return }
        if (input.isBlank()) { status.text = "Speak or enter some text first."; return }

        status.text = "Gemini is preparing the text…"
        executor.execute {
            try {
                val prompt = """
You are a multilingual writing assistant.
Rewrite the user's spoken text into natural, professional text while preserving the user's intended meaning.
Detect the language automatically.
- Marathi speech: output Marathi.
- Hindi speech: output Hindi.
- English speech: output English.
- Mixed Marathi/Hindi/English: preserve the natural mixed-language style unless a phrase clearly belongs in another language.
Do not translate just for the sake of translating.
Fix grammar, punctuation and obvious speech-to-text errors.
Add an emoji only when it naturally fits the context; never add many emojis.
Return ONLY the final text, with no explanation, quotation marks, or headings.

User text:
$input
""".trimIndent()

                val body = JSONObject()
                    .put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(
                            JSONObject().put("text", prompt)
                        ))
                    ))

                val url = URL(
                    "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "gemini-2.5-flash:generateContent?key=" + apiKey
                )
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val responseCode = conn.responseCode
                val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                if (responseCode !in 200..299) throw Exception("HTTP $responseCode: $response")

                val json = JSONObject(response)
                val text = json.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text").trim()

                runOnUiThread {
                    result.setText(text)
                    status.text = "Ready. Review, copy, or paste."
                    speakResult()
                }
            } catch (e: Exception) {
                runOnUiThread { status.text = "Gemini error: ${e.message}" }
            }
        }
    }

    private fun speakResult() {
        val text = result.text.toString().trim()
        if (text.isNotEmpty()) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "result")
    }

    private fun copyText() {
        val text = result.text.toString()
        val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.setPrimaryClip(android.content.ClipData.newPlainText("AI text", text))
        status.text = "Copied to clipboard."
    }

    private fun requestPaste() {
        startService(Intent(this, PasteAccessibilityService::class.java).setAction(PasteAccessibilityService.ACTION_PASTE))
        status.text = "Paste requested. Make sure the desired text field is focused."
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts.language = Locale.getDefault()
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts.shutdown()
        executor.shutdownNow()
        super.onDestroy()
    }
}
