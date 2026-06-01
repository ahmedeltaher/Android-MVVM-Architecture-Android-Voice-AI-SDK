package com.sdk.voiceai.debug

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SdkLogEvent(val timestampMs: Long, val tag: String, val message: String)

class SdkEventLog(private val capacity: Int = 200) {

    private val _events = MutableStateFlow<List<SdkLogEvent>>(emptyList())
    val events: StateFlow<List<SdkLogEvent>> = _events.asStateFlow()

    fun log(tag: String, message: String) {
        val event = SdkLogEvent(System.currentTimeMillis(), tag, message)
        val current = _events.value
        _events.value = if (current.size >= capacity) {
            current.drop(1) + event
        } else {
            current + event
        }
    }

    fun clear() {
        _events.value = emptyList()
    }
}
