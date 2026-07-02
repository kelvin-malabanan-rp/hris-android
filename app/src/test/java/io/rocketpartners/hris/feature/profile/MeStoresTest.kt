package io.rocketpartners.hris.feature.profile

import io.rocketpartners.hris.core.networking.ApiError
import io.rocketpartners.hris.core.ui.Phase
import io.rocketpartners.hris.feature.assets.AssetRepository
import io.rocketpartners.hris.feature.assets.MyAssetsStore
import io.rocketpartners.hris.feature.payslips.PayslipRepository
import io.rocketpartners.hris.feature.payslips.PayslipsStore
import io.rocketpartners.hris.model.AssetAssignment
import io.rocketpartners.hris.model.Payslip
import io.rocketpartners.hris.model.UserProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeProfileRepo(
    var profileResult: Result<UserProfile> = Result.success(UserProfile(id = 1)),
    var updateResult: Result<UserProfile> = Result.success(UserProfile(id = 1)),
    var passwordResult: Result<Unit> = Result.success(Unit),
) : ProfileRepository {
    override suspend fun profile() = profileResult.getOrThrow()
    override suspend fun updateProfile(update: ProfileUpdate) = updateResult.getOrThrow()
    override suspend fun changePassword(current: String, new: String, confirm: String) { passwordResult.getOrThrow() }
}

private class FakePayslipRepo(
    var listResult: Result<List<Payslip>> = Result.success(emptyList()),
    var downloadResult: Result<ByteArray> = Result.success(ByteArray(0)),
) : PayslipRepository {
    override suspend fun myPayslips(payPeriodId: Int?) = listResult.getOrThrow()
    override suspend fun downloadData(id: Int) = downloadResult.getOrThrow()
}

private class FakeAssetRepo(var result: Result<List<AssetAssignment>> = Result.success(emptyList())) : AssetRepository {
    override suspend fun myAssets() = result.getOrThrow()
}

class MeStoresTest {

    @Test
    fun profileStore_loadThenSaveUpdatesProfile() = runTest {
        val repo = FakeProfileRepo(
            profileResult = Result.success(UserProfile(id = 1, firstName = "Angelo")),
            updateResult = Result.success(UserProfile(id = 1, firstName = "Angel")),
        )
        val store = ProfileStore(repo)
        store.load()
        assertEquals(Phase.Loaded, store.state.value.phase)
        assertEquals("Angelo", store.state.value.profile?.firstName)
        assertNull(store.save(ProfileUpdate(firstName = "Angel")))
        assertEquals("Angel", store.state.value.profile?.firstName)
    }

    @Test
    fun profileStore_saveFailureReturnsMessage() = runTest {
        val store = ProfileStore(FakeProfileRepo(updateResult = Result.failure(ApiError.Server("nope", 400))))
        assertEquals("nope", store.save(ProfileUpdate(city = "Makati")))
    }

    @Test
    fun payslipsStore_periodsAndFilter() = runTest {
        val repo = FakePayslipRepo(
            listResult = Result.success(
                listOf(
                    Payslip(id = 1, payPeriodLabel = "June 2026"),
                    Payslip(id = 2, payPeriodLabel = "May 2026"),
                    Payslip(id = 3, payPeriodLabel = "June 2026"),
                ),
            ),
        )
        val store = PayslipsStore(repo)
        store.load()
        assertEquals(listOf("June 2026", "May 2026"), store.state.value.periods)
        store.selectPeriod("June 2026")
        assertEquals(listOf(1, 3), store.state.value.filtered.map { it.id })
    }

    @Test
    fun payslipsStore_downloadTracksAndClearsError() = runTest {
        val store = PayslipsStore(FakePayslipRepo(downloadResult = Result.failure(ApiError.Network("x"))))
        val bytes = store.download(Payslip(id = 9))
        assertNull(bytes)
        assertTrue(store.state.value.downloadError != null)
        assertTrue(store.state.value.downloadingIds.isEmpty())
        store.clearDownloadError()
        assertNull(store.state.value.downloadError)
    }

    @Test
    fun payslipsStore_safeFileNameSanitizes() {
        val store = PayslipsStore(FakePayslipRepo())
        assertEquals("5-payslip.pdf", store.safeFileName(Payslip(id = 5, fileName = "payslip.pdf")))
        // Path separators are stripped so a hostile name can't escape the temp dir.
        assertEquals("9-a-b.pdf", store.safeFileName(Payslip(id = 9, fileName = "a/b.pdf")))
    }

    @Test
    fun myAssetsStore_loadPopulates() = runTest {
        val store = MyAssetsStore(FakeAssetRepo(Result.success(listOf(AssetAssignment(id = 1, assetName = "MacBook")))))
        store.load()
        assertEquals(Phase.Loaded, store.state.value.phase)
        assertEquals("MacBook", store.state.value.assets.first().assetName)
    }
}
