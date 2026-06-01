# Migration Guide — Android Voice AI SDK

## v0.x → v1.0.0

This document lists every breaking API change introduced in v1.0.0 with before/after code snippets.

---

### 1. `VoiceAISession.start()` is no longer a suspend function

**Before (v0.x)**
```kotlin
// Called from a coroutine
lifecycleScope.launch {
    session.start()
}
```

**After (v1.0.0)**
```kotlin
// Called directly — no coroutine context required
session.start()
// Use session.events to observe the resulting state transitions
```

---

### 2. `VoiceAIConfig.sttEngine` changed from `SpeechToTextEngine?` to `Any?`

The field type was widened to `Any?` to break a circular dependency between the `model` and `stt` packages. The cast is performed inside `VoiceAISDK.Builder`.

**Before (v0.x)**
```kotlin
val config = VoiceAIConfig(
    anthropicApiKey = key,
    sttEngine = AndroidSttEngine(context),  // typed as SpeechToTextEngine?
)
```

**After (v1.0.0)**
```kotlin
val config = VoiceAIConfig(
    anthropicApiKey = key,
    sttEngine = AndroidSttEngine(context),  // Any? — same call site, no change needed
)
// Or via the DSL:
val sdk = voiceAISDK(context) {
    anthropicApiKey = key
    config { copy(sttEngine = AndroidSttEngine(context)) }
}
```

---

### 3. `VoiceAIEvent.ErrorOccurred` replaces the old `onError` callback interface

**Before (v0.x)**
```kotlin
session.setOnErrorListener { error ->
    showError(error.message)
}
```

**After (v1.0.0)**
```kotlin
lifecycleScope.launch {
    session.events.collect { event ->
        if (event is VoiceAIEvent.ErrorOccurred) {
            showError(event.error.message)
        }
    }
}
```

---

### 4. `TtsConfig.volume` parameter added

`TtsConfig` gains a `volume: Float` field (default `1.0f`). If you construct `TtsConfig` positionally this is a source-level break.

**Before (v0.x)**
```kotlin
val cfg = TtsConfig(Locale.US, 1.0f, 1.0f)  // locale, rate, pitch
```

**After (v1.0.0)**
```kotlin
val cfg = TtsConfig(
    locale = Locale.US,
    speechRate = 1.0f,
    pitch = 1.0f,
    volume = 1.0f,  // new field
)
```

---

### 5. `ElevenLabsTtsEngine` constructor now requires `httpClient` parameter

To share connection pools across engines the `OkHttpClient` is now caller-supplied.

**Before (v0.x)**
```kotlin
val engine = ElevenLabsTtsEngine(apiKey = key, voiceId = "abc123")
```

**After (v1.0.0)**
```kotlin
val httpClient = OkHttpClient.Builder().build()
val engine = ElevenLabsTtsEngine(
    apiKey = key,
    voiceId = "abc123",
    httpClient = httpClient,
)
```

---

### 6. `SessionState.Error(error)` replaces old `SessionState.Failed` enum value

**Before (v0.x)**
```kotlin
when (state) {
    SessionState.Failed -> showError()
    else -> { }
}
```

**After (v1.0.0)**
```kotlin
when (state) {
    is SessionState.Error -> showError(state.error.message)
    else -> { }
}
```

---

## Deprecation Notices

### `VoiceAISession.setOnErrorListener` — removed in v1.0.0

```kotlin
@Deprecated(
    message = "Use session.events.collect { if (it is VoiceAIEvent.ErrorOccurred) … } instead.",
    replaceWith = ReplaceWith(
        "lifecycleScope.launch { session.events.collect { if (it is VoiceAIEvent.ErrorOccurred) handler(it.error) } }",
    ),
    level = DeprecationLevel.ERROR,
)
fun setOnErrorListener(handler: (VoiceAIError) -> Unit): Unit = error("Removed in v1.0.0")
```

### `VoiceAIConfig.legacyMode` — removed in v1.0.0

The `legacyMode` flag that disabled SSE streaming in v0.x has been removed. Streaming is always enabled.

```kotlin
@Deprecated(
    message = "legacyMode is removed. Streaming is always enabled in v1.0.0.",
    replaceWith = ReplaceWith(""),
    level = DeprecationLevel.ERROR,
)
val legacyMode: Boolean = false
```
