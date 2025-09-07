package com.example.feature.detail.api

import com.example.core.common.presenter.ParamInit
import kotlinx.coroutines.flow.StateFlow

data class DetailState(val item: String = "")

interface DetailPresenter : ParamInit<String> {
  val state: StateFlow<DetailState>
}
