# HRIS Android — 1:1 Kotlin/Compose Port Design

**Date:** 2026-07-01
**Source:** `../hris-ios` (SwiftUI, ~11.3k LOC, 130 files, ~20 features)
**Target:** `hris-android` — Kotlin + Jetpack Compose, full 1:1 port, architecture mirrored.

## Decisions

- **Location:** `HRIS/hris-android` (sibling of `hris-ios`).
- **Scope:** Full 1:1 port of every feature, done in buildable phases.
- **Stack:** Jetpack Compose + Material3; layered DI/repository/Store structure mapping 1:1 to the iOS layers.
- **applicationId:** `io.rocketpartners.hris`; **minSdk 26**, compile/target SDK 35.
- **Build:** Gradle Kotlin DSL + version catalog (`libs.versions.toml`). Single `:app` module.
- **Backend:** same — `https://dev-hris.geloflix.com/api/v1`.
- **Mock mode + tests:** both replicated.
- **AppIntents/Siri:** deferred/stubbed (Android has no direct equivalent).
- **Push:** FCM (replaces APNs), wired with a placeholder `google-services.json` + TODO.

## Layer / package mapping (`App → Features → Core → Models → DesignSystem`)

| iOS | Android package |
|---|---|
| `App/AppEnvironment` (composition root) | `app/AppEnvironment` — manual DI graph (no Hilt) |
| `App/RootView` (auth gating) | `app/RootScreen` — switches on `AuthState` |
| `App/MainTabView` | `app/MainTabScaffold` — Material3 `NavigationBar` + `navigation-compose` |
| `App/DeepLinkRouter`, `PushService`, `AppDelegate` | `app/*` (FCM) |
| `Core/Networking` | `core/networking` — `Endpoint` data class + `ApiClient` interface + `LiveApiClient` over OkHttp + kotlinx.serialization |
| `Core/Auth` | `core/auth` — `AuthCoordinator` class w/ `Mutex`; tokens in EncryptedSharedPreferences |
| `Models/*` | `model/*` — `@Serializable data class` |
| `DesignSystem/*` | `designsystem/*` — `Theme` object + Compose components |
| `Features/<X>` | `feature/<x>` — `Repository` + `LiveRepository` + `Store` (ViewModel) + Composables |

## Concept mapping (SwiftUI → Compose)

| iOS | Android |
|---|---|
| `@MainActor @Observable Store` + `Phase` | `ViewModel` + `StateFlow<UiState>`; `Phase` sealed interface `idle/loading/loaded/failed` |
| `async/await`, `reloadToken: Int` | coroutines `suspend`/`Flow`; reload token as `StateFlow<Int>` |
| `Codable` | `kotlinx.serialization` |
| `WireDate` (UTC `yyyy-MM-dd` / ISO-8601) | `java.time` + fixed UTC formatters |
| `AuthedAsyncImage` | Coil `AsyncImage` + auth-header interceptor |
| SF Symbols | Material Icons + ported vector assets |
| Haptics | `HapticFeedback` / `Vibrator` |
| `HTMLText` | `HtmlCompat.fromHtml` → `AnnotatedString` |
| Keychain | EncryptedSharedPreferences |
| Launch screen | Android 12 SplashScreen API |
| Mock (`MockURLProtocol` + launch args) | OkHttp interceptor + canned JSON, toggled by `BuildConfig`/intent extras (`-mock`, `-autologin`, `-tab`) |

## Won't map cleanly (flagged, not silently dropped)

- **AppIntents/Siri** → stubbed/deferred.
- **iOS 26 liquid glass** (`GlassCard`, `GlassSurface`, `tabBarMinimizeBehavior`) → approximated with Material3 translucent surfaces.
- **Push** APNs → FCM; placeholder config + TODO for real Firebase credentials.

## Testing

JUnit4 + `kotlinx-coroutines-test` + Turbine for `StateFlow`. `Fake<X>Repository` mirroring `Tests/Support/Fakes.swift` (result-typed stubs + call recording). ViewModel tests assert `Phase` + emitted state. `ApiClient` tested against MockWebServer. New code gets tests as it is written.

## Phases (each buildable)

1. **Scaffold** — Gradle/catalog, Theme + core DesignSystem, Models, networking, auth, `AppEnvironment`, mock interceptor, Root + Tab scaffold, **Login**.
2. **Home + Calendar**
3. **Time Off** (Leave + WFH + unified Schedule)
4. **Me/Profile + Payslips + Assets**
5. **Announcements + Notifications + Approvals + Tickets + Search**
6. **FCM push + deep links + polish + test sweep**
