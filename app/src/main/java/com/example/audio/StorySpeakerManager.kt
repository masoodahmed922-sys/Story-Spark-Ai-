package com.example.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.ai.StoryTranslationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class WomanVoiceStyle(val displayName: String, val description: String, val pitch: Float, val speed: Float) {
    WARM_STORYTELLER(
        displayName = "Elena (Warm Storyteller)",
        description = "Natural, soothing feminine tone for bedtime & daytime stories",
        pitch = 1.22f,
        speed = 0.92f
    ),
    SOFT_BEDTIME(
        displayName = "Clara (Soft Bedtime)",
        description = "Gentle, whisper-soft feminine cadence for sleepy bedtime reading",
        pitch = 1.30f,
        speed = 0.82f
    ),
    EXPRESSIVE_LIVELY(
        displayName = "Maya (Expressive & Lively)",
        description = "Bright, cheerful feminine voice perfect for adventures & comedy",
        pitch = 1.18f,
        speed = 0.98f
    )
}

class StorySpeakerManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _currentStyle = MutableStateFlow(WomanVoiceStyle.WARM_STORYTELLER)
    val currentStyle: StateFlow<WomanVoiceStyle> = _currentStyle.asStateFlow()

    private val _speechRateMultiplier = MutableStateFlow(1.0f)
    val speechRateMultiplier: StateFlow<Float> = _speechRateMultiplier.asStateFlow()

    private val _currentLanguageCode = MutableStateFlow("en")
    val currentLanguageCode: StateFlow<String> = _currentLanguageCode.asStateFlow()

    private val _voiceWarning = MutableStateFlow<String?>(null)
    val voiceWarning: StateFlow<String?> = _voiceWarning.asStateFlow()

    private var onSpeechDoneCallback: (() -> Unit)? = null

    init {
        initializeTts()
    }

    private fun initializeTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                applyVoiceConfig(_currentLanguageCode.value)
                setupProgressListener()
            }
        }
    }

    fun setLanguage(langCode: String) {
        _currentLanguageCode.value = langCode
        applyVoiceConfig(langCode)
    }

    private fun getLocaleForCode(code: String): Locale = when (code.lowercase()) {
        "hi" -> Locale("hi", "IN")
        "ur" -> Locale("ur", "PK")
        "ar" -> Locale("ar")
        "zh" -> Locale("zh", "CN")
        "fr" -> Locale("fr", "FR")
        "ja" -> Locale("ja", "JP")
        "ru" -> Locale("ru", "RU")
        else -> Locale("en", "US")
    }

    private fun applyVoiceConfig(langCode: String) {
        val speech = tts ?: return
        val targetLocale = getLocaleForCode(langCode)
        try {
            val availability = speech.isLanguageAvailable(targetLocale)
            if (availability == TextToSpeech.LANG_MISSING_DATA || availability == TextToSpeech.LANG_NOT_SUPPORTED) {
                _voiceWarning.value = "Voice for ${StoryTranslationService.getLanguageNativeName(langCode)} is not installed on this device."
                return
            } else {
                _voiceWarning.value = null
                speech.language = targetLocale
            }

            // Find female or native voice for the selected locale
            val availableVoices = speech.voices
            val femaleVoice: Voice? = availableVoices?.firstOrNull { voice ->
                voice.locale.language.equals(targetLocale.language, ignoreCase = true) &&
                    (voice.name.lowercase().contains("female") ||
                     voice.name.lowercase().contains("woman") ||
                     voice.features.any { it.contains("female", ignoreCase = true) })
            } ?: availableVoices?.firstOrNull { voice ->
                voice.locale.language.equals(targetLocale.language, ignoreCase = true)
            }

            if (femaleVoice != null) {
                speech.voice = femaleVoice
            }

            val style = _currentStyle.value
            val rate = style.speed * _speechRateMultiplier.value
            speech.setPitch(style.pitch)
            speech.setSpeechRate(rate)
        } catch (_: Exception) {
            // Graceful fallback
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                onSpeechDoneCallback?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isSpeaking.value = false
            }
        })
    }

    fun setVoiceStyle(style: WomanVoiceStyle) {
        _currentStyle.value = style
        applyVoiceConfig(_currentLanguageCode.value)
    }

    fun setRateMultiplier(multiplier: Float) {
        _speechRateMultiplier.value = multiplier
        applyVoiceConfig(_currentLanguageCode.value)
    }

    fun speak(text: String, languageCode: String = _currentLanguageCode.value, onDone: (() -> Unit)? = null) {
        if (!isInitialized || tts == null) {
            initializeTts()
        }

        onSpeechDoneCallback = onDone
        _currentLanguageCode.value = languageCode
        applyVoiceConfig(languageCode)

        val utteranceId = "StorySpark_Speak_${System.currentTimeMillis()}"
        val cleanText = text.replace(Regex("[#*`_]"), "").trim()
        if (cleanText.isNotBlank()) {
            tts?.speak(cleanText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            _isSpeaking.value = true
        }
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
