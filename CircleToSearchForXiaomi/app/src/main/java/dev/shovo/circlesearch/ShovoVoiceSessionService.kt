package dev.shovo.circlesearch

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

/**
 * Service managing VoiceInteractionSession lifecycles for Shovo Circle Search.
 */
class ShovoVoiceSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return ShovoVoiceSession(this)
    }
}

