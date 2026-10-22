# ChatBotPro

ChatBotPro is an Android app that runs AI language models locally on your phone.
It is a rebranded and adapted build of [Pocket LLM](https://github.com/dineshsoudagar/local-llms-on-android)
by Dinesh Soudagar (MIT licensed — see `NOTICE.md`).

- Application ID / namespace: `org.chaitu.chatbotpro`
- Developer: Chaitanya
- Contact: email@chaitu.net
- Effective date for the bundled legal documents: 22 October 2026

## Features

- On-device text generation with LiteRT-LM and ONNX Runtime
- Local chat history, attachments, OCR, camera input, and speech input
- Optional LAN web interface served from the phone
- Bundled Terms of Use and Privacy Policy

## Build

The app has no product flavors: it uses the standard debug and release build
types. It uses Google ML Kit for on-device text recognition.

Build the release APK:

```sh
./gradlew :app:assembleRelease
```

For local testing, a debug build installs alongside the release under the
application ID `org.chaitu.chatbotpro.debug`:

```sh
./gradlew :app:assembleDebug
```

## Requirements

- JDK 17
- Android SDK Platform 36
- Android build-tools 36.0.0

Unit tests:

```sh
./gradlew :app:testDebugUnitTest
```

Build outputs are written to `app/build/outputs/apk/release/`.

## Toolchain

This project builds with Gradle 8.10.2, Android Gradle Plugin 8.7.2, and
Kotlin 2.4.0. Kotlin 2.4 is required because the LiteRT-LM dependency ships
Kotlin 2.4 metadata.

The Sherpa ONNX Android AAR is included at
`app/libs/sherpa-onnx-static-link-onnxruntime-1.13.4.aar` so builds do not
depend on GitHub Releases availability. The upstream Ivy repository remains
configured in `settings.gradle.kts` as a fallback.

## Branding and attribution

Launcher icons, the toolbar mark, and the LAN web UI logo all use `logo.jpeg`.
The upstream MIT notice is retained in `NOTICE.md` and in the in-app Terms of
Use.

## License

The upstream Pocket LLM project is MIT licensed. See `NOTICE.md` for the full
upstream notice. Third-party libraries, model files, and model runtimes keep
their own licenses.
