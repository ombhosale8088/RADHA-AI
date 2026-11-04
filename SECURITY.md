# Security notes

1. Never commit API keys, passwords, signing keys, or private tokens to GitHub.
2. The current prototype stores its AI key locally in app-private preferences. This is not secure enough for a public app. Use a backend before distributing to other people.
3. Ask for permissions only when a feature is used. Explain why each permission is needed.
4. Calls and messaging must require an explicit user action. The app currently opens confirmation/draft UI; it must not silently call or send.
5. Do not collect contacts, audio, location, or conversation content unless needed and clearly disclosed.
6. Provide a way to clear local conversation data and revoke permissions.
7. Treat model-generated tool arguments as untrusted input. Validate names, URLs, numbers, time values, and all arguments before acting.
8. Do not claim that an action succeeded unless Android confirms it.
