package com.sdk.voiceai.fakes

import com.sdk.voiceai.stt.SpeechToTextEngine
import com.sdk.voiceai.stt.SttConfig
import com.sdk.voiceai.stt.TranscriptResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeSttEngine(
    var nextResult: TranscriptResult = TranscriptResult("Hello world", 0.9f, true),
) : SpeechToTextEngine {

    var callCount: Int = 0

    override suspend fun transcribe(audioBytes: ByteArray, config: SttConfig): TranscriptResult {
        callCount++
        return nextResult
    }

    override fun transcribeStream(config: SttConfig): Flow<TranscriptResult> = flowOf(nextResult)

    override fun cancel() {}
}
