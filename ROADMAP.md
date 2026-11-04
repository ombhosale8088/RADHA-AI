# RADHA AI development roadmap

The goal is to grow RADHA AI in safe, testable stages. “World's biggest assistant” is an ambition, not a feature that can be completed in one code drop. Each stage needs implementation, device testing, privacy review, and reliable error handling.

## Stage 0 — Make the current app build
- Build the existing Kotlin project with JDK 17 and Android SDK 34.
- Install on the target Android phone.
- Test microphone permission, Marathi/English/Hindi speech recognition, TTS, and network errors.
- Test every action individually; verify the confirmation shown to the user.

## Stage 1 — Stable assistant core
- Replace in-memory chat history with bounded, user-clearable local conversation history.
- Add request timeout, cancellation, retry/backoff, structured error messages, and offline state.
- Add settings for language, speech speed, wake/launch behavior, and privacy controls.
- Add unit tests for tool argument validation and action routing.

## Stage 2 — Secure AI backend
- Create a private HTTPS backend (for example, a small Kotlin/Node/Python service).
- Keep provider API keys only in server-side environment secrets; never commit keys or bundle them in the APK.
- Authenticate each app installation, rate-limit requests, cap usage, validate tool calls, and redact logs.
- Add provider abstraction so the model can be changed without rewriting Android UI.

## Stage 3 — Phone integrations (permission by permission)
- Calendar and reminders through supported Android intents/APIs.
- Contacts lookup only after runtime permission and clear explanation.
- Calls through the dialer/confirmation screen.
- Messaging through share/deep-link flows; never silently send messages.
- App launching using Android package visibility rules; avoid broad package access unless justified.
- Accessibility automation only for a clearly disclosed, user-enabled accessibility purpose; do not use it to bypass app security or user confirmation.

## Stage 4 — Knowledge and productivity
- Web search with source links and freshness indicators.
- User-selected documents and notes with explicit import/delete controls.
- Tasks, reminders, study helper, translation, summarization, and voice dictation.
- Optional connectors (calendar, email, storage) with separate consent and revocation.

## Stage 5 — Quality and release
- Automated tests, crash reporting with privacy controls, accessibility testing, battery/network testing.
- Data deletion/export, privacy policy, terms, support contact, and secure release signing.
- Closed testing before any public release.

## Not promised by this roadmap
No Android app can legitimately promise unrestricted access to every app, automatic installation of any app, bypassing lock screens, hidden monitoring, or silent sending/calling. Android and third-party apps enforce these boundaries.
