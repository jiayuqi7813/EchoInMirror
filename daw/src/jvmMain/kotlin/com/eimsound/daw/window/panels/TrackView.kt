package com.eimsound.daw.window.panels

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eimsound.audioprocessor.AudioProcessorEditor
import com.eimsound.daw.api.EchoInMirror
import com.eimsound.daw.api.processor.Bus
import com.eimsound.daw.api.processor.TrackAudioProcessorWrapper
import com.eimsound.daw.api.window.Panel
import com.eimsound.daw.api.window.PanelDirection
import com.eimsound.daw.components.BasicAudioParameterView
import com.eimsound.daw.components.CustomCheckbox
import com.eimsound.daw.components.utils.clickableWithIcon
import com.eimsound.daw.components.utils.toOnSurfaceColor

@Composable
private fun CardHeader(p: TrackAudioProcessorWrapper, index: Int) {
//    val shape = MaterialTheme.shapes.small.copy(bottomStart = CornerSize(0.dp), bottomEnd = CornerSize(0.dp))
//    val backgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(LocalAbsoluteTonalElevation.current)
    Surface(Modifier.clickableWithIcon(onClick = p::onClick)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            val isBypassed = p.isBypassed
            Row(Modifier.weight(1F).padding(horizontal = 12.dp)) {
                Text("$index.", Modifier.padding(end = 6.dp),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(p.name, Modifier.weight(1F), style = MaterialTheme.typography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isBypassed) TextDecoration.LineThrough else TextDecoration.None,
                    color = LocalContentColor.current.copy(alpha = if (isBypassed) 0.7F else 1F))
            }
            CustomCheckbox(!isBypassed, { p.isBypassed = !it }, Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun AudioProcessorEditor(index: Int, p: TrackAudioProcessorWrapper) {
//    stickyHeader(p) { CardHeader(p, index) }
//    item(p.processor) {
//    val shape = MaterialTheme.shapes.small.copy(topStart = CornerSize(0.dp), topEnd = CornerSize(0.dp))
    Surface(Modifier.fillMaxWidth().padding(8.dp), MaterialTheme.shapes.small,
        tonalElevation = 5.dp, shadowElevation = 2.dp) {
        Column {
            CardHeader(p, index)
            Divider()
            if (p is AudioProcessorEditor) p.Editor()
            else if (p.parameters.isNotEmpty()) BasicAudioParameterView(p)
            else Text("未知的处理器: ${p.name}", Modifier.padding(16.dp, 50.dp), textAlign = TextAlign.Center)
        }
    }
//    }
}

@Composable
private fun TrackName() {
    val track = EchoInMirror.selectedTrack
    val color by animateColorAsState(if (track is Bus) MaterialTheme.colorScheme.primary
        else track?.color ?: MaterialTheme.colorScheme.surface, tween(100))
    Surface(Modifier.fillMaxWidth(), shadowElevation = 2.dp, tonalElevation = 4.dp, color = color) {
        Text(track?.name ?: "未选择", color = color.toOnSurfaceColor(), style = MaterialTheme.typography.labelLarge,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(8.dp, 6.dp),
            textAlign = TextAlign.Center)
    }
}

object TrackView : Panel {
    override val name = "轨道视图"
    override val direction = PanelDirection.Vertical

    @Composable
    override fun Icon() {
        Icon(Icons.Default.ViewDay, name)
    }

    @Composable
    override fun Content() {
        Column {
            TrackName()
            Box(Modifier.fillMaxSize()) {
                val state = rememberLazyListState()
                LazyColumn(state = state) {
                    val track = EchoInMirror.selectedTrack
                    if (track != null) {
                        itemsIndexed(track.preProcessorsChain) { index, item ->
                            AudioProcessorEditor(index, item)
                        }
                        item {
                            if (track.postProcessorsChain.isNotEmpty())
                                Divider(Modifier.padding(horizontal = 16.dp), 2.dp, MaterialTheme.colorScheme.primary)
                        }
                        itemsIndexed(track.postProcessorsChain) { index, item ->
                            AudioProcessorEditor(index, item)
                        }
                    }
                }
                VerticalScrollbar(rememberScrollbarAdapter(state), Modifier.align(Alignment.CenterEnd).fillMaxHeight())
            }
        }
    }
}
