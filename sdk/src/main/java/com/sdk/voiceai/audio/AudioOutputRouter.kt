package com.sdk.voiceai.audio

import android.content.Context
import android.media.AudioManager
import timber.log.Timber

enum class AudioRoute { SPEAKER, EARPIECE, BLUETOOTH }

class AudioOutputRouter(context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun setRoute(route: AudioRoute) {
        when (route) {
            AudioRoute.SPEAKER -> {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = true
                @Suppress("DEPRECATION") audioManager.stopBluetoothSco()
                Timber.d("AudioOutputRouter: route=SPEAKER")
            }
            AudioRoute.EARPIECE -> {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false
                @Suppress("DEPRECATION") audioManager.stopBluetoothSco()
                Timber.d("AudioOutputRouter: route=EARPIECE")
            }
            AudioRoute.BLUETOOTH -> {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                @Suppress("DEPRECATION") audioManager.startBluetoothSco()
                @Suppress("DEPRECATION") audioManager.isBluetoothScoOn = true
                Timber.d("AudioOutputRouter: route=BLUETOOTH")
            }
        }
    }

    fun reset() {
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
        @Suppress("DEPRECATION") audioManager.stopBluetoothSco()
        Timber.d("AudioOutputRouter: reset")
    }
}
