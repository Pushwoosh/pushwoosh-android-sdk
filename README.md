Pushwoosh Android SDK
=====================
[![GitHub release](https://img.shields.io/github/release/Pushwoosh/pushwoosh-android-sdk.svg?style=flat-square)](https://github.com/Pushwoosh/pushwoosh-android-sdk/releases) [![Maven Central](https://img.shields.io/maven-central/v/com.pushwoosh/pushwoosh?style=flat-square)](https://central.sonatype.com/artifact/com.pushwoosh/pushwoosh)

## 📚 Documentation

- [Integration Guide](https://docs.pushwoosh.com/platform-docs/pushwoosh-sdk/android-push-notifications) - step-by-step setup for FCM, HMS, ADM
- [API Reference](https://pushwoosh.github.io/pushwoosh-android-sdk) - classes, methods, all SDK modules
- [Sample Projects](https://github.com/Pushwoosh/pushwoosh-android-sample) - ready-to-run demo apps

## 📦 Gradle Integration

```groovy
dependencies {
    implementation 'com.pushwoosh:pushwoosh:6.7.52'
    implementation 'com.pushwoosh:pushwoosh-firebase:6.7.52'
}
```

## 🤖 AI-Assisted Integration

Integrate Pushwoosh Android SDK using AI coding assistants (Claude Code, Cursor, GitHub Copilot, etc.).

> **Requirement:** Your AI assistant must have access to [Context7](https://context7.com/) MCP server or web search capabilities.

### Quick Start Prompts

Choose the prompt that matches your task:

---

#### 1. Basic SDK Integration

```
Integrate Pushwoosh Android SDK into my Android project with Firebase Cloud Messaging (FCM).

Requirements:
- Add gradle dependencies (pushwoosh, pushwoosh-firebase)
- Configure AndroidManifest.xml with Pushwoosh App ID: YOUR_APP_ID
- Register for push notifications in MainActivity

Use Context7 MCP to fetch Pushwoosh Android SDK documentation.
```

---

#### 2. Custom Push Notification Logic

```
Show me how to handle push notification callbacks (receive, open) with Pushwoosh SDK in Android. I want to add analytics tracking for these events.

Use Context7 MCP to fetch Pushwoosh Android SDK documentation for NotificationServiceExtension.
```

---

#### 3. User Segmentation with Tags

```
Show me how to use Pushwoosh tags for user segmentation in Android. Create example helper class with methods for setting and getting tags.

Use Context7 MCP to fetch Pushwoosh Android SDK documentation for setTags and getTags.
```

---

Pushwoosh team
http://www.pushwoosh.com
