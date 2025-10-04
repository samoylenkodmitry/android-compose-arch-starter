package com.archstarter.feature.settings.impl.language

import com.archstarter.core.common.scope.ScreenScope
import com.archstarter.feature.settings.api.LanguageSelectionEvent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import me.tatarka.inject.annotations.Inject

@ScreenScope
class LanguageSelectionBus @Inject constructor() {
    private val _selections = MutableSharedFlow<LanguageSelectionEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val selections: SharedFlow<LanguageSelectionEvent> = _selections

    fun publish(event: LanguageSelectionEvent) {
        _selections.tryEmit(event)
    }
}
