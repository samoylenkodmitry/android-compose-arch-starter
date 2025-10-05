package com.archstarter.core.common.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

interface AssistedVmFactory<T : ViewModel> {
  fun create(handle: SavedStateHandle): T
}