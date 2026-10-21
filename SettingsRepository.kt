package com.athaniqraq.app.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore("athan_settings")

class SettingsRepository(private val context: Context) {
    private val tasbihKey = intPreferencesKey("tasbih_count")
    private val targetKey = intPreferencesKey("tasbih_target")
    private val maghribOffsetKey = intPreferencesKey("maghrib_offset")
    private val fontSizeKey = intPreferencesKey("dua_font_size")
    private val languageKey = stringPreferencesKey("language")
    private val bookmarksKey = stringSetPreferencesKey("bookmarks")

    val tasbih = context.store.data.map { it[tasbihKey] ?: 0 }
    val target = context.store.data.map { it[targetKey] ?: 33 }
    val maghribOffset = context.store.data.map { it[maghribOffsetKey] ?: 12 }
    val fontSize = context.store.data.map { it[fontSizeKey] ?: 22 }
    val language = context.store.data.map { it[languageKey] ?: "ar" }
    val bookmarks = context.store.data.map { it[bookmarksKey] ?: emptySet() }

    suspend fun setTasbih(v: Int) { context.store.edit { it[tasbihKey] = v.coerceAtLeast(0) } }
    suspend fun setTarget(v: Int) { context.store.edit { it[targetKey] = v } }
    suspend fun setOffset(v: Int) { context.store.edit { it[maghribOffsetKey] = v.coerceIn(-30, 60) } }
    suspend fun setFontSize(v: Int) { context.store.edit { it[fontSizeKey] = v.coerceIn(16, 36) } }
    suspend fun setLanguage(v: String) { context.store.edit { it[languageKey] = v } }
    suspend fun toggleBookmark(id: String) { context.store.edit { val s = (it[bookmarksKey] ?: emptySet()).toMutableSet(); if (!s.add(id)) s.remove(id); it[bookmarksKey] = s } }
}
