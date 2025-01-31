package com.eimsound.daw.utils

interface BasicEditor {
    fun delete() { }
    fun copy() { }
    fun cut() {
        copy()
        delete()
    }
    fun paste() { }
    val hasSelected get() = true
    val canPaste get() = true
}

interface MultiSelectableEditor : BasicEditor {
    fun selectAll() { }
}

interface SerializableEditor : BasicEditor {
    fun canPasteFromString(value: String) = true
    fun copyAsString(): String
    fun pasteFromString(value: String)
}
