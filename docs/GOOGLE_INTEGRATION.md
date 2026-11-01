# Google Native Circle to Search Integration

The app uses the Voice Interaction Service (VIS) invocation route documented by the upstream MiCTS project:

1. Require Google to be the selected default Digital Assistant.
2. Obtain Android's voiceinteraction Binder service.
3. Invoke IVoiceInteractionManagerService.showSessionFromSession through HiddenApiBypass.
4. Pass invocation_time_ms and omni.entry_point = 1; on Android 14+ use the hyperOS_home caller argument.
5. Let Google render its native Circle to Search UI.

This does not create a local crop screen or send an image to Lens. Google must decide that Circle to Search is available on the device. A successful Binder return only confirms that Android accepted the VIS invocation; it is not proof of a physical Back Tap or a successful Google UI render.

If Google rejects the request with Omni invocation failed: not enabled, the app cannot override Google's device whitelist or server-side feature flag on a normal unrooted device. See the upstream MiCTS instructions at https://github.com/parallelcc/MiCTS/blob/main/README_en.md.
