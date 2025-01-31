package com.eimsound.daw.window.panels

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.AudioProcessorManager
import com.eimsound.audioprocessor.AudioSourceManager
import com.eimsound.audioprocessor.data.midi.getNotes
import com.eimsound.audioprocessor.data.midi.toMidiTracks
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.components.*
import com.eimsound.daw.components.trees.FileNode
import com.eimsound.daw.components.trees.Tree
import com.eimsound.daw.components.utils.HorizontalResize
import com.eimsound.daw.impl.processor.eimAudioProcessorFactory
import com.eimsound.daw.processor.PreviewerAudioProcessor
import kotlinx.coroutines.*
import org.apache.commons.lang3.SystemUtils
import java.io.File
import javax.sound.midi.MidiSystem

val fileBrowserPreviewer = PreviewerAudioProcessor(AudioProcessorManager.instance.eimAudioProcessorFactory)

object FileSystemBrowser : Panel {
    override val name = "文件浏览"
    override val direction = PanelDirection.Vertical
    private val filePath = SystemUtils.getUserDir()
    private var component: (@Composable BoxScope.() -> Unit)? by mutableStateOf(null)
    private var nodeName: String? by mutableStateOf(null)

    @Composable
    override fun Icon() {
        Icon(Icons.Filled.FolderOpen, name)
    }

    @Composable
    override fun Content() {
        Column {
            Tree(Modifier.weight(1F), { if (it is File) createPreviewerComponent(it) }) {
                FileNode(filePath)
            }
            Previewer()
        }
    }

    @Composable
    private fun PlayHead(interactionSource: MutableInteractionSource, width: IntArray) {
        val isHovered by interactionSource.collectIsHoveredAsState()
        val left: Float
        val leftDp = LocalDensity.current.run {
            left = (fileBrowserPreviewer.playPosition * width[0]).toFloat() - (if (isHovered) 2 else 1) * density
            left.toDp()
        }
        val color = MaterialTheme.colorScheme.primary
        Spacer(
            Modifier.width(if (isHovered) 4.dp else 2.dp).fillMaxHeight()
                .graphicsLayer(translationX = left)
                .hoverable(interactionSource)
                .background(color)
        )
        Spacer(Modifier.width(leftDp).fillMaxHeight().background(color.copy(0.14F)))
    }

    @Composable
    private fun Previewer() {
        val width = remember { intArrayOf(1) }
        Surface(Modifier.fillMaxWidth().height(40.dp).onGloballyPositioned { width[0] = it.size.width }, tonalElevation = 3.dp) {
            @OptIn(ExperimentalFoundationApi::class)
            DropdownMenu({ close ->
                MenuItem({
                    fileBrowserPreviewer.position.isPlaying = !fileBrowserPreviewer.position.isPlaying
                    close()
                }) {
                    Text("自动播放")
                    Filled()
                    Icon(
                        if (fileBrowserPreviewer.position.isPlaying) Icons.Outlined.CheckBox
                        else Icons.Outlined.CheckBoxOutlineBlank, "自动播放"
                    )
                }
                MenuItem({
                    fileBrowserPreviewer.position.isProjectLooping = !fileBrowserPreviewer.position.isProjectLooping
                    close()
                }) {
                    Text("循环播放")
                    Filled()
                    Icon(
                        if (fileBrowserPreviewer.position.isProjectLooping) Icons.Outlined.CheckBox
                        else Icons.Outlined.CheckBoxOutlineBlank, "自动播放"
                    )
                }
                MenuItem {
                    Text("音量")
                    Filled()
                    Slider(fileBrowserPreviewer.volume, { fileBrowserPreviewer.volume = it }, Modifier.width(140.dp))
                }
                Divider()
                MenuItem(close, enabled = false, modifier = Modifier.fillMaxSize()) {
                    Text(nodeName ?: "请选择文件...", overflow = TextOverflow.Ellipsis, maxLines = 1)
                }
            }, enabled = false, matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                val c = component
                if (c == null) Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("请选择文件...", style = MaterialTheme.typography.labelMedium)
                } else Box {
                    val draggableState = remember {
                        DraggableState { fileBrowserPreviewer.playPosition += it / width[0].toDouble() }
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(Modifier.padding(horizontal = 4.dp), content = c)
                    Box(Modifier.fillMaxSize()
                        .draggable(draggableState, Orientation.Horizontal, interactionSource = interactionSource)
                        .pointerInput(Unit) {
                            detectTapGestures({
                                fileBrowserPreviewer.position.isProjectLooping =
                                    !fileBrowserPreviewer.position.isProjectLooping
                            }) { fileBrowserPreviewer.playPosition = it.x / width[0].toDouble() }
                        }
                        .pointerHoverIcon(PointerIcon.HorizontalResize)
                    ) {
                        PlayHead(interactionSource, width)
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun createPreviewerComponent(file: File) {
        GlobalScope.launch {
            if (file.isDirectory) return@launch
            val ext = file.extension.lowercase()
            var hasContent = false
            try {
                if (ext == "mid") {
                    val notes = withContext(Dispatchers.IO) {
                        MidiSystem.getSequence(file).toMidiTracks().getNotes()
                    }
                    component = { MidiView(notes) }
                    nodeName = file.name
                    fileBrowserPreviewer.setPreviewTarget(notes)
                    hasContent = true
                } else if (AudioSourceManager.instance.supportedFormats.contains(ext)) {
                    val audioSource = AudioSourceManager.instance.createAudioSource(file.toPath())
                    EchoInMirror.audioThumbnailCache[file, audioSource]?.let {
                        component = { Waveform(it) }
                        nodeName = file.name
                        hasContent = true
                    }
                    fileBrowserPreviewer.setPreviewTarget(audioSource)
                }
            } catch (e: Exception) {
                hasContent = false
                e.printStackTrace()
            }
            if (!hasContent) {
                component = null
                nodeName = null
                fileBrowserPreviewer.clear()
            }
        }
    }
}
