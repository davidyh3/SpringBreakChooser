package com.example.springbreakchooser

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, SensorEventListener {

    // for sensor
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // for speaker and converter
    private lateinit var editText: EditText
    private lateinit var tts: TextToSpeech
    private var hasGreeted = false

    // for language implemented
    private val languageOptions = arrayOf("English", "Español", "Français", "中文", "日本語", "한국어")
    private val languageCodes = arrayOf("en-US", "es-ES", "fr-FR", "zh-CN", "ja-JP", "ko-KR")
    private lateinit var selectedLanguageCode: String
    private val destinationOptions = mapOf(
        "English" to listOf("geo:0,0?q=Boston", "geo:0,0?q=Los Angeles"),
        "Español" to listOf("geo:0,0?q=Barcelona", "geo:0,0?q=Mexico City"),
        "Français" to listOf("geo:0,0?q=Paris", "geo:0,0?q=Montreal"),
        "中文" to listOf("geo:0,0?q=Beijing", "geo:0,0?q=Shanghai"),
        "日本語" to listOf("geo:0,0?q=Tokyo", "geo:0,0?q=Kyoto"),
        "한국어" to listOf("geo:0,0?q=Seoul", "geo:0,0?q=Busan")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        editText = findViewById(R.id.editText)
        val languageSpinner = findViewById<Spinner>(R.id.language_spinner)
        languageSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, languageOptions)

        tts = TextToSpeech(this, this)

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View, position: Int, id: Long) {
                selectedLanguageCode = languageCodes[position]
                promptSpeechInput() // Prompt the user to speak
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        hasGreeted = false
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.stop()
        tts.shutdown()
    }

    private val speechRecognizerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val spokenText: String? =
                    result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        ?.let { it[0] }
                editText.setText(spokenText)
            }
        }

    private fun promptSpeechInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguageCode)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Please speak now...")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Speech recognition is not supported on this device.", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            val acceleration =
                Math.sqrt((it.values[0] * it.values[0] + it.values[1] * it.values[1] + it.values[2] * it.values[2]).toDouble())

            // Considering 9.81 m/s^2 as Earth's gravity, a significant shake would be a considerable deviation from this
            if (acceleration > 12 && !hasGreeted) {
                selectRandomDestination(selectedLanguageCode)
                hasGreeted = true
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not implemented but required
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // Determine if the selected language is supported by the TTS engine
            val result = tts.setLanguage(Locale(selectedLanguageCode))

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Log an error if the language data is missing or the language is not supported
                Log.e("TTS", "This language is not supported.")
            }
        } else {
            // Log an error if the TTS engine failed to initialize
            Log.e("TTS", "Initialization of the Text-to-Speech engine failed.")
        }
    }


    private fun selectRandomDestination(languageCode: String) {
        // Select a random city URI from the list
        destinationOptions[languageCode]?.let { destinations ->
            val destinationUri = destinations.random()
            openMap(destinationUri)
            val greetingMessage = getGreetingMessage(languageCode)
            tts.speak(greetingMessage, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun getGreetingMessage(languageCode: String): String {
        return when (languageCode) {
            "en-US" -> "Hello"
            "es-ES" -> "Hola"
            "fr-FR" -> "Bonjour"
            "zh-CN" -> "你好"
            "ja-JP" -> "こんにちは"
            "ko-KR" -> "안녕하세요"
            else -> "Hello"
        }
    }

    private fun openMap(locationUri: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(locationUri))
        startActivity(intent)
    }

}