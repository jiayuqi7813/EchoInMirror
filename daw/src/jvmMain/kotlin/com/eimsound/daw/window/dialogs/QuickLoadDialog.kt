package com.eimsound.daw.window.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.eimsound.audioprocessor.AudioProcessorDescription
import com.eimsound.daw.EchoInMirror
import com.eimsound.daw.components.MenuItem
import com.eimsound.daw.components.Scrollable
import java.awt.Dimension

private fun closeQuickLoadWindow() {
    EchoInMirror.windowManager.dialogs[QuickLoadDialog] = false
}

val TOP_TEXTFIELD_HEIGHT = 40.dp
val BOTTOM_TEXTFIELD_HEIGHT = 40.dp

@OptIn(ExperimentalMaterial3Api::class)
val QuickLoadDialog = @Composable {
    val descriptions = EchoInMirror.audioProcessorManager.factories.values.flatMap { it.descriptions }.sortedBy { it.name }.distinct() // 所有插件

    var selectedFactory by mutableStateOf<String?>(null)
    var selectedCategory by mutableStateOf<String?>(null)
    var selectedInstrument by mutableStateOf<Boolean?>(null)
    var selectedDescription by mutableStateOf<AudioProcessorDescription?>(null)

    // 根据已选的厂商、类别、乐器过滤插件，显示在最后一列
    val descList = descriptions.filter {
        (selectedFactory == null || it.manufacturerName == selectedFactory) &&
        (selectedCategory == null || it.category == selectedCategory) &&
        (selectedInstrument == null || it.isInstrument == selectedInstrument)
    }

    Dialog(::closeQuickLoadWindow, title = "快速加载") {
        window.minimumSize = Dimension(860, 700)
        window.isModal = false

        Scaffold (
            Modifier.fillMaxSize(),
            topBar = {
                Surface(Modifier.fillMaxWidth().height(TOP_TEXTFIELD_HEIGHT)) {

                }
            },
            content = {
                Row(Modifier.fillMaxSize().padding(top= TOP_TEXTFIELD_HEIGHT, bottom = BOTTOM_TEXTFIELD_HEIGHT), horizontalArrangement = Arrangement.SpaceEvenly) {
                    DescLister(
                        modifier = Modifier.weight(1f),
                        descList = descriptions.mapNotNull { it.category }.distinct(),
                        onClick = { selectedCategory = it },
                        selectedDesc = selectedCategory,
                        defaultText = "所有类别"
                    )
                    DescLister(
                        modifier = Modifier.weight(1f),
                        descList = descriptions.mapNotNull { it.manufacturerName }.distinct(),
                        onClick = { selectedFactory = it },
                        selectedDesc = selectedFactory,
                        defaultText = "所有厂商"
                    )

                    Surface(Modifier.weight(1f)) {
                        Scrollable(true, false) {
                            Column {
                                descriptions.filter {
                                    (selectedFactory == null || it.manufacturerName == selectedFactory) &&
                                            (selectedCategory == null || it.category == selectedCategory) &&
                                            (selectedInstrument == null || it.isInstrument == selectedInstrument) }.forEach {description ->
                                    val isSelected = selectedDescription?.name == description.name
                                    MenuItem( isSelected,
                                        modifier = Modifier.fillMaxSize(),
                                        onClick = {
                                            if (isSelected) {
                                                selectedDescription = null // 这行后续可以删除
                                                // 已选中时再点击触发插件添加请求
                                                // EchoInMirror.Track.addAudioProcessor(description) （不存在的API（
                                            } else {
                                                selectedDescription = description
                                            }
                                        }
                                    ){
                                        Text(description.name, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start, maxLines = 1)
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(Modifier.fillMaxWidth().height(BOTTOM_TEXTFIELD_HEIGHT)) {

                }
            }
        )
    }
}

@Composable
fun DescLister(
    modifier: Modifier = Modifier,
    descList: List<String>,
    onClick: (clickString: String?) -> Unit,
    selectedDesc: String?,
    defaultText: String
) {
    Surface(modifier = modifier) {
        Scrollable(vertical = true, horizontal = false) {
            Column {
                MenuItem(selectedDesc == null,
                    modifier = Modifier.fillMaxSize(),
                    onClick = {
                        onClick(null)
                    }
                ){
                    Text(defaultText, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start, maxLines = 1)
                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(10.dp))
                }
                descList.forEach {description ->
                    val isSelected = selectedDesc == description
                    MenuItem( isSelected,
                        modifier = Modifier.fillMaxSize(),
                        onClick = {
                                onClick(description)
                        }
                    ){
                        Text(description, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start, maxLines = 1)
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(10.dp))
                    }
                }
            }
        }
    }
}
