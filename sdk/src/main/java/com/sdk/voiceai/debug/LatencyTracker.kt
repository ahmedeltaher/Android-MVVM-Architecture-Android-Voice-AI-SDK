package com.sdk.voiceai.debug

import android.os.SystemClock
import timber.log.Timber

class LatencyTracker {

    private val marks = mutableMapOf<String, Long>()

    fun mark(name: String) {
        marks[name] = SystemClock.elapsedRealtime()
    }

    fun measure(from: String, to: String): Long? {
        val start = marks[from] ?: return null
        val end = marks[to] ?: return null
        return end - start
    }

    fun log(label: String, from: String, to: String) {
        val ms = measure(from, to)
        if (ms != null) Timber.d("Latency [$label] $from → $to = ${ms}ms")
    }

    fun reset() {
        marks.clear()
    }
}
