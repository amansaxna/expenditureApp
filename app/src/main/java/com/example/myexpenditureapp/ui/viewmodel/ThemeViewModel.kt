package com.example.myexpenditureapp.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.myexpenditureapp.data.Graph
import com.example.myexpenditureapp.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeViewModel : ViewModel() {
    private val prefs by lazy {
        runCatching {
            Graph.appContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        }.getOrNull()
    }

    private val initialMode: ThemeMode
        get() {
            val saved = prefs?.getString("theme_mode", ThemeMode.SYSTEM.name)
            return runCatching { ThemeMode.valueOf(saved ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM)
        }

    private val _themeMode = MutableStateFlow(initialMode)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs?.edit()?.putString("theme_mode", mode.name)?.apply()
    }
}
