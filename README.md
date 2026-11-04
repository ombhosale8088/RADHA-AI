# RADHA AI — Android Assistant Starter

RADHA AI is the name selected for this Android voice-assistant project. This repository is a **starter foundation**, not a finished all-powerful assistant. It currently contains a Kotlin Android app with a voice orb, speech-to-text, text-to-speech, a chat-completions client, and a small set of user-confirmed phone actions.

## Current features in this source

- English (India), Marathi, and Hindi language selection.
- Speech recognition and spoken responses (requires Android speech services).
- AI chat using an OpenAI-compatible Chat Completions API.
- Open an installed app, open a website/search, prepare a WhatsApp message, prepare a call, set an alarm, flashlight control, and battery status.
- Android voice-assistant service declarations. Device support and default-assistant selection vary by Android version/manufacturer.

## Important limitations

- This does **not** control every part of a phone. Android deliberately restricts background actions, messages, calls, app installation, screen access, and sensitive settings.
- WhatsApp messages are only drafted; the user must press Send. Calls are presented for confirmation.
- The current prototype stores an API key in app preferences. That is suitable only for a private prototype. **Do not publish or distribute this build with a personal API key.** A production release should call a private backend that keeps the provider key on the server, applies authentication, rate limits, and usage limits.
- The declared `RadhaRecognitionService` is currently an empty placeholder; Android's full system voice-recognition flow is not implemented. The app's in-app speech recognition is separate.
- No APK is included. Build it using the instructions below.

## Build APK with GitHub Actions

1. Create a GitHub repository and upload the contents of this folder (so `settings.gradle.kts` and `app/` are at repository root).
2. Open **Actions** and enable workflows if GitHub asks.
3. Run **Build APK** using `workflow_dispatch`, or push a commit.
4. Open the completed workflow run and download the `RADHA-AI-APK` artifact.
5. Extract the artifact and install `app-debug.apk` on your Android phone. Android may ask you to allow installation from that source.

## Build locally

Open this folder in Android Studio (JDK 17, Android SDK 34 installed), wait for Gradle sync, then choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

## Project layout

- `app/src/main/java/com/radha/ai/Main.kt` — app UI, speech, AI request loop, phone-action handlers, voice service classes.
- `app/src/main/java/com/radha/ai/Avatar.kt` — embedded avatar asset.
- `app/src/main/AndroidManifest.xml` — app components and permissions.
- `app/src/main/res/xml/voice_interaction_service.xml` — voice-interaction service metadata.
- `docs/ROADMAP.md` — staged feature plan and architecture.
- `docs/SECURITY.md` — API-key and permission guidance.

## Next development milestone

Before adding more actions, first make a reproducible debug build, test speech on the target phone, move AI requests behind a private backend, and add explicit permission/confirmation UX. See `docs/ROADMAP.md`.
