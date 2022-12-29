package cn.apisium.eim.window.panels.playlist

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.zIndex
import cn.apisium.eim.EchoInMirror
import cn.apisium.eim.api.Track
import cn.apisium.eim.components.*
import cn.apisium.eim.components.icons.Crown
import cn.apisium.eim.components.icons.DebugStepOver
import cn.apisium.eim.utils.onClick
import cn.apisium.eim.window.dialogs.openColorPicker
import cn.apisium.eim.window.panels.editor.calcScroll

private val TRACK_ITEM_ICON_SIZE = Modifier.size(16.dp)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TrackItem(track: Track, height: Dp, index: Int, depth: Int = 0) {
    Row(Modifier.height(height).padding(start = (depth * 8).dp).onPointerEvent(PointerEventType.Press) {
        EchoInMirror.selectedTrack = track
    }) {
        Box {
            val color by animateColorAsState(track.color, tween(100))
            Canvas(Modifier.fillMaxHeight().width(8.dp).background(color.copy(alpha = 0.5F)).onClick {
                openColorPicker(track.color) { track.color = it }
            }) {
                val y = size.height * (1F - track.levelMeter.maxLevel.toPercentage())
                drawRect(color, Offset(0F, y), Size(size.width, size.height - y))
            }
        }
        Row(Modifier.padding(8.dp, 4.dp)) {
            Text(index.toString(),
                Modifier.width(20.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Column(Modifier.fillMaxHeight(), Arrangement.SpaceBetween) {
                Text(track.name,
                    style = MaterialTheme.typography.labelLarge,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                SegmentedButtons {
                    SegmentedButton({ track.isMute = !track.isMute }, track.isMute, false) {
                        Icon(if (track.isMute) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, null, TRACK_ITEM_ICON_SIZE)
                    }
                    SegmentedDivider()
                    SegmentedButton({ track.isSolo = !track.isSolo }, track.isSolo, false) {
                        Icon(Crown, null, TRACK_ITEM_ICON_SIZE)
                    }
                    SegmentedDivider()
                    SegmentedButton({ track.isDisabled = !track.isDisabled }, track.isDisabled, false) {
                        Icon(DebugStepOver, null, TRACK_ITEM_ICON_SIZE)
                    }
                }
                VolumeSlider(track, Modifier.fillMaxWidth().offset((-4).dp), false)
            }
        }
    }
    track.subTracks.forEachIndexed { i, it -> key(it.uuid) {
        Divider(Modifier.offset(8.dp * (depth + 1)))
        TrackItem(it, height, i + 1, depth + 1)
    } }
}

var noteWidth = mutableStateOf(0.2.dp)

@Composable
private fun TrackContent(height: Dp = 70.dp, track: Track, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(height)) {
        track.notes.read()
        track.notes.forEach {
            Box(Modifier.height(height / 128).width(noteWidth.value * it.duration)
                .offset(x = noteWidth.value * it.time, y = height - height / 128 * it.note)
                .background(track.color))
        }
    }
    Divider()
    track.subTracks.forEach { key(it.uuid) { TrackContent(height, it, modifier) } }
}

val verticalScrollState = ScrollState(0)
val horizontalScrollState = ScrollState(0)

@Suppress("DuplicatedCode")
private suspend fun PointerInputScope.handleMouseEvent() {
    forEachGesture {
        awaitPointerEventScope {
            var event: PointerEvent
            do {
                event = awaitPointerEvent(PointerEventPass.Initial)
                when (event.type) {
                    PointerEventType.Scroll -> {
                        calcScroll(event, noteWidth, horizontalScrollState)
                        continue
                    }
                    else -> {}
                }
            } while (!event.changes.fastAll(PointerInputChange::changedToDownIgnoreConsumed))
            val down = event.changes[0]

            var drag: PointerInputChange?
            do {
                @Suppress("INVISIBLE_MEMBER")
                drag = awaitPointerSlopOrCancellation(down.id, down.type,
                    triggerOnMainAxisSlop = false) { change, _ -> change.consume() }
            } while (drag != null && !drag.isConsumed)
            if (drag == null) return@awaitPointerEventScope
            drag(drag.id) {
                it.consume()
            }
        }
    }
}

@Composable
fun Playlist() {
    Row {
        val trackHeight = 70.dp
        Surface(Modifier.width(200.dp).fillMaxHeight().zIndex(5f), shadowElevation = 2.dp, tonalElevation = 2.dp) {
            Column {
                Surface(shadowElevation = 2.dp, tonalElevation = 4.dp) {
                    Row(Modifier.height(TIMELINE_HEIGHT).fillMaxWidth().padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        cn.apisium.eim.components.silder.Slider(
                            noteWidth.value.value / 0.4f,
                            { noteWidth.value = 0.4.dp * it },
                            valueRange = 0.15f..8f
                        )
                    }
                }
                Column(Modifier.verticalScroll(verticalScrollState)) {
                    Divider()
                    EchoInMirror.bus.subTracks.forEachIndexed { i, it ->
                        key(it.uuid) {
                            TrackItem(it, trackHeight, i + 1)
                        }
                    }
                    Divider()
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            Column {
                var contentWidth by remember { mutableStateOf(0.dp) }
                var cursorOffsetY by remember { mutableStateOf(0.dp) }
                val localDensity = LocalDensity.current
                Timeline(Modifier.zIndex(3f), noteWidth, horizontalScrollState, true)
                Box(Modifier.weight(1f).onGloballyPositioned {
                    with(localDensity) {
                        contentWidth = it.size.width.toDp()
                        cursorOffsetY = it.size.height.toDp()
                    }
                }.pointerInput(Unit) { handleMouseEvent() }) {
                    EditorGrid(noteWidth, horizontalScrollState)
                    Column(Modifier.horizontalScroll(horizontalScrollState).verticalScroll(verticalScrollState)
                        .width(10000.dp)) {
                        Divider()
                        EchoInMirror.bus.subTracks.forEach {
                            key(it.uuid) { TrackContent(trackHeight, it) }
                        }
                    }
                    PlayHead(noteWidth, horizontalScrollState, contentWidth)
                }
            }
            HorizontalScrollbar(rememberScrollbarAdapter(horizontalScrollState),
                Modifier.align(Alignment.TopStart).fillMaxWidth())
        }
    }
}