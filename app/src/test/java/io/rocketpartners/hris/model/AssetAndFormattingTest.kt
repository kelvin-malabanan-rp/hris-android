package io.rocketpartners.hris.model

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class AssetAndFormattingTest {

    private val today = Instant.parse("2026-03-15T12:00:00Z")

    @Test
    fun assetReturnState_overdueDueSoonAndNone() {
        assertEquals(
            AssetAssignment.ReturnState.OVERDUE,
            AssetAssignment(id = 1, expectedReturnDate = "2026-03-14").returnState(today),
        )
        assertEquals(
            AssetAssignment.ReturnState.DUE_SOON,
            AssetAssignment(id = 1, expectedReturnDate = "2026-03-20").returnState(today),
        )
        assertEquals(
            AssetAssignment.ReturnState.NONE,
            AssetAssignment(id = 1, expectedReturnDate = "2026-04-30").returnState(today),
        )
        assertEquals(
            AssetAssignment.ReturnState.NONE,
            AssetAssignment(id = 1, expectedReturnDate = null).returnState(today),
        )
    }

    @Test
    fun formatFileSize_scalesUnits() {
        assertEquals("512 bytes", formatFileSize(512))
        assertEquals("2 KB", formatFileSize(2048))
        assertEquals("1.2 MB", formatFileSize(1_200_000))
    }

    @Test
    fun humanStatusLabel_titleCasesTokens() {
        assertEquals("Pending Manager", humanStatusLabel("PENDING_MANAGER"))
        assertEquals("In Progress", humanStatusLabel("in_progress"))
        assertEquals("Approved", humanStatusLabel("approved"))
    }
}
