package com.example.engine

import android.content.Context
import android.media.MediaActionSound
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticSoundManager(private val context: Context) {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val mediaActionSound = MediaActionSound().apply {
        load(MediaActionSound.SHUTTER_CLICK)
        load(MediaActionSound.FOCUS_COMPLETE)
    }

    var isSilentSpyMode: Boolean = false

    fun onZoomStepChanged() {
        // Light haptic tick
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                it.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(10)
            }
        }
    }

    fun onZoomKeyMilestoneReached() {
        // Stronger click on key zoom milestone like 10X, 30X
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                it.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(35)
            }
        }
    }

    fun onSniperLocked() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 20, 40, 20), -1))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(30)
            }
        }
        if (!isSilentSpyMode) {
            mediaActionSound.play(MediaActionSound.FOCUS_COMPLETE)
        }
    }

    fun playShutterEffect() {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                it.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(45)
            }
        }
        if (!isSilentSpyMode) {
            mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
        }
    }

    fun release() {
        mediaActionSound.release()
    }
}
