# Nova for Chromebook — source starter v0.1

This Android Studio project is a first local-first prototype.

## Included
- Compose chat interface
- Local Room database for chat history and memory notes
- Model-file import into app-private storage
- On-device inference integration using Google LiteRT-LM
- A second model pass that proposes durable memories after each exchange
- Memory notes are added to later prompts

## Critical compatibility notes
1. LiteRT-LM expects a compatible `.litertlm` model file. It cannot directly load PocketPal's `.gguf` model. Your existing Qwen GGUF cannot be selected as-is; it would need a supported conversion or a compatible LiteRT-LM model.
2. This is source code, not a compiled APK. It has not been built or tested on your Chromebook.
3. Models may be several GB and need sufficient storage and RAM. This app starts with CPU inference for broad compatibility.
4. Memory extraction is model-generated and may make mistakes. Treat notes as suggestions and review them. This first prototype does not yet have a memory edit/delete interface or robust duplicate merging.
5. No Android network permission is declared. Model inference and memory storage are local. Gradle needs internet to download development dependencies.

## Build
1. Open this folder in Android Studio.
2. Allow Gradle sync and install any requested Android SDK components.
3. Choose Build > Build Bundle(s) / APK(s) > Build APK(s).
4. Install the debug APK on a Chromebook that allows Android app installation.

## Main source
- `MainActivity.kt`: chat UI and model import
- `LocalBrain.kt`: local inference and memory curator
- `MemoryDb.kt`: local Room database

## Next steps
Add memory review/edit/delete, export/import, robust JSON parsing, model metadata checks, and testing against the target Chromebook's CPU architecture and RAM.
