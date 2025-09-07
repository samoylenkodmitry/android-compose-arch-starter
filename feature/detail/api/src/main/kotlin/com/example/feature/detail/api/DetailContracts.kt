package com.example.feature.detail.api

import com.example.core.common.presenter.ParamInit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class Detail(val id: String)

data class DetailState(val item: String = "")

interface DetailPresenter : ParamInit<String> {
  val state: StateFlow<DetailState>
}
