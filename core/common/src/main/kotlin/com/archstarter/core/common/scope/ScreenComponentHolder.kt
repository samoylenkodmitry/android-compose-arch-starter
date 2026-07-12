package com.archstarter.core.common.scope

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dagger.hilt.EntryPoints

/**
 * A [ViewModel] that owns the [ScreenComponent] instance so its lifetime matches the hosting
 * ViewModelStore (i.e. the NavBackStackEntry):
 * - survives recompositions and configuration changes,
 * - is only disposed when the entry leaves the back stack,
 * - is rebuilt when the owning activity/App instance changes (e.g. font-scale recreation), so
 *   Hilt-provided singletons captured by the old graph don't go stale.
 */
class ScreenComponentHolder(
  private val builder: ScreenComponent.Builder,
  private var appInstanceId: Int,
) : ViewModel() {

  var component: ScreenComponent by mutableStateOf(builder.build())
    private set

  init {
    log("created component inst=${System.identityHashCode(component)}, appInstanceId=$appInstanceId")
  }

  /** Attach the [LifecycleOwner]'s lifecycle so screen-scoped coroutines pause/resume with it. */
  fun attachLifecycle(lifecycleOwner: LifecycleOwner) {
    try {
      scopeHolder(component).attachLifecycle(lifecycleOwner.lifecycle)
      log("attached lifecycle from $lifecycleOwner")
    } catch (e: Exception) {
      log("error attaching lifecycle: ${e.message}")
    }
  }

  /** Rebuild the component when the App/activity instance changes, cancelling the old graph. */
  fun updateAppInstance(newAppInstanceId: Int) {
    if (appInstanceId == newAppInstanceId) return

    val oldComponent = component
    val oldInst = System.identityHashCode(oldComponent)
    cancelComponent(oldComponent, "app instance changed from $appInstanceId to $newAppInstanceId")

    component = builder.build()
    appInstanceId = newAppInstanceId
    log("rebuilt component oldInst=$oldInst, newInst=${System.identityHashCode(component)}, appInstanceId=$appInstanceId")
  }

  override fun onCleared() {
    log("onCleared() - cancelling component inst=${System.identityHashCode(component)}")
    cancelComponent(component, "holder cleared")
    super.onCleared()
  }

  private fun cancelComponent(component: ScreenComponent, reason: String) {
    try {
      scopeHolder(component).cancel()
      log("cancelled component inst=${System.identityHashCode(component)} because $reason")
    } catch (e: Exception) {
      log("error cancelling scopeHolder: ${e.message}")
    }
  }

  private fun scopeHolder(component: ScreenComponent): ScreenScopeHolder =
    EntryPoints.get(component, ScreenScopeHolderEntryPoint::class.java).screenScopeHolder()

  private fun log(msg: String) = Log.d("ScreenComponentHolder", msg)

  companion object {
    fun factory(builder: ScreenComponent.Builder, appInstanceId: Int) = viewModelFactory {
      initializer { ScreenComponentHolder(builder, appInstanceId) }
    }
  }
}
