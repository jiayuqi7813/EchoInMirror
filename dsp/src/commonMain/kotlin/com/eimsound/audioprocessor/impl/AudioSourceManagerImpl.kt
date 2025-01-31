package com.eimsound.audioprocessor.impl

import androidx.compose.runtime.mutableStateMapOf
import com.eimsound.audioprocessor.*
import com.eimsound.daw.utils.NoSuchFactoryException
import com.eimsound.daw.utils.asString
import kotlinx.serialization.json.JsonObject
import java.nio.file.Path
import java.util.*

const val IN_MEMORY_FILE_SIZE = 64 * 1024 * 1024 // 64 MB

class AudioSourceManagerImpl : AudioSourceManager {
    override val factories = mutableStateMapOf<String, AudioSourceFactory<*>>()
    override val supportedFormats get() = factories.values.mapNotNull { it as? FileAudioSourceFactory<*> }
        .flatMap { it.supportedFormats }.toSet()

    init { reload() }

    override fun createAudioSource(factory: String, source: AudioSource?): AudioSource {
        return factories[factory]?.createAudioSource(source) ?: throw NoSuchFactoryException(factory)
    }

    override fun createAudioSource(json: JsonObject): AudioSource {
        val list = arrayListOf<JsonObject>()
        var node: JsonObject? = json
        while (node != null) {
            list.add(node)
            node = node["source"] as? JsonObject
        }
        var source: AudioSource? = null
        for (i in list.lastIndex downTo 0) {
            node = list[i]
            val factory = node["factory"]!!.asString()
            val f = factories[factory] ?: throw NoSuchFactoryException(factory)
            source = f.createAudioSource(source, node)
        }
        return source ?: throw IllegalStateException("No source")
    }

    override fun createAudioSource(file: Path, factory: String?): FileAudioSource = if (factory != null) {
        val f = factories[factory] ?: throw NoSuchFactoryException(factory)
        if (f !is FileAudioSourceFactory<*>) throw UnsupportedOperationException("Factory $factory does not support files")
        f.createAudioSource(file)
    } else factories.firstNotNullOfOrNull { (_, value) ->
        if (value !is FileAudioSourceFactory<*>) return@firstNotNullOfOrNull null
        try {
            value.createAudioSource(file)
        } catch (ignored: UnsupportedOperationException) {
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    } ?: throw UnsupportedOperationException("No factory supports file $file")

    override fun createAutoWrappedAudioSource(file: Path): AudioSource = createAudioSource(file).run {
        if (length <= IN_MEMORY_FILE_SIZE) createMemorySource(this) else this
    }

    override fun createResampledSource(source: AudioSource, factory: String?) =
        createAudioSource<ResampledAudioSource, ResampledAudioSourceFactory<ResampledAudioSource>>(source, factory)

    override fun createMemorySource(source: AudioSource, factory: String?) =
        createAudioSource<MemoryAudioSource, MemoryAudioSourceFactory<MemoryAudioSource>>(source, factory)

    private inline fun <reified A: AudioSource, reified T: AudioSourceFactory<A>> createAudioSource(source: AudioSource, factory: String?): A {
        if (factory != null) {
            val f = factories[factory] ?: throw NoSuchFactoryException(factory)
            if (f !is T) throw UnsupportedOperationException("Factory $factory does not inherited from ${T::class.simpleName}")
            return f.createAudioSource(source)
        }
        return factories.firstNotNullOfOrNull { (_, value) ->
            if (value !is T) return@firstNotNullOfOrNull null
            try {
                value.createAudioSource(source)
            } catch (ignored: UnsupportedOperationException) {
                null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } ?: throw UnsupportedOperationException("No factory inherited from ${T::class.simpleName}")
    }

    override fun reload() {
        factories.clear()
        factories.putAll(ServiceLoader.load(AudioSourceFactory::class.java).associateBy { it.name })
    }
}