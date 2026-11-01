package dev.shovo.circlesearch

import android.service.voice.VoiceInteractionService

/**
 * Official AOSP Digital Assistant Service for Shovo Circle Search.
 *
 * Enables Shovo Circle Search to be selected as the system-wide Default Digital Assistant
 * in Android / HyperOS settings (Settings -> Apps -> Default apps -> Digital assistant).
 * Allows native invocation via Power Button long-press, Home long-press, or Back Tap.
 */
class ShovoVoiceInteractionService : VoiceInteractionService()

