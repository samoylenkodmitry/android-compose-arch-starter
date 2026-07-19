package com.archstarter.shared.platform

/**
 * Identifies the platform at runtime. Each target will provide an `actual`
 * implementation while we incrementally port services out of the Android app
 * module.
 */
expect fun currentPlatformLabel(): String
