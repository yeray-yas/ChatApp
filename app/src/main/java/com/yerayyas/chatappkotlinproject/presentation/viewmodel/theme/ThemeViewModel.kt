package com.yerayyas.chatappkotlinproject.presentation.viewmodel.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yerayyas.chatappkotlinproject.domain.model.ThemeMode
import com.yerayyas.chatappkotlinproject.domain.model.ThemePreferences
import com.yerayyas.chatappkotlinproject.domain.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeRepository: ThemeRepository
) : ViewModel() {

    val themePreferences: StateFlow<ThemePreferences> = themeRepository.themePreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemePreferences()
        )

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            themeRepository.updateThemeMode(themeMode)
        }
    }

    fun updateDynamicColors(useDynamicColors: Boolean) {
        viewModelScope.launch {
            themeRepository.updateDynamicColors(useDynamicColors)
        }
    }
}