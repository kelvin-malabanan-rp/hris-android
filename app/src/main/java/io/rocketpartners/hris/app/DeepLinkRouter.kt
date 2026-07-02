package io.rocketpartners.hris.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds a single pending [DeepLinkTarget] for the tab host to consume. Mirrors iOS `DeepLinkRouter`:
 * `MainActivity` publishes a target parsed from a launch/notification intent, and `MainTabScaffold`
 * observes [pending], acts on it, then calls [clear]. Lives in [AppEnvironment] so the same instance
 * is shared between the activity and the composition.
 */
class DeepLinkRouter {
    private val _pending = MutableStateFlow<DeepLinkTarget?>(null)
    val pending: StateFlow<DeepLinkTarget?> = _pending.asStateFlow()

    fun submit(target: DeepLinkTarget) { _pending.value = target }

    fun clear() { _pending.value = null }
}
