package com.example.feature.detail.api

import com.example.core.common.presenter.ParamInit
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class Detail(val id: Int)

data class DetailState(
  val title: String = "",
  val content: String = "",
  val sourceUrl: String = "",
  val originalWord: String = "",
  val translatedWord: String = "",
  val ipa: String? = null,
)

interface DetailPresenter : ParamInit<Int> {
  val state: StateFlow<DetailState>
}
