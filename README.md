# Ishara
**An Offline-first, Multimodal AI Indian Sign Language (ISL) Assistant for the Hearing-Impaired Community**

> **Ishara (ইশারা)** is a Bengali word meaning *sign* or *gesture*.

---

## Features
- Transcribe your voice to find ISL translations in real-time.
- Analyze real-world objects using your camera and teach you their sign.
- Translate complex conversational phrases into ISL's unique SOV (Subject-Object-Verb) grammar.
- Play instant, localized sign-language video snippets right inside the chat.
- On-Device LLM — Built with Google's Gemma 4 model, parsing complex conversational context completely offline.
- Complete Privacy — 100% offline, your voice, images and chat history never leave your device.
- Dynamic Language Support — Native bilingual support for both English and Bengali (বাংলা).

---

## Tech Stack
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Dependency Injection:** Dagger Hilt
* **Inference Model:** Google Gemma 4 E2B-IT
* **Local Database:** Room Database (Session/Chat History Tracking)
* **Camera / Vision:** CameraX API
* **Edge AI & Models:**
  * **Google LiteRT (TensorFlow Lite RunTime):** On-device Large Language/Vision Model execution.
  * **Sherpa-ONNX:** Real-time, offline Voice Activity Detection & Speech-to-Text inference.
---

## Architecture Diagram

```
        User Input (Text / Image)
                   │
                   ▼
            ChatViewModel
                   │
                   ▼
      ISL System Prompt Injected
        (SOV grammar rules)
                   │
                   ▼
      LiteRT Engine — Gemma 4 E2B-IT
        on-device via JNI mmap
                   │
                   ▼
          Raw Model Response
                   │
                   ▼
           ISL Tag Parser
          ┌────────┴────────┐
          │                 │
     Plain Text       [[ISL: WORD]]
     Response              Tags
          │                 │
          ▼                 ▼
     Chat Bubble       Video Player
                    (sign_*.mp4 playback)
```

---

## Development

### Prerequisites
- **Android Studio / SDK Tools:** Ladybug (or Koala) / Android Command Line Tools
- **Android SDK:** Target API 35, Minimum API 26 (Android 8.0)
- **Java (JDK):** Version 21
- **Gradle:** 8.12
- **RAM:** Minimum 8GB

### Setup

```bash
git clone https://github.com/iamPulakesh/Ishara.git
cd Ishara
./gradlew installDebug
```
