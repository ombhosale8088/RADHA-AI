# Security and privacy checklist

1. Treat the API key currently entered in the app as a prototype-only secret. Do not share screenshots, source files, or APKs containing a real key.
2. Do not commit API keys, passwords, signing files, or private tokens to GitHub.
3. For any shared/public build, move model requests to a private backend. Rotate a key immediately if it has been exposed.
4. Ask for Android runtime permissions only when the user invokes the related feature, explain why they are needed, and handle denial gracefully.
5. Show a clear confirmation before calling, sending, deleting, purchasing, or changing important device settings.
6. Keep conversation history minimal, provide a clear-history action, and do not upload contacts or recordings unless the user explicitly requests the feature.
7. Use HTTPS, validate URLs and tool arguments, set request limits, and avoid logging personal content.
8. Never promise complete control over Android or third-party apps. Respect each app's own permissions and confirmation screens.
