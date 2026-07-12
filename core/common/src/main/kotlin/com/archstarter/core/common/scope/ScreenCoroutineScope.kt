package com.archstarter.core.common.scope

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

/**
 * Lifecycle-aware coroutine scope owned by a [ScreenComponent].
 *
 * The scope backing [screenScope] behaves like this:
 * - If a [Lifecycle] is attached: it pauses (cancels every child job) when the screen goes
 *   off-screen (ON_STOP) and resumes (recreates the backing job) when it comes back (ON_START).
 * - If no lifecycle is attached: it lives for as long as the component does and is only cancelled
 *   when the component is disposed (see [cancel]).
 *
 * This is what lets flows collected in `viewModelScope` (via [ScreenViewModel]) stop collecting
 * while the screen is not visible, instead of leaking a stale subscription that keeps firing.
 */
@ScreenScope
class ScreenScopeHolder @Inject constructor() {
  private val id = System.identityHashCode(this)

  // Backing job that all coroutines launched from screenScope() are children of.
  @Volatile
  private var componentJob: Job? = null

  @Volatile
  private var lifecycle: Lifecycle? = null

  private val lifecycleObserver = LifecycleEventObserver { _, event ->
    when (event) {
      Lifecycle.Event.ON_STOP -> {
        log("ON_STOP - cancelling lifecycle-aware scope")
        componentJob?.cancel()
        componentJob = null
      }
      Lifecycle.Event.ON_START -> {
        if (componentJob?.isActive != true) {
          log("ON_START - recreating lifecycle-aware scope")
          componentJob = SupervisorJob()
        }
      }
      else -> {}
    }
  }

  @Synchronized
  private fun getJob(): Job {
    if (componentJob?.isActive != true) {
      componentJob = SupervisorJob()
    }
    return componentJob!!
  }

  /**
   * A [CoroutineScope] tied to the current backing job. Coroutines launched from it are cancelled
   * when the screen stops (if a lifecycle is attached) or when the component is disposed.
   */
  fun screenScope(): CoroutineScope = CoroutineScope(getJob() + Dispatchers.Main.immediate)

  /** Attach a [Lifecycle] to enable automatic pause/resume of coroutines. Idempotent. */
  fun attachLifecycle(lifecycle: Lifecycle) {
    if (this.lifecycle == lifecycle) return
    this.lifecycle?.removeObserver(lifecycleObserver)
    this.lifecycle = lifecycle
    log("lifecycle attached, state=${lifecycle.currentState}")
    lifecycle.addObserver(lifecycleObserver)
  }

  /** Cancel the component-level scope. Called when the owning [ScreenComponent] is disposed. */
  fun cancel() {
    log("cancelled - all scopes will be cancelled")
    lifecycle?.removeObserver(lifecycleObserver)
    lifecycle = null
    componentJob?.cancel()
    componentJob = null
  }

  fun isActive(): Boolean = componentJob?.isActive == true

  private fun log(msg: String) = Log.d("ScreenScopeHolder-$id", msg)
}
