package io.rocketpartners.hris.designsystem

/** Time-of-day greeting text. Mirrors iOS `Greeting`. */
object Greeting {
    fun text(hour: Int): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}
