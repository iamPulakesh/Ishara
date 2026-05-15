# Ishara
**An Offline-first, Multimodal AI Indian Sign Language (ISL) Assistant for the Hearing-Impaired Community**

> **Ishara** is a Bengali word meaning *sign* or *gesture*.

---

## Key Features
- **Offline & Private:** The entire application runs locally on the device. No internet connection is required (only one time needed to download the Gemma 4 model), ensuring complete user privacy as voice, camera data, and chat history never leave the user's phone.
- **In-Chat Video Playback:** Automatically parses the AI's response and seamlessly plays localized, animated ISL video snippets directly within the chat interface.
- **Camera-Based Real-Time Learning:** Users can point their smartphone camera at real-world objects. The app analyzes the image and instantly replies with the corresponding ISL sign. Check the built-in signs [here](SIGN_LIBRARY.md).
- **Real-Time Speech-to-Text:** Converts spoken words into text instantly using the offline Sherpa-ONNX engine, allowing family members to speak naturally to the app.
- **Bilingual Support:** Natively supports both English and Bengali (বাংলা) for voice input, text input, and AI processing.

---

## Tech Stack
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose (Material 3)
* **Dependency Injection:** Dagger Hilt
* **Inference Model:** Gemma-4-E2B-it-litert-lm
* **Local Database:** Room Database (Session/Chat History Tracking)
* **Camera / Vision:** CameraX API
* **Edge AI & Models:**
  * **Google LiteRT (TensorFlow Lite RunTime):** On-device Large Language/Vision Model execution.
  * **Sherpa-ONNX:** Real-time, offline Voice Activity Detection & Speech-to-Text inference.
---

---

## Development

### Prerequisites
- **Android Studio / SDK Tools:** Ladybug (or Koala) / Android Command Line Tools
- **Android SDK:** Target API 35, Minimum API 26 (Android 8.0)
- **Java (JDK):** Version 21
- **Gradle:** 8.12
- **RAM:** Minimum 8GB

### Setup

**Bash:**
```bash
git clone https://github.com/iamPulakesh/Ishara.git
cd Ishara
./gradlew installDebug
```

**PowerShell:**
```powershell
.\gradlew installDebug
```
---

## Acknowledgements

- **ISL Sign Videos** — The Indian Sign Language video resources used in this application are sourced from the [**Indian Sign Language Research and Training Centre (ISLRTC)**](https://islrtc.nic.in), an autonomous body under the Department of Empowerment of Persons with Disabilities (DEPwD), Ministry of Social Justice & Empowerment, Government of India.
- **ISL Animated Videos Dataset** — Additional sign language videos sourced from the [**Indian Sign Language Animated Videos**](https://www.kaggle.com/datasets/koushikchouhan/indian-sign-language-animated-videos) dataset on Kaggle by Koushik Chouhan.