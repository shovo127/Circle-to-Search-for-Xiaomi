package dev.shovo.circlesearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OmniTriggerPlanTest {
    @Test
    fun api33UsesLegacySignature() {
        val plan = OmniTrigger.InvocationPlan.forApi(33)
        assertEquals(3, plan.argumentCount)
        assertEquals(null, plan.caller)
    }

    @Test
    fun api34UsesHyperOsCallerSignature() {
        val plan = OmniTrigger.InvocationPlan.forApi(34)
        assertEquals(4, plan.argumentCount)
        assertEquals("hyperOS_home", plan.caller)
    }

    @Test
    fun api36UsesHyperOsCallerSignature() {
        val plan = OmniTrigger.InvocationPlan.forApi(36)
        assertEquals(4, plan.argumentCount)
        assertEquals("hyperOS_home", plan.caller)
    }

    @Test
    fun recognizesGoogleAssistantComponent() {
        assertTrue(OmniTrigger.isGoogleAssistant(
            "com.google.android.googlequicksearchbox/com.google.android.voiceinteraction.GsaVoiceInteractionService"
        ))
        assertTrue(OmniTrigger.isGoogleAssistant("com.google.android.googlequicksearchbox"))
    }

    @Test
    fun rejectsMissingOrNonGoogleAssistant() {
        assertFalse(OmniTrigger.isGoogleAssistant(null))
        assertFalse(OmniTrigger.isGoogleAssistant("com.miui.voiceassist/.VoiceService"))
        assertFalse(OmniTrigger.isGoogleAssistant("com.google.android.apps.nexuslauncher/.Assistant"))
    }
}
