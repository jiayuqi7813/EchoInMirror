package com.eimsound.daw.i18n;


import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

interface I18n {
    fun getMessage(key: String): String
}

class I18nImpl(private val locale: Locale) : I18n {
    private val messages: ResourceBundle

    init {
        messages = ResourceBundle.getBundle("messages", locale)
    }

    override fun getMessage(key: String): String {
        return try {
            messages.getString(key)
        } catch (e: MissingResourceException) {
            "!$key!"
        }
    }
}

// 新的object

object I18nHelper {
    private val i18n: I18n = I18nImpl(Locale("zh"))

    fun getMessage(key: String): String {
        return i18n.getMessage(key)
    }
}

