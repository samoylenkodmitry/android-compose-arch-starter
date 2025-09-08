package com.example.feature.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.common.presenter.rememberPresenter
import com.example.core.designsystem.AppTheme
import com.example.feature.settings.api.SettingsPresenter
import com.example.feature.settings.api.SettingsState
import com.example.feature.settings.api.supportedLanguages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun SettingsScreen(presenter: SettingsPresenter? = null) {
    val p = presenter ?: rememberPresenter<SettingsPresenter, Unit>()
    val state by p.state.collectAsStateWithLifecycle()
    Column(Modifier.padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Native language")
        LanguageDropdown(state.nativeLanguage, p::onNativeSelected)
        Spacer(Modifier.height(16.dp))
        Text("Learning language")
        LanguageDropdown(state.learningLanguage, p::onLearningSelected)
    }
}

@Composable
private fun LanguageDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            supportedLanguages.forEach { lang ->
                DropdownMenuItem(
                    text = { Text(lang) },
                    onClick = {
                        expanded = false
                        onSelect(lang)
                    }
                )
            }
        }
    }
}

// ---- Fake for preview ----
private class FakeSettingsPresenter : SettingsPresenter {
    private val _state = MutableStateFlow(SettingsState())
    override val state: StateFlow<SettingsState> = _state
    override fun onNativeSelected(language: String) {
        _state.value = _state.value.copy(nativeLanguage = language)
    }
    override fun onLearningSelected(language: String) {
        _state.value = _state.value.copy(learningLanguage = language)
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewSettings() {
    AppTheme { SettingsScreen(presenter = FakeSettingsPresenter()) }
}
