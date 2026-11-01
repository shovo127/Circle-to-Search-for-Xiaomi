# Activation

## One supported activation path

**Double Back Tap → launch Circle to Search for Xiaomi → invoke Google native Circle to Search.**

HyperOS owns the physical back-tap sensor and the list of actions it offers. Select the app under **Open app** only when that item exists in the Back Tap menu on the installed firmware.

The app launcher is a transparent trampoline. It does not display a dashboard or intercept screen touches. It invokes the Android Voice Interaction Manager Service VIS entry point and exits.

## Requirements and limitations

- Google must be selected as the default Digital Assistant.
- Google's native Circle to Search must be enabled for the device/account/region.
- If the Google service returns not enabled or rejects the request, the app cannot enable Google's server-side/device eligibility flag without the required system/root-level modification.
- No Power-button long press, bottom navigation hold, custom accessibility overlay, or Quick Settings tile is part of this build.
