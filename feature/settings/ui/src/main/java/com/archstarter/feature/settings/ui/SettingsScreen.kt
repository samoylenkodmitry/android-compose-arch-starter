package com.archstarter.feature.settings.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archstarter.core.common.presenter.rememberPresenter
import com.archstarter.core.designsystem.AppTheme
import com.archstarter.feature.settings.api.SettingsPresenter
import com.archstarter.feature.settings.api.SettingsState
import com.archstarter.feature.settings.api.supportedLanguages
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
    var searchQuery by remember { mutableStateOf("") }
    val filteredLanguages = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            supportedLanguages
        } else {
            supportedLanguages.filter { language ->
                language.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    Box {
        OutlinedButton(onClick = {
            expanded = true
            searchQuery = ""
        }) {
            Text(selected)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                searchQuery = ""
            }
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search languages") },
                singleLine = true,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(top = 8.dp)
                    .fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            AnimatedVisibility(
                visible = filteredLanguages.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(filteredLanguages, key = { it }) { lang ->
                        DropdownMenuItem(
                            text = { Text(lang) },
                            onClick = {
                                expanded = false
                                onSelect(lang)
                                searchQuery = ""
                            }
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = filteredLanguages.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                DropdownMenuItem(
                    text = { Text("No languages found") },
                    enabled = false,
                    onClick = {},
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
    override fun initOnce(params: Unit) {}
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewSettings() {
    AppTheme { SettingsScreen(presenter = FakeSettingsPresenter()) }
}
