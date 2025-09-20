package com.archstarter.feature.settings.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.archstarter.core.common.presenter.rememberPresenter
import com.archstarter.core.designsystem.AppTheme
import com.archstarter.feature.settings.api.LanguageChooserParams
import com.archstarter.feature.settings.api.LanguageChooserPresenter
import com.archstarter.feature.settings.api.LanguageChooserRole
import com.archstarter.feature.settings.api.LanguageChooserState
import com.archstarter.feature.settings.api.SettingsPresenter
import com.archstarter.feature.settings.api.SettingsState
import com.archstarter.feature.settings.api.supportedLanguages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@Composable
fun SettingsScreen(
    presenter: SettingsPresenter? = null,
    nativeLanguagePresenter: LanguageChooserPresenter? = null,
    learningLanguagePresenter: LanguageChooserPresenter? = null,
) {
    val p = presenter ?: rememberPresenter<SettingsPresenter, Unit>()
    val state by p.state.collectAsStateWithLifecycle()

    val nativeChooser = nativeLanguagePresenter
        ?: rememberLanguageChooserPresenter(LanguageChooserRole.Native, state.nativeLanguage)
    val learningChooser = learningLanguagePresenter
        ?: rememberLanguageChooserPresenter(LanguageChooserRole.Learning, state.learningLanguage)

    Column(Modifier.padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text("Native language")
        Spacer(Modifier.height(4.dp))
        LanguageChooserField(presenter = nativeChooser)
        Spacer(Modifier.height(16.dp))
        Text("Learning language")
        Spacer(Modifier.height(4.dp))
        LanguageChooserField(presenter = learningChooser)
    }
}

@Composable
private fun rememberLanguageChooserPresenter(
    role: LanguageChooserRole,
    selected: String,
): LanguageChooserPresenter {
    return rememberPresenter<LanguageChooserPresenter, LanguageChooserParams>(
        key = "language_${role.name}",
        params = LanguageChooserParams(role = role, selectedLanguage = selected),
    )
}

@Composable
private fun LanguageChooserField(
    presenter: LanguageChooserPresenter,
) {
    val state by presenter.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(state.isExpanded, state.query) {
        if (state.isExpanded) {
            listState.scrollToItem(0)
        }
    }

    val errorMessage = state.errorMessage
    val results = state.results

    Box {
        OutlinedButton(onClick = presenter::onToggleExpanded) {
            val label = if (state.selectedLanguage.isBlank()) "Select language" else state.selectedLanguage
            Text(label)
        }
        DropdownMenu(
            expanded = state.isExpanded,
            onDismissRequest = presenter::onDismiss,
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = presenter::onQueryChange,
                placeholder = { Text("Search languages") },
                singleLine = true,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {
                    DropdownMenuItem(
                        text = {
                            Column(Modifier.fillMaxWidth()) {
                                Text(errorMessage, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(4.dp))
                                Text("Tap to retry", style = MaterialTheme.typography.labelSmall)
                            }
                        },
                        onClick = presenter::onRetry,
                    )
                }

                results.isEmpty() -> {
                    DropdownMenuItem(
                        text = { Text("No languages found") },
                        enabled = false,
                        onClick = {},
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                    ) {
                        items(results) { language ->
                            DropdownMenuItem(
                                text = { Text(language) },
                                onClick = { presenter.onSelect(language) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---- Fake for preview ----
private class FakeSettingsPresenter : SettingsPresenter {
    private val _state = MutableStateFlow(SettingsState())
    override val state: StateFlow<SettingsState> = _state
    override fun onNativeSelected(language: String) {
        _state.update { current -> current.copy(nativeLanguage = language) }
    }
    override fun onLearningSelected(language: String) {
        _state.update { current -> current.copy(learningLanguage = language) }
    }
    override fun initOnce(params: Unit) {}
}

private class FakeLanguageChooserPresenter(
    initial: String,
) : LanguageChooserPresenter {
    private val languages = supportedLanguages.take(6)
    private val _state = MutableStateFlow(
        LanguageChooserState(
            selectedLanguage = initial,
            results = languages,
        ),
    )
    override val state: StateFlow<LanguageChooserState> = _state

    override fun initOnce(params: LanguageChooserParams) {
        _state.update { it.copy(selectedLanguage = params.selectedLanguage) }
    }

    override fun onToggleExpanded() {
        _state.update { it.copy(isExpanded = !it.isExpanded) }
    }

    override fun onDismiss() {
        _state.update { it.copy(isExpanded = false) }
    }

    override fun onQueryChange(query: String) {
        _state.update {
            it.copy(
                query = query,
                results = if (query.isBlank()) languages else languages.filter {
                    it.contains(query, ignoreCase = true)
                },
            )
        }
    }

    override fun onSelect(language: String) {
        _state.update { it.copy(selectedLanguage = language, isExpanded = false) }
    }

    override fun onRetry() {}
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun PreviewSettings() {
    AppTheme {
        val native = remember { FakeLanguageChooserPresenter("English") }
        val learning = remember { FakeLanguageChooserPresenter("Spanish") }
        SettingsScreen(
            presenter = FakeSettingsPresenter(),
            nativeLanguagePresenter = native,
            learningLanguagePresenter = learning,
        )
    }
}
