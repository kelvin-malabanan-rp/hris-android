package io.rocketpartners.hris.feature.timeoff

import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.feature.wfh.WfhApprovalsStore
import io.rocketpartners.hris.model.WfhSchedule
import io.rocketpartners.hris.support.FakeWfhRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApprovalsStoreTest {

    private val pending = listOf(
        WfhSchedule(id = 1, date = "2026-07-06", userName = "Maria"),
        WfhSchedule(id = 2, date = "2026-07-08", userName = "James"),
    )

    @Test
    fun load_populatesPending() = runTest {
        val store = WfhApprovalsStore(FakeWfhRepository(pendingApprovalsResult = Result.success(pending)), scope = this)
        store.load()
        assertEquals(Phase.Loaded, store.state.value.phase)
        assertEquals(2, store.state.value.pending.size)
    }

    @Test
    fun approve_removesOptimisticallyThenCommitsAfterWindow() = runTest {
        val store = WfhApprovalsStore(
            FakeWfhRepository(pendingApprovalsResult = Result.success(pending)),
            scope = this,
            undoWindowMs = 50,
        )
        store.load()
        store.approve(pending.first())
        // Optimistically removed + undo toast shown.
        assertEquals(1, store.state.value.pending.size)
        assertEquals("Approved", store.state.value.undoMessage)
        store.waitForPendingCommits()
        // Committed: toast cleared, row stays gone.
        assertNull(store.state.value.undoMessage)
        assertEquals(1, store.state.value.pending.size)
    }

    @Test
    fun undo_cancelsCommitAndRestoresRow() = runTest {
        val store = WfhApprovalsStore(
            FakeWfhRepository(pendingApprovalsResult = Result.success(pending)),
            scope = this,
            undoWindowMs = 10_000,
        )
        store.load()
        store.reject(pending.first())
        assertEquals(1, store.state.value.pending.size)
        store.undo()
        assertEquals(2, store.state.value.pending.size)
        assertNull(store.state.value.undoMessage)
        assertTrue(store.state.value.pending.any { it.id == 1 })
    }
}
