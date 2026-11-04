# RADHA AI — Android starter project

RADHA AI is an early-stage Android voice-assistant project. This repository contains the Android app source, voice interaction declarations, Gradle configuration, and a GitHub Actions workflow that builds a debug APK.

## Current implemented scope

- Android app screen and animated RADHA orb from the supplied Kotlin source.
- Speech-to-text using Android `SpeechRecognizer` (availability/language depend on the device and installed speech services).
- Text-to-speech using Android `TextToSpeech`.
- OpenAI Chat Completions API integration using `gpt-4o-mini` and tool calling.
- User-confirmed call flow, WhatsApp draft opening, app launching, web search, alarm request, flashlight request, and battery status.
- English, Marathi, and Hindi language choices in the supplied app code.

## Not implemented / not guaranteed

This is not a finished general-purpose agent. It cannot control every app or every phone setting. Android restricts background activity, messaging, calls, accessibility, permissions, and default-assistant behavior. WhatsApp messages are only drafted; the user must press Send. Calls require a user confirmation. Device support varies. The voice-interaction service and default-assistant integration must be tested on a real device.

## Build on GitHub (phone-friendly)

1. Download and extract `RADHA_AI_FULL.zip`.
2. Open GitHub in your browser and create a new repository named `RadhaAI`.
3. Upload the **contents of the inner `RadhaAI` folder** to the repository root. Do not upload the outer `RADHA_AI_FULL` folder itself.
4. Confirm that the repository root contains `settings.gradle.kts`, `build.gradle.kts`, `app/`, and `.github/workflows/build.yml`.
5. Open the **Actions** tab. Select **Build RADHA AI APK** and run it with **Run workflow** (or wait for the push build).
6. When the run completes successfully, open the run, scroll to **Artifacts**, and download `RADHA-AI-debug-apk`.
7. Extract the artifact ZIP and install `app-debug.apk` on your Android phone. Android may ask you to allow installation from that browser/file manager.

## Build with Android Studio

- Use Android Studio with JDK 17 and Android SDK Platform 34 installed.
- Open the inner `RadhaAI` folder as the project.
- Let Gradle sync, then choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
- The debug APK is created at `app/build/outputs/apk/debug/app-debug.apk`.

## API key and privacy warning

The supplied implementation currently asks for an OpenAI API key and stores it in app-private preferences. This is acceptable only for a personal prototype, not for public distribution: a key embedded/stored in a client app can be extracted or misused. Do not publish your key, put it in GitHub, send it in screenshots, or share this APK. For a public product, move AI requests to a secure backend that you control, add authentication, quotas, abuse prevention, and privacy controls. API usage may incur charges. Never give the assistant an API key belonging to someone else.

## Permissions

The app may request microphone and contacts access; calling and camera/flashlight actions have additional Android permission requirements. Grant only permissions you understand and need. You can deny permissions and still use supported parts of the app. Calls/messages must remain user-confirmed.

## Project structure

```text
RadhaAI/
├── .github/workflows/build.yml
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/radha/ai/
│       │   ├── Main.kt
│       │   └── Avatar.kt
│       └── res/xml/voice_interaction_service.xml
├── build.gradle.kts
├── gradle.properties
└── settings.gradle.kts
```

## Important

The source is based on the files supplied in this chat and includes project configuration around them. It has not been built or device-tested in this environment. A successful GitHub Actions build is the next verification step; if it fails, share the complete first error block from the Actions log so it can be corrected precisely.
