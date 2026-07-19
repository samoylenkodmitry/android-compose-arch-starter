package com.archstarter.shared.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Simple bootstrapper that exposes a shared CoroutineScope placeholder. The
 * implementation will evolve into the real persistence stack as the migration
 * proceeds.
 */
object SharedDataBootstrap {
  val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
