package com.factory.solacecalmsolitaire.util

import android.media.AudioManager
import android.media.ToneGenerator

/** Lightweight, asset-free feedback tones (avoids bundling audio files for simple cues). */
object SoundPlayer {
    private var toneGenerator: ToneGenerator? = null

    private fun generator(): ToneGenerator =
        toneGenerator ?: ToneGenerator(AudioManager.STREAM_MUSIC, 70).also { toneGenerator = it }

    fun playMove() {
        runCatching { generator().startTone(ToneGenerator.TONE_PROP_BEEP2, 35) }
    }

    fun playInvalid() {
        runCatching { generator().startTone(ToneGenerator.TONE_PROP_NACK, 60) }
    }

    fun playWin() {
        runCatching { generator().startTone(ToneGenerator.TONE_PROP_ACK, 400) }
    }

    fun release() {
        runCatching { toneGenerator?.release() }
        toneGenerator = null
    }
}
