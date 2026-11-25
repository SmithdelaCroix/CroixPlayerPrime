/*
 * Croix Player Prime (2026) | GPL-3.0 License
 */

package com.croix.prime.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AudioOutputRoute(val labelEs: String, val labelEn: String) {
    SPEAKER("Altavoz", "Phone Speaker"),
    BLUETOOTH("Bluetooth", "Bluetooth Audio"),
    WIRED_HEADSET("Audífonos Cable / USB", "Wired / USB Headset"),
}

data class ActiveAudioOutput(
    val route: AudioOutputRoute = AudioOutputRoute.SPEAKER,
    val deviceName: String = "Altavoz del dispositivo",
)

/**
 * Monitors Android 12+ audio output routes (Bluetooth, Wired/USB Headset, Built-in Speaker)
 * so Croix Player Prime can auto-switch AutoEq/EQ profiles per connected device.
 */
class AudioOutputMonitor(context: Context) {
    private val audioManager = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val _activeOutput = MutableStateFlow(detectCurrentOutput())
    val activeOutput: StateFlow<ActiveAudioOutput> = _activeOutput.asStateFlow()

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            _activeOutput.value = detectCurrentOutput()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            _activeOutput.value = detectCurrentOutput()
        }
    }

    init {
        runCatching {
            audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
        }
    }

    fun refresh(): ActiveAudioOutput {
        val current = detectCurrentOutput()
        _activeOutput.value = current
        return current
    }

    private fun detectCurrentOutput(): ActiveAudioOutput {
        val outputs = runCatching {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        }.getOrNull().orEmpty()

        val bt = outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                it.type == AudioDeviceInfo.TYPE_HEARING_AID
        }
        if (bt != null) {
            return ActiveAudioOutput(
                route = AudioOutputRoute.BLUETOOTH,
                deviceName = bt.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Audífonos Bluetooth",
            )
        }

        val wired = outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_USB_DEVICE
        }
        if (wired != null) {
            return ActiveAudioOutput(
                route = AudioOutputRoute.WIRED_HEADSET,
                deviceName = wired.productName?.toString()?.takeIf { it.isNotBlank() } ?: "Audífonos Cable / USB",
            )
        }

        return ActiveAudioOutput(
            route = AudioOutputRoute.SPEAKER,
            deviceName = "Altavoz del teléfono",
        )
    }

    fun release() {
        runCatching { audioManager.unregisterAudioDeviceCallback(callback) }
    }
}
