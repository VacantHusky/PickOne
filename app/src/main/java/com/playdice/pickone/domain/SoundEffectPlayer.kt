package com.playdice.pickone.domain

import android.media.AudioManager
import android.media.ToneGenerator
import com.playdice.pickone.model.SoundChoice
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundEffectPlayer @Inject constructor() {
    fun play(choice: SoundChoice) {
        if (choice == SoundChoice.NONE) return
        val tone = when (choice) {
            SoundChoice.NONE -> return
            SoundChoice.CRISP -> ToneGenerator.TONE_PROP_BEEP
            SoundChoice.SOFT -> ToneGenerator.TONE_PROP_ACK
            SoundChoice.MECHANICAL -> ToneGenerator.TONE_DTMF_A
        }
        val duration = when (choice) {
            SoundChoice.SOFT -> 180
            SoundChoice.MECHANICAL -> 120
            else -> 100
        }
        ToneGenerator(AudioManager.STREAM_MUSIC, 55).apply {
            startTone(tone, duration)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ release() }, duration + 80L)
        }
    }
}
