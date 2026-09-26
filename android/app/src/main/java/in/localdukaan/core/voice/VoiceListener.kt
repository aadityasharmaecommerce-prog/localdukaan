package `in`.localdukaan.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** Mic se Hindi/Hinglish suno — Google STT + hamare rules (koi AI/cloud-key nahi).
 *  Net na ho to on-device try (Hindi voice-pack hona chahiye), warna NEEDS_NET error. */
sealed interface VoiceListenResult {
 data class Heard(val text: String, val final: Boolean) : VoiceListenResult
 data class Failed(val reason: VoiceFail) : VoiceListenResult
}
enum class VoiceFail { NOT_SUPPORTED, NO_MIC, NEEDS_NET, NO_SPEECH, BUSY }

class VoiceListener(private val context: Context) {
 private var recognizer: SpeechRecognizer? = null
 private var running = false

 fun available(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

 fun start(onResult: (VoiceListenResult) -> Unit) {
  if (running) { onResult(VoiceListenResult.Failed(VoiceFail.BUSY)); return }
  if (!available()) { onResult(VoiceListenResult.Failed(VoiceFail.NOT_SUPPORTED)); return }
  val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
   putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
   putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
   putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("hi-IN", "en-IN"))
   putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
   putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
   putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
  }
  val sr = SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
  running = true
  sr.setRecognitionListener(object : RecognitionListener {
   override fun onReadyForSpeech(p: Bundle?) {}
   override fun onBeginningOfSpeech() {}
   override fun onRmsChanged(rmsdB: Float) {}
   override fun onBufferReceived(b: ByteArray?) {}
   override fun onEndOfSpeech() {}
   override fun onEvent(type: Int, p: Bundle?) {}
   override fun onPartialResults(r: Bundle?) {
    r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
     ?.takeIf { it.isNotBlank() }?.let { onResult(VoiceListenResult.Heard(it, false)) }
   }
   override fun onResults(r: Bundle?) {
    stop()
    val text = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
    if (text.isBlank()) onResult(VoiceListenResult.Failed(VoiceFail.NO_SPEECH))
    else onResult(VoiceListenResult.Heard(text, true))
   }
   override fun onError(code: Int) {
    stop()
    onResult(VoiceListenResult.Failed(when (code) {
     SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_SERVER -> VoiceFail.NEEDS_NET
     SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceFail.NO_MIC
     SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceFail.NO_SPEECH
     else -> VoiceFail.NO_SPEECH
    }))
   }
  })
  sr.startListening(intent)
 }

 fun stop() {
  running = false
  runCatching { recognizer?.stopListening(); recognizer?.destroy() }
  recognizer = null
 }
}
