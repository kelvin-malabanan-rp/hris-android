package io.rocketpartners.hris.designsystem

/** Clamped progress fraction (0..1) for rings/bars. Mirrors iOS `ProgressMath`. */
object ProgressMath {
    fun fraction(value: Double, total: Double): Double {
        if (total <= 0) return 0.0
        return (value / total).coerceIn(0.0, 1.0)
    }
}
