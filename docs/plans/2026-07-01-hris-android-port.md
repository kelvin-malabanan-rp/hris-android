# HRIS Android Port — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build `hris-android`, a Kotlin + Jetpack Compose 1:1 port of the `hris-ios` SwiftUI app, with mirrored architecture, mock mode, and unit tests.

**Architecture:** Single `:app` Gradle module, packages mirroring the iOS layers (`app`/`feature`/`core`/`model`/`designsystem`). Manual DI graph (`AppEnvironment`) mirrors the iOS composition root. Each feature = `Repository` interface + `LiveRepository` + `Store` (ViewModel exposing `StateFlow` with a `Phase` state machine) + Composables. Networking is a thin `ApiClient` over OkHttp + kotlinx.serialization mirroring the iOS `Endpoint`/`ApiResponse`/`APIError` value types. Auth uses a `Mutex`-coalesced `AuthCoordinator` + EncryptedSharedPreferences.

**Tech Stack:** Kotlin 2.x, Jetpack Compose (Material3, BOM), navigation-compose, OkHttp, kotlinx.serialization, Coil, androidx.security-crypto, coroutines/Flow, JUnit4 + kotlinx-coroutines-test + Turbine + MockWebServer. Gradle Kotlin DSL + version catalog. minSdk 26 / target 35. applicationId `io.rocketpartners.hris`.

**Reference:** iOS source at `../hris-ios/Sources`. Each task cites the iOS file(s) it ports. Read the iOS file before porting.

**Conventions (per repo ground rules):** explicit imports only (no wildcards), add unit tests for new logic, remove unused code, report changed/added/removed per step, commit frequently, never push to main.

---

## Phase 1 — Scaffold & Foundation (buildable app that logs in)

### Task 1.1: Gradle project skeleton

**Files (create):**
- `settings.gradle.kts`, `build.gradle.kts` (root), `gradle/libs.versions.toml`
- `app/build.gradle.kts`, `app/proguard-rules.pro`
- `gradle/wrapper/gradle-wrapper.properties`, `gradlew`, `gradlew.bat`, `gradle-wrapper.jar`
- `app/src/main/AndroidManifest.xml`
- `.gitignore` (Android: `.gradle/`, `build/`, `local.properties`, `.idea/`, `*.iml`)

**Steps:**
1. Version catalog: AGP, Kotlin, Compose BOM, Material3, navigation-compose, okhttp, kotlinx-serialization(+json), coil-compose, androidx-security-crypto, lifecycle-viewmodel-compose, coroutines, junit4, coroutines-test, turbine, mockwebserver.
2. `app/build.gradle.kts`: applicationId `io.rocketpartners.hris`, minSdk 26, compile/target 35; enable Compose, kotlinx-serialization plugin; `buildConfigField` for `BASE_URL`.
3. Manifest: single `MainActivity` (exported, LAUNCHER), INTERNET permission, `android:name=".app.HrisApp"` (Application), SplashScreen theme.
4. Generate wrapper: `gradle wrapper --gradle-version 8.9` (or copy from a known-good template if `gradle` unavailable).
5. **Verify:** `./gradlew help` succeeds.
6. **Commit:** `chore: gradle project skeleton`

### Task 1.2: Models (ports `Sources/Models/*` + `Sources/Models/User`)

**Files (create):** `app/src/main/java/io/rocketpartners/hris/model/*.kt` — `User`, `AuthTokens`, `Announcement`, `AppNotification`, `AssetAssignment`, `CalendarEvent`, `LeaveApplication`, `LeaveBalance`, `LeaveType`, `Payslip`, `Ticket`, `UserOnLeave`, `UserProfile`, `WfhSchedule`, `WfhWeeklyUsage`.

**Steps per model:** read the iOS struct → write `@Serializable data class` with matching fields; replicate custom decoding (e.g. `User` composes `name` from `firstName`/`lastName`/email; `permissions` defaults empty) via `@SerialName` + a companion factory or custom serializer. Port computed props (`canApproveWfh`, `canApproveLeave`) as Kotlin `val`.
**Test:** `model/UserSerializationTest.kt` — decode a `firstName`/`lastName` payload, assert composed `name`; decode with `permissions` absent → empty; assert `canApproveWfh`/`canApproveLeave`. One serialization test per model with non-trivial decoding.
**Commit:** `feat: port domain models`

### Task 1.3: Networking core (ports `Sources/Core/Networking/*`)

**Files (create):** `core/networking/` — `Endpoint.kt`, `ApiResponse.kt`, `ApiError.kt`, `ApiClient.kt` (interface) + `LiveApiClient.kt`, `Paged.kt`, `WireDate.kt`, `Json.kt` (kotlinx `Json` config), `MultipartFile.kt`.

**Details:**
- `Endpoint`: data class `path/method/query/body/requiresAuth`, builds an OkHttp `Request` against `baseUrl` (mirror `urlRequest`). `Method` enum GET/POST/PUT/PATCH/DELETE.
- `ApiResponse<T>`: accepts both `success` boolean and `status:"success"` string → unified `success` (mirror the iOS envelope).
- `ApiError`: sealed class `InvalidUrl/Network/Decoding/Unauthorized/Server(message,status)/Unknown`, each with `userMessage`.
- `ApiClient` interface: `suspend send<T>`, `sendVoid`, `sendData`, `sendMultipart<T>` (mirror the protocol).
- `LiveApiClient`: OkHttp call wrapped in `suspendCancellableCoroutine`/`await`; bearer auth via `TokenProviding`; single transparent refresh+retry on 401; envelope decode via kotlinx.serialization; multipart body builder mirroring `multipartBody`.
- `WireDate`: UTC `yyyy-MM-dd` + ISO-8601 formatters (`java.time`), civil-day matching helpers.

**Test:** `core/networking/LiveApiClientTest.kt` against **MockWebServer** — success envelope decodes `data`; `status:"success"` and boolean `success` both accepted; 401 triggers one refresh+retry then `Unauthorized`; 5xx → `Server` with message; `sendVoid` on `data:null`. `WireDateTest.kt` for round-trips + civil-day matching.
**Commit:** `feat: port networking core`

### Task 1.4: Auth core (ports `Sources/Core/Auth/*`)

**Files (create):** `core/auth/` — `TokenProviding.kt`, `AuthTokens.kt` (or reuse model), `TokenStore.kt` (interface) + `EncryptedTokenStore.kt` (EncryptedSharedPreferences), `AuthCoordinator.kt`.

**Details:** `AuthCoordinator` implements `TokenProviding`; `Mutex` coalesces concurrent `refresh()` into one network call (mirror the actor's coalescing); persists tokens via `TokenStore`; clears on refresh failure.
**Test:** `core/auth/AuthCoordinatorTest.kt` — concurrent `refresh()` calls collapse to a single network hit (fake refresh counter); tokens persisted; failure clears tokens. `EncryptedTokenStoreTest.kt` (instrumented or Robolectric) round-trip.
**Commit:** `feat: port auth core (coalesced refresh + encrypted token store)`

### Task 1.5: Auth feature + AuthService (ports `Sources/Features/Auth`, `Sources/Features/Login`, iOS `AuthService`)

**Files (create):** `feature/auth/` — `AuthRepository.kt` + `LiveAuthRepository.kt` (`login`, `me`/bootstrap, `refresh`, `logout`), `AuthService.kt` (ViewModel/holder with `AuthState = Unauthenticated/Authenticating/Authenticated(User)`). `feature/login/LoginScreen.kt` + `LoginStore.kt`.
**Test:** `feature/auth/AuthServiceTest.kt` with `FakeAuthRepository` — bootstrap success → Authenticated; bootstrap 401 → Unauthenticated; login success/failure transitions. `feature/login/LoginStoreTest.kt`.
**Commit:** `feat: port auth service + login`

### Task 1.6: DesignSystem foundation (ports `Sources/DesignSystem/*`)

**Files (create):** `designsystem/` — `Theme.kt` (spacing, radii, typography, brand accent, `statusColor(String)`), `ColorHex.kt`, and core components: `AppBackground`, `DSCard`, `ContentCard`, `GlassCard`/`GlassSurface` (Material3 translucent approximation), `Avatar`/`Initials`, `EmptyStateView`, `ErrorStateView`, `StatusBadge`, `StatTile`, `LeaveBalanceTile`, `ProgressRing`(+`ProgressMath`), `Skeleton`, `Toast`, `FAB`, `HtmlText` (HtmlCompat→AnnotatedString), `Greeting`, `Haptics`, `LeaveDateText`.
**Test:** `designsystem/ThemeTest.kt` — `statusColor` maps known backend statuses; `ProgressMathTest.kt` — ring fraction math. (Composable visuals validated by build + manual run.)
**Commit:** `feat: port design system foundation`

### Task 1.7: Mock mode (ports `Sources/Testing/MockURLProtocol.swift`)

**Files (create):** `core/networking/mock/MockInterceptor.kt` (OkHttp `Interceptor` returning canned JSON in the real envelope), `MockFixtures.kt`. Toggle via `BuildConfig.MOCK` or intent extras (`-mock`, `-autologin`, `-tab`) read in `MainActivity`.
**Test:** `MockInterceptorTest.kt` — a mocked endpoint returns the expected fixture through `LiveApiClient`.
**Commit:** `feat: port mock mode`

### Task 1.8: App shell (ports `App/AppEnvironment`, `RootView`, `MainTabView`, `HRISApp`, `AppDelegate`, `DeepLinkRouter` stub)

**Files (create):** `app/` — `HrisApp.kt` (Application), `MainActivity.kt`, `AppEnvironment.kt` (manual DI graph wiring `LiveApiClient` + `AuthCoordinator` + all repositories), `RootScreen.kt` (auth gating on `AuthState`), `MainTabScaffold.kt` (NavigationBar tabs: Home, Calendar, Time Off, Me, Search) with a `reloadToken` mechanism, `BackendConfig.kt`, `ChromeVisibility.kt`.
**Test:** `app/AppEnvironmentTest.kt` — graph builds and exposes non-null repositories; `RootScreen` shows Login when unauthenticated (Robolectric or state-level test).
**Verify:** app builds, launches with `-mock -autologin`, shows tab scaffold; without autologin shows Login.
**Commit:** `feat: app shell — environment, root gating, tab scaffold`

---

## Phase 2 — Home + Calendar

### Task 2.1: Home (ports `Features/Home` — 2 files, uses Announcements/Out-Today/Leave/WFH summaries)
Repository + `HomeStore` + `HomeScreen` (greeting, announcements strip, out-today, leave balance tiles, WFH summary, "View all" links). Tests: `HomeStoreTest` with fakes (loading→loaded, failure). Commit.

### Task 2.2: Calendar (ports `Features/Calendar` — 11 files)
`CalendarRepository`/`LiveCalendarRepository`, `CalendarStore`, month grid + event list Composables, `CalendarEvent` mapping, civil-day matching via `WireDate`. Tests: store load, day selection, event grouping. Commit per logical chunk.

---

## Phase 3 — Time Off (Leave + WFH + unified Schedule)

### Task 3.1: Leave (ports `Features/Leave` — 8 files)
`LeaveRepository` (`myLeaves`, `balances`, `types`, `apply`, `cancel`), `LeaveStore`, list/detail/apply Composables. Tests mirror `LeaveStoreTests` (`test_loadPopulatesLeavesAndBalances`, etc.). Commit.

### Task 3.2: WFH (ports `Features/WFH` — 6 files)
`WfhRepository` (schedules, weekly usage, apply/cancel), `WfhStore`, uniform WFH rows. Tests. Commit.

### Task 3.3: Unified Schedule (ports `Features/TimeOff` — 4 files)
Combined Leave+WFH screen with reordered sections + floating `+` FAB. Tests for the combined store. Commit.

---

## Phase 4 — Me / Profile + Payslips + Assets

### Task 4.1: Profile (ports `Features/Profile` — 5 files)
`ProfileRepository`, `ProfileStore`, `UserProfile` detail screen, avatar via Coil. Tests. Commit.

### Task 4.2: Payslips (ports `Features/Payslips` — 4 files)
`PayslipRepository` (list + PDF download via `sendData`), `PayslipStore`, list + PDF open. Tests. Commit.

### Task 4.3: Assets (ports `Features/Assets` — 3 files)
`AssetRepository`, `AssetStore`, assignment list. Tests. Commit.

---

## Phase 5 — Announcements + Notifications + Approvals + Tickets + Search

### Task 5.1: Announcements (`Features/Announcements` — 5 files) — repo/store/list+detail with `HtmlText`. Tests. Commit.
### Task 5.2: Notifications (`Features/Notifications` — 4 files) — repo/store/list, read/unread. Tests. Commit.
### Task 5.3: Approvals (`Features/Approvals` — 1 file) — approve/reject actions gated by permissions. Tests. Commit.
### Task 5.4: Tickets (`Features/Tickets` — 6 files) — repo (list/create/comment, multipart attachment via `sendMultipart`), store, screens. Tests. Commit.
### Task 5.5: Search (`Features/Search` — 2 files) — global search tab, debounced query, results. Tests. Commit.

---

## Phase 6 — Push, deep links, polish, test sweep

### Task 6.1: FCM (ports `App/PushService`, `AppDelegate` push) — `FcmService`, token registration endpoint, notification channels; placeholder `google-services.json` + TODO. Tests for token-registration repo call.
### Task 6.2: Deep links (ports `App/DeepLinkRouter`) — map notification/URL routes to tabs/detail; nav-compose deep links. Tests for route parsing.
### Task 6.3: AppIntents — documented stub (no Android equivalent) in `docs/`.
### Task 6.4: Polish + full test sweep — run `./gradlew testDebugUnitTest`, fix gaps, verify mock-mode run of every tab. Final commit.

---

## Execution notes
- After each task: `./gradlew :app:assembleDebug` (or `testDebugUnitTest`) must pass before commit.
- Report changed/added/removed files each step.
- Keep the iOS file open as the spec; when Compose can't match iOS exactly (glass, tab minimize), approximate and note it.
