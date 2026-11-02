# SANAM Job Assistant v2.0
> **"Boli se Resume, Voice se Naukri"**  
> *Designed & Developed by Dayaram Chouhan ❤️*

Package Name: `com.dayaramchouhan.sanam`  
Version: `2.0.0` (versionCode 2)

---

## 🌟 Key Features (SANAM v2.0 Engine)

1. **Full-Screen Holographic Orb (`SanamHolographicOrb`)**
   - Pure black theme with animated particle stars.
   - 70% screen height center Orb with state-based audio visualizer rings (`Idle`, `Listening`, `Speaking`, `Thinking`, `Muted`).
   - Voice welcome: *"Hi Boss! Mai SANAM! Aapki Job Apply Assistant! Bolo kaunsi job apply karwani hai!"*

2. **5 Multi-Agent Team Architecture (`domain/agents/`)**
   - **JobSearchAgent**: Searches Canada Job Bank, Indeed, Seek Australia, and NZ Accredited Jobs.
   - **DescriptionReaderAgent**: Reads full job description and extracts required skills, experience, and keywords.
   - **ATSScorerAgent**: Detailed skill comparison, current score vs required score (92+).
   - **ResumeBuilderAgent**: Generates ATS 92+ single-column Canada format resume + cover letter PDF documents.
   - **ApplyAgent**: Prepares email application intent with attached PDFs, logs application in Room DB, and sets 4-week follow-up reminder.

3. **FreeLLM Brain (`FreeLLMProvider`)**
   - Auto-switches to FreeLLM API fallback (`https://api.freellmapi.com/v1`) if Gemini quota ends.

4. **Qwen-Layered Document Eyes (`NeuralLensScanner`)**
   - Splits uploaded resumes into 3 distinct neural layers: Personal Info Layer, Experience Layer, Skills & Education Layer.

5. **Phone & PC Remote Controller (`PhonePcController`)**
   - Launch apps (WhatsApp, Chrome, YouTube, Camera, Settings), control brightness/volume, and send remote PC socket commands.

---

## 📱 How to Install APK on Android Device

1. Download `app-debug.apk` or `app-release.apk` from the build outputs (`app/build/outputs/apk/`).
2. Open **Settings > Security** on your phone and enable **"Install Unknown Apps"**.
3. Tap the downloaded APK file and click **Install**.
4. Launch **SANAM Job Assistant**.

---

## 🔑 How to Add Gemini API Key or Groq Key

1. Open **SANAM Job Assistant**.
2. Tap the **Settings** icon on the bottom navigation bar.
3. Under **SANAM AI Connectors**:
   - Paste your **Gemini Live API Key** or **Groq API Key**.
4. Your API key will be saved securely using `EncryptedSharedPreferences`.
5. Keep **Free Mode ON** for 100% free operation using device-local fallback models!

---

*Designed & Developed by Dayaram Chouhan ❤️ — ATS & Voice System for Overseas Jobs.*
