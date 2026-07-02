package io.rocketpartners.hris.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.rocketpartners.hris.BuildConfig
import io.rocketpartners.hris.designsystem.HrisTheme

/**
 * Single-activity host. Reads launch flags from intent extras (the Android analog of the iOS launch
 * args): `--ez mock true`, `--ez autologin true`, `--es tab home|calendar|leave|me|search`. Also
 * parses deep-link intents (push taps + `hris://` URIs) into [DeepLinkRouter] — mirrors iOS
 * `AppDelegate` forwarding notification payloads to the router.
 */
class MainActivity : ComponentActivity() {

    private lateinit var environment: AppEnvironment

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mock = intent.getBooleanExtra("mock", BuildConfig.MOCK_DEFAULT)
        val autoLogin = intent.getBooleanExtra("autologin", false)
        val initialTab = HrisTab.fromKey(intent.getStringExtra("tab"))
        environment = AppEnvironment(applicationContext, mock = mock, autoLogin = autoLogin)
        // Hand the token authority to the global Coil loader so `/uploads` images load authenticated.
        (application as? HrisApp)?.tokenProvider = environment.tokenProvider

        handleDeepLink(intent)

        setContent {
            HrisTheme {
                RootScreen(environment = environment, initialTab = initialTab)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    /** Parses a deep-link from the intent's data URI or push extras and publishes it to the router. */
    private fun handleDeepLink(intent: Intent) {
        val target = deepLinkFromUri(intent) ?: deepLinkFromExtras(intent) ?: return
        environment.deepLinkRouter.submit(target)
    }

    private fun deepLinkFromUri(intent: Intent): DeepLinkTarget? {
        val uri = intent.data ?: return null
        val query = uri.queryParameterNames.associateWith { uri.getQueryParameter(it) }
        return DeepLinkTarget.fromUri(uri.scheme, uri.host, uri.pathSegments.firstOrNull(), query)
    }

    private fun deepLinkFromExtras(intent: Intent): DeepLinkTarget? {
        val data = mapOf(
            "referenceType" to intent.getStringExtra("referenceType"),
            "referenceId" to intent.getStringExtra("referenceId"),
            "type" to intent.getStringExtra("type"),
        )
        if (data.values.all { it == null }) return null
        return DeepLinkTarget.fromData(data)
    }
}
