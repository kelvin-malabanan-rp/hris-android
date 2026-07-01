package io.rocketpartners.hris.app

import android.content.Context
import io.rocketpartners.hris.core.auth.AuthCoordinator
import io.rocketpartners.hris.core.auth.EncryptedTokenStore
import io.rocketpartners.hris.core.auth.InMemoryTokenStore
import io.rocketpartners.hris.core.auth.TokenStore
import io.rocketpartners.hris.core.networking.ApiClient
import io.rocketpartners.hris.core.networking.LiveApiClient
import io.rocketpartners.hris.feature.announcements.AnnouncementRepository
import io.rocketpartners.hris.feature.announcements.LiveAnnouncementRepository
import io.rocketpartners.hris.feature.auth.AuthService
import io.rocketpartners.hris.feature.auth.LiveAuthRepository
import io.rocketpartners.hris.feature.calendar.CalendarRepository
import io.rocketpartners.hris.feature.calendar.LiveCalendarRepository
import io.rocketpartners.hris.feature.leave.LeaveRepository
import io.rocketpartners.hris.feature.leave.LiveLeaveRepository
import io.rocketpartners.hris.feature.profile.LiveProfileRepository
import io.rocketpartners.hris.feature.profile.ProfileRepository
import io.rocketpartners.hris.feature.wfh.LiveWfhRepository
import io.rocketpartners.hris.feature.wfh.WfhRepository

/**
 * The single composition root — the one place objects are wired, mirroring iOS `AppEnvironment`.
 * Builds one OkHttp client + [AuthCoordinator] and constructs the API client, token store, and
 * services from them. Feature repositories are added here as each feature is ported.
 */
class AppEnvironment(
    context: Context,
    val mock: Boolean,
    autoLogin: Boolean,
) {
    private val httpClient = BackendConfig.makeClient(mock = mock, autoLogin = autoLogin)

    // Mock mode uses a non-persistent store so demo sessions start fresh; real builds use the
    // hardware-encrypted keychain equivalent.
    private val tokenStore: TokenStore =
        if (mock) InMemoryTokenStore() else EncryptedTokenStore(context)

    private val coordinator = AuthCoordinator(BackendConfig.baseUrl, httpClient, tokenStore)

    val apiClient: ApiClient = LiveApiClient(BackendConfig.baseUrl, httpClient, coordinator)

    val authService: AuthService = AuthService(LiveAuthRepository(apiClient, tokenStore))

    val calendarRepository: CalendarRepository = LiveCalendarRepository(apiClient)
    val leaveRepository: LeaveRepository = LiveLeaveRepository(apiClient)
    val wfhRepository: WfhRepository = LiveWfhRepository(apiClient)
    val profileRepository: ProfileRepository = LiveProfileRepository(apiClient)
    val announcementRepository: AnnouncementRepository = LiveAnnouncementRepository(apiClient)
}
