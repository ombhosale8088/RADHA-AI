# RADHA AI development roadmap

## Phase 1 — Build and verify
- Build the debug APK with GitHub Actions or Android Studio.
- Install on a test phone.
- Verify launch, permissions, microphone recognition, Marathi/Hindi/English TTS, and error handling.
- Verify each tool separately; keep calls and message sending user-confirmed.

## Phase 2 — Safe foundations
- Replace client-side API key storage with a private backend.
- Add secure authentication, rate limits, usage limits, and server-side secrets.
- Add clear consent screens, privacy policy, data deletion, and local conversation controls.
- Add tests for tool argument validation and malformed model responses.

## Phase 3 — Assistant features
- Add reminders and calendar integration with user approval.
- Add app shortcuts and Android intents for supported actions.
- Add document and image understanding with explicit user selection.
- Add offline fallback commands for basic device functions.

## Phase 4 — Reliability
- Add structured logs that exclude secrets and personal content.
- Add crash reporting only with consent.
- Add unit/instrumentation tests and device compatibility checks.
- Add accessibility support only for legitimate, user-enabled workflows and within Android policy.

## Phase 5 — Public release readiness
- Backend security review and privacy/legal review.
- Play policy compliance and permission minimization.
- Signed release build, secure update process, and support channel.
- Independent security testing before public launch.
