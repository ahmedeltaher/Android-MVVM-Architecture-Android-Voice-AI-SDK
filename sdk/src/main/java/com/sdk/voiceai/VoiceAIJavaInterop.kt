package com.sdk.voiceai

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

// Java-friendly extension functions for VoiceAISession
// These wrap suspend functions as CompletableFuture for Java callers

fun VoiceAISession.startAsync(): CompletableFuture<Unit> =
    CoroutineScope(Dispatchers.Main).future { this@startAsync.start() }

fun VoiceAISession.stopAsync(): CompletableFuture<Unit> =
    CoroutineScope(Dispatchers.Main).future { this@stopAsync.stop() }

// Java-friendly listener interface
fun interface VoiceAIEventListener {
    fun onEvent(event: com.sdk.voiceai.model.VoiceAIEvent)
}

fun VoiceAISession.addEventListenerJava(
    scope: CoroutineScope,
    listener: VoiceAIEventListener,
) { scope.launch { events.collect { listener.onEvent(it) } } }
