# Speaker Enabler 🔊

**Speaker Enabler** is a powerful, lightweight, and modern utility application designed for Android devices to bypass a stuck headphone jack or physical earphone sensor. It lets you force all system audio (media stream and call stream profiles) directly through the built-in device loudspeaker, restoring full sound capabilities instantly.

---

## 🚀 Key Features

*   **⚡ Forced Speaker Override**: Instantly forces the system audio routing to ignore connected headphones or a stuck earphone port sensor.
*   **🔒 Active Silent Lock**: Employs an ultra-low-power, infinite silent PCM buffer stream on the active audio channel. This ensures that the Android OS never resets or reverts the communication stream to headphones after periods of inactivity.
*   **🎶 Stream Profiles**:
    *   **Media Stream (Default / Recommended)**: Perfect for YouTube, music apps, games, and standard system notifications.
    *   **Call Stream**: Forces standard communication routing. Highly robust for locked system bypass.
*   **🛡️ Background Protection Service**: Runs a battery-friendly Foreground Service with notification controls to keep the speaker routing active when you close the app or lock the screen.
*   **📡 Real-time Hardware Tracker**: Reacts instantly to physical headphone plug and unplug events using the native Android `AudioDeviceCallback` API.
*   **🩺 Built-in Audio Diagnostics**: Play a pure system-synthesized 440Hz test alert directly to verify physical routing functionality.

---

## 🛠️ Architecture & Under-the-Hood Details

### The "Stuck Headphone Jack" Challenge
When an Android device's earphone jack gets physically stuck (often due to lint, dust, water, or hardware wear), the physical switch inside the port remains closed. The OS is forced to send all audio to the headphones, silencing your built-in speakers.

### Why standard solutions fail:
Standard applications often use loops to continuously assert routing. This causes the Android OS audio pipeline driver to break down and rebuild repeatedly, resulting in an irritating "stuttering" or "choppy" sound every few seconds.

### The Speaker Enabler Solution:
1. **Intelligent Event-Driven Routing**: Instead of heavy CPU polling loops, the app registers a native hardware callback (`AudioDeviceCallback`). It only updates routing when physical hardware changes are detected.
2. **Active Silent Loop Security**: Plays an unnoticeable, zero-power silent buffer using a static `AudioTrack` configuration. This registers the application's stream as actively engaged with the OS, completely neutralizing the 3-second system timeout reset.
3. **Hardware Tone Generation**: Uses native hardware oscillators (`ToneGenerator`) to produce diagnostic sounds on the selected output stream instead of depending on brittle media files.

---

## 📱 Screenshots & Design Aesthetic

The application is built entirely using **Jetpack Compose** following **Material Design 3 (M3)** guidelines:
*   **Obsidian Dark Aesthetic**: Highly responsive, dark layout designed with eye-safe dark tones (`#0F0F13`) and glowing cyan (`#00FFCC`) status accents.
*   **Tactile Controls**: Features a glowing round toggle with continuous pulsing animations when active.
*   **Hardware Status Hub**: Displays clear connection status boxes so you can quickly see if headphones are still physically detected by the OS.

---

## 📦 GitHub Release Information

If you want to host this on GitHub, here are the recommended details for creating a repository and a release:

*   **Repository Name**: `SpeakerEnabler`
*   **Repository Description**: `An Android utility app designed to bypass a stuck headphone jack or earphone sensor and force all system audio to the built-in loudspeaker.`
*   **Commit Message**: `feat: implement event-driven audio routing with active silent lock to bypass stuck headphone jack`
*   **Release Tag**: `v1.0.0`
*   **Release Title**: `Speaker Enabler v1.0.0 Stable`
*   **Release Notes**:
    ```markdown
    ### 🚀 Speaker Enabler v1.0.0 Stable Release

    We are proud to release the first stable version of **Speaker Enabler**, the ultimate solution for stuck headphone jacks on modern Android devices.

    #### What's New in v1.0.0:
    *   **Intelligent Active Audio Lock**: Fixed the 2-3 second auto-reset issue by introducing a lightweight, zero-power, background silent stream.
    *   **Stutter-Free Playback**: Completely removed polling loops in favor of native event callbacks, resulting in fluid and continuous audio.
    *   **Dual Stream Profiles**: Support for both Call and Media stream profiles.
    *   **Foreground Service Protection**: Easily persist the loudspeaker lock even when the app is in the background or the screen is off.
    *   **Diagnostic Tools**: Synthesized test sounds using hardware `ToneGenerator` for instantaneous feedback.
    
    *Download the compiled APK below (`SpeakerEnabler_v1.0.apk`) and install it on your Android device!*
    ```

---

## 🔧 Build Requirements

*   **Language**: Kotlin 1.9+
*   **UI Framework**: Jetpack Compose
*   **Minimum SDK**: API 26 (Android 8.0)
*   **Target SDK**: API 34 (Android 14)
*   **Build Tool**: Gradle (Kotlin DSL)

---

## 🔒 Security & Privacy

This application is **100% offline** and does **not** collect, store, or transmit any user data. 
*   **No API Keys**: The codebase contains absolutely no hardcoded API keys, tracking software, ads, or third-party analytical integrations.
*   **Least-Privilege Permissions**: Uses only necessary permissions:
    *   `POST_NOTIFICATIONS` (to show the Foreground Service controls).
    *   `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (to host the silent lock service).
    *   `MODIFY_AUDIO_SETTINGS` (to routing audio outputs).
