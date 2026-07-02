# RP HRIS — Android

Native Android client for **RP HRIS**, an employee HR self-service app: Home dashboard,
Calendar, Leave applications, WFH scheduling, Profile, payslips, assets, announcements,
support tickets, notifications, and global search.

This is a **1:1 port of the SwiftUI iOS app** (`hris-ios`) — same screens, same layout, same
behavior — rebuilt in Kotlin + Jetpack Compose with a mirrored architecture. It talks to the
same Spring backend at `https://dev-hris.geloflix.com/api/v1`.

## Toolchain

- **Kotlin 2.0.21**, **Jetpack Compose** (Compose BOM `2024.12.01`, Material 3)
- **AGP 8.7.3**, Gradle wrapper `8.11.1`, JDK 17
- **minSdk 26**, **compileSdk / targetSdk 36**
- Networking: **OkHttp 4.12** + **kotlinx.serialization**
- Images: **Coil 2.7**
- Auth storage: **EncryptedSharedPreferences** (`androidx.security-crypto`)
- Push: **Firebase Cloud Messaging** (`firebase-bom 33.7.0`)
- Tests: **JUnit4** + `kotlinx-coroutines-test` + Turbine + MockWebServer + Robolectric

## Commands

```sh
# Build the debug APK
./gradlew :app:assembleDebug

# Run the unit test suite
./gradlew :app:testDebugUnitTest

# Install on a running device/emulator
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Running without a backend (mock mode)

An in-process OkHttp `MockInterceptor` returns canned JSON in the real `ApiResponse` envelope, so
the whole app runs fully populated with no server. Toggle it via intent extras (the Android analog
of the iOS launch args):

```sh
adb shell am start -n io.rocketpartners.hris/.app.MainActivity \
  --ez mock true --ez autologin true --es tab home
```

- `--ez mock true` — install the mock session (default build hits the real backend).
- `--ez autologin true` — skip the login screen with a pre-authenticated session.
- `--es tab <home|calendar|leave|me|search>` — choose the initial tab.

Deep links can be exercised the same way:

```sh
adb shell am start -a android.intent.action.VIEW -d "hris://tab/calendar"
adb shell am start -a android.intent.action.VIEW -d "hris://notification?referenceType=LEAVE&referenceId=1001"
```

## Architecture

Layered, dependency-injected, protocol-first — mirroring the iOS app. Layers:
`app` → `feature` → `core` → `model`, plus a shared `designsystem`.

- **Composition root** — `app/AppEnvironment.kt` is the single place objects are wired: one
  OkHttp client + `AuthCoordinator`, the `ApiClient`, and every repository. `MainActivity` builds
  it once and passes it down through `RootScreen` → `MainTabScaffold`.
- **Auth gating** — `RootScreen` switches on `AuthService` state (unauthenticated / authenticating /
  authenticated). On launch, `bootstrap()` hits `GET /auth/me` to restore a session.
- **Networking** — an `Endpoint` value type → `ApiClient` (`LiveApiClient`). Responses are unwrapped
  from the `ApiResponse` envelope; errors surface as `ApiError`.
- **Tokens** — `AuthCoordinator` (a `Mutex`-coalesced `TokenProviding`) does a single transparent
  refresh + retry on a 401. Tokens persist in `EncryptedSharedPreferences`.
- **Feature pattern** (Leave / WFH / Calendar / Profile / Home / Notifications / …): a `Repository`
  interface + `Live…Repository`, and a `Store` exposing a `StateFlow<UiState>` with a shared `Phase`
  state machine (`Idle / Loading / Loaded / Failed`). Stores never touch the network directly.
- **Dates** — `WireDate` handles the backend's `yyyy-MM-dd` (all-day) and ISO-8601 (datetime) shapes
  in UTC; civil-day matching avoids timezone drift.
- **Design system** — `designsystem/Theme.kt` centralizes spacing, radii, typography, the brand
  accent, and `Theme.statusColor()`. Reuse these tokens rather than hardcoding values.

## Project structure

```
app/src/main/java/io/rocketpartners/hris/
  app/            composition root, MainActivity, tab scaffold, deep links, push
  core/           networking, auth, shared UI (Phase)
  designsystem/   Theme, Avatar, FilterChip, cards, shared components
  feature/        home, calendar, leave, wfh, timeoff, profile, payslips, assets,
                  announcements, notifications, tickets, search, login, auth, common
  model/          domain models (kotlinx.serialization)
```

## Push notifications (FCM)

FCM is wired end-to-end (device-token registration, notification channel, tap → deep link).

`app/google-services.json` is **gitignored** (Firebase config is treated as a secret), so it is not
in the repo. The google-services plugin is applied only when that file is present, and Firebase
initialization is guarded — so **the app builds and runs without it**; push is simply inert. To
enable real push delivery, drop your own `app/google-services.json` from the Firebase console (a
project with an Android app for `io.rocketpartners.hris`) and rebuild.
