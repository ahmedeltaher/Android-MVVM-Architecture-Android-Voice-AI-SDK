package com.sdk.voiceai.audio

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import timber.log.Timber

class HeadsetManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    if (state == 1) {
                        Timber.d("HeadsetManager: wired headset connected")
                        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    } else if (state == 0) {
                        Timber.d("HeadsetManager: wired headset disconnected")
                        audioManager.mode = AudioManager.MODE_NORMAL
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    Timber.d("HeadsetManager: Bluetooth connected")
                    @Suppress("DEPRECATION")
                    audioManager.startBluetoothSco()
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = true
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    Timber.d("HeadsetManager: Bluetooth disconnected")
                    @Suppress("DEPRECATION")
                    audioManager.stopBluetoothSco()
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = false
                }
            }
        }
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.registerReceiver(receiver, filter)
        Timber.d("HeadsetManager: registered")
    }

    fun unregister() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            Timber.w("HeadsetManager: receiver was not registered")
        }
    }
}
