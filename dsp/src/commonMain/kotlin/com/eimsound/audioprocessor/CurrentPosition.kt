package com.eimsound.audioprocessor

/**
 * @see com.eimsound.daw.impl.CurrentPositionImpl
 * @see com.eimsound.audioprocessor.RenderPosition
 */
interface CurrentPosition {
    var bpm: Double
    val timeInSamples: Long
    val timeInSeconds: Double
    val ppq: Int
    val timeInPPQ: Int
    val ppqPosition: Double
    var isPlaying: Boolean
    var isLooping: Boolean
    var isProjectLooping: Boolean
    var isRecording: Boolean
    val isRealtime: Boolean
    val bufferSize: Int
    val sampleRate: Int
    var timeSigNumerator: Int
    var timeSigDenominator: Int
    var projectRange: IntRange
    var loopingRange: IntRange
    val ppqCountOfBlock: Int

    fun update(timeInSamples: Long)
    fun setPPQPosition(ppqPosition: Double)
    fun setCurrentTime(timeInPPQ: Int)
    fun setSampleRateAndBufferSize(sampleRate: Int, bufferSize: Int)
}

fun CurrentPosition.convertPPQToSamples(ppq: Int) = (ppq.toDouble() / this.ppq / bpm * 60.0 * sampleRate).toLong()
fun CurrentPosition.convertSamplesToPPQ(samples: Long) = (samples.toDouble() / sampleRate * bpm / 60.0 * this.ppq).toInt()
fun CurrentPosition.convertPPQToSeconds(ppq: Float) = ppq.toDouble() / this.ppq / bpm * 60.0
fun CurrentPosition.convertSecondsToPPQ(seconds: Float) = seconds * bpm * this.ppq / 60.0
val CurrentPosition.projectDisplayPPQ get() = projectRange.last.coerceAtLeast(timeSigDenominator * ppq * 96) +
        timeSigDenominator * ppq * 32
val CurrentPosition.oneBarPPQ get() = timeSigDenominator * ppq
