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

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_HEADSET_PLUG -> {
                    val state = intent.getIntExtra("state", -1)
                    if (state == 1) {
                        Timber.d("HeadsetManager: Wired headset connected")
                        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    } else if (state == 0) {
                        Timber.d("HeadsetManager: Wired headset disconnected")
                        audioManager.mode = AudioManager.MODE_NORMAL
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    Timber.d("HeadsetManager: Bluetooth device connected")
                    audioManager.startBluetoothSco()
                    audioManager.isBluetoothScoOn = true
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    Timber.d("HeadsetManager: Bluetooth device disconnected")
                    audioManager.stopBluetoothSco()
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
    }

    fun unregister() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: IllegalArgumentException) {
            Timber.d("HeadsetManager: Receiver was not registered")
        }
    }
}
