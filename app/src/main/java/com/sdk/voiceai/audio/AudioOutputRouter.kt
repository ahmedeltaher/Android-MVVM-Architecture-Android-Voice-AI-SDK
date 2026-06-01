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
                audioManager.stopBluetoothSco()
                Timber.d("AudioOutputRouter: route set to SPEAKER")
            }
            AudioRoute.EARPIECE -> {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false
                audioManager.stopBluetoothSco()
                Timber.d("AudioOutputRouter: route set to EARPIECE")
            }
            AudioRoute.BLUETOOTH -> {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.startBluetoothSco()
                audioManager.isBluetoothScoOn = true
                Timber.d("AudioOutputRouter: route set to BLUETOOTH")
            }
        }
    }

    fun reset() {
        audioManager.mode = AudioManager.MODE_NORMAL
        audioManager.isSpeakerphoneOn = false
        audioManager.stopBluetoothSco()
        Timber.d("AudioOutputRouter: reset to default routing")
    }
}
