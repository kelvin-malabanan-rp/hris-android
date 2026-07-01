package io.rocketpartners.hris.app

import android.app.Application

/**
 * Process-wide Application. The composition root ([AppEnvironment]) is created lazily and
 * held here so a single object graph is shared across the process — mirrors `HRISApp`
 * creating one `AppEnvironment` on iOS.
 */
class HrisApp : Application()
