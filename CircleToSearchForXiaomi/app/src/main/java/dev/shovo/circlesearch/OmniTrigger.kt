package dev.shovo.circlesearch

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * Invokes Google's native Circle to Search through Android's Voice Interaction
 * Manager Service. This follows the rootless VIS invocation route documented
 * by upstream MiCTS; it does not spoof device properties or bypass Google's
 * device eligibility checks.
 */
internal object OmniTrigger {
    private const val TAG = "OmniTrigger"
    private const val GOOGLE_PACKAGE = "com.google.android.googlequicksearchbox"
    private const val ENTRY_POINT = 1
    private const val SHOW_SESSION_FLAGS = 7
    private const val CALLER = "hyperOS_home"

    sealed class Result {
        data object Triggered : Result()
        data object GoogleNotDefault : Result()
        data object Rejected : Result()
        data class Failed(val reason: String) : Result()
    }

    fun invoke(context: Context): Result {
        val assistant = Settings.Secure.getString(
            context.contentResolver,
            "assistant"
        )
        if (!isGoogleAssistant(assistant)) {
            Log.w(TAG, "Google is not the default Digital Assistant: " + assistant)
            return Result.GoogleNotDefault
        }

        return try {
            val bundle = Bundle().apply {
                putLong("invocation_time_ms", SystemClock.elapsedRealtime())
                putInt("omni.entry_point", ENTRY_POINT)
            }

            val managerInterface = Class.forName(
                "com.android.internal.app.IVoiceInteractionManagerService"
            )
            val serviceManager = Class.forName("android.os.ServiceManager")
            val binder = serviceManager.getMethod("getService", String::class.java)
                .invoke(null, "voiceinteraction") as? IBinder
                ?: return Result.Failed("Android Voice Interaction service is unavailable")

            val stubClass = Class.forName(
                "com.android.internal.app.IVoiceInteractionManagerService\$Stub"
            )
            val manager = stubClass.getMethod("asInterface", IBinder::class.java)
                .invoke(null, binder)
                ?: return Result.Failed("Could not connect to Android Voice Interaction service")

            val plan = InvocationPlan.forApi(Build.VERSION.SDK_INT)
            val accepted = if (plan.caller != null) {
                HiddenApiBypass.invoke(
                    managerInterface, manager, "showSessionFromSession",
                    null, bundle, SHOW_SESSION_FLAGS, plan.caller
                ) as? Boolean
            } else {
                HiddenApiBypass.invoke(
                    managerInterface, manager, "showSessionFromSession",
                    null, bundle, SHOW_SESSION_FLAGS
                ) as? Boolean
            } ?: false

            Log.i(TAG, "VIS invocation result=" + accepted + ", SDK=" + Build.VERSION.SDK_INT)
            if (accepted) Result.Triggered else Result.Rejected
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to invoke native Google Circle to Search.", e)
            Result.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    internal fun isGoogleAssistant(component: String?): Boolean {
        return component?.substringBefore('/')?.equals(GOOGLE_PACKAGE, ignoreCase = true) == true
    }

    internal data class InvocationPlan(val apiLevel: Int, val caller: String?) {
        val argumentCount: Int
            get() = if (caller == null) 3 else 4

        companion object {
            fun forApi(apiLevel: Int): InvocationPlan {
                return InvocationPlan(
                    apiLevel = apiLevel,
                    caller = if (apiLevel >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) CALLER else null
                )
            }
        }
    }
}
