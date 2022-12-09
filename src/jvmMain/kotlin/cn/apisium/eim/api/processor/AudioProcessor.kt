package cn.apisium.eim.api.processor

import cn.apisium.eim.api.CurrentPosition

interface AudioProcessor: AutoCloseable {
    val inputChannelsCount: Int
    val outputChannelsCount: Int
    var name: String
    val uuid: Long
    suspend fun processBlock(buffers: Array<FloatArray>, position: CurrentPosition, midiBuffer: ArrayList<Byte>) { }
    fun prepareToPlay() { }
    override fun close() { }
}
