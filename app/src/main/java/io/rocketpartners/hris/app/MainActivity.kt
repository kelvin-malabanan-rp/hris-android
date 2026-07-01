package io.rocketpartners.hris.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.rocketpartners.hris.BuildConfig
import io.rocketpartners.hris.designsystem.HrisTheme

/**
 * Single-activity host. Reads launch flags from intent extras (the Android analog of the iOS launch
 * args): `--ez mock true`, `--ez autologin true`, `--es tab home|calendar|leave|me|search`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val mock = intent.getBooleanExtra("mock", BuildConfig.MOCK_DEFAULT)
        val autoLogin = intent.getBooleanExtra("autologin", false)
        val initialTab = HrisTab.fromKey(intent.getStringExtra("tab"))
        val environment = AppEnvironment(applicationContext, mock = mock, autoLogin = autoLogin)

        setContent {
            HrisTheme {
                RootScreen(environment = environment, initialTab = initialTab)
            }
        }
    }
}
