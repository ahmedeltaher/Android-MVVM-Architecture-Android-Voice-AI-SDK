package com.sdk.voiceai.telemetry

import timber.log.Timber

data class TelemetryEvent(
    val type: String,
    val properties: Map<String, String> = emptyMap(),
)

class SdkTelemetry(private val enabled: Boolean = false) {
    private var sessionCount = 0
    private val errorCounts = mutableMapOf<String, Int>()
    private val latencySamples = mutableListOf<Long>()

    fun recordSessionStart() { if (!enabled) return; sessionCount++ }
    fun recordError(errorType: String) { if (!enabled) return; errorCounts[errorType] = (errorCounts[errorType] ?: 0) + 1 }
    fun recordLatency(ms: Long) { if (!enabled) return; latencySamples.add(ms) }
    fun flush() {
        if (!enabled) return
        val p50 = if (latencySamples.isEmpty()) 0L else latencySamples.sorted()[latencySamples.size / 2]
        Timber.d("Telemetry — sessions=$sessionCount errors=$errorCounts p50Latency=${p50}ms")
        // Real implementation would POST anonymised data to a backend.
        // Never sends audio, transcripts, or PII.
    }
}
