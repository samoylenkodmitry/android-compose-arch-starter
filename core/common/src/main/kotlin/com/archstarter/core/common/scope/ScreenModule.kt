package com.archstarter.core.common.scope

import kotlinx.coroutines.flow.MutableStateFlow
import me.tatarka.inject.annotations.Inject

@ScreenScope
class ScreenBus @Inject constructor() {
  val text = MutableStateFlow("screen bus initialized")
  fun send(message: String) {
    text.value = message
  }
}