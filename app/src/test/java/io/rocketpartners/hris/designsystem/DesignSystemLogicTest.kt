package io.rocketpartners.hris.designsystem

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DesignSystemLogicTest {

    @Test
    fun statusColor_mapsFamiliesToleratingLabelAndCase() {
        assertEquals(Color(0xFF065F46), Theme.statusColor("APPROVED", dark = false))
        assertEquals(Color(0xFF34D399), Theme.statusColor("approved", dark = true))
        assertEquals(Color(0xFFB91C1C), Theme.statusColor("REJECTED", dark = false))
        assertEquals(Color(0xFFB91C1C), Theme.statusColor("declined", dark = false))
        assertEquals(Color(0xFF4B5563), Theme.statusColor("PENDING_CANCELLATION_but_cancel", dark = false))
        assertEquals(Color(0xFF9A3412), Theme.statusColor("PENDING_MANAGER", dark = false))
    }

    @Test
    fun statusColor_unknownFallsBackToNeutralGrey() {
        assertEquals(Color(0xFF6B7280), Theme.statusColor("mystery", dark = false))
        assertEquals(Color(0xFF9CA3AF), Theme.statusColor("mystery", dark = true))
    }

    @Test
    fun hexColor_parsesSixAndThreeDigitAndRejectsGarbage() {
        assertEquals(Color(0x00, 0x7A, 0xFF), hexColor("#007AFF"))
        assertEquals(Color(0xFF, 0xFF, 0xFF), hexColor("fff"))
        assertNull(hexColor(null))
        assertNull(hexColor("#12"))
        assertNull(hexColor("#GGGGGG"))
    }

    @Test
    fun progressMath_clampsAndGuardsZeroTotal() {
        assertEquals(0.5, ProgressMath.fraction(5.0, 10.0), 0.0001)
        assertEquals(1.0, ProgressMath.fraction(15.0, 10.0), 0.0001)
        assertEquals(0.0, ProgressMath.fraction(-1.0, 10.0), 0.0001)
        assertEquals(0.0, ProgressMath.fraction(5.0, 0.0), 0.0001)
    }
}
