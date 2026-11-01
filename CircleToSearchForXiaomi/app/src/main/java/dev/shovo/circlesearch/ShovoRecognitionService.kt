package dev.shovo.circlesearch

import android.content.Intent
import android.speech.RecognitionService

/**
 * Lightweight stub RecognitionService required by Android framework (attrs.xml)
 * so that VoiceInteractionService is treated as a fully valid system digital assistant
 * without Android framework resetting the assistant setting to null.
 */
class ShovoRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) = Unit
    override fun onCancel(listener: Callback?) = Unit
    override fun onStopListening(listener: Callback?) = Unit
}

