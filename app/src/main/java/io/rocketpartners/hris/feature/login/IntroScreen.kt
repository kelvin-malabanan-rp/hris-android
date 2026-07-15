package io.rocketpartners.hris.feature.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rocketpartners.hris.R
import io.rocketpartners.hris.designsystem.Poppins
import io.rocketpartners.hris.designsystem.Theme

private data class IntroFeature(val number: String, val name: String, val detail: String)

private val introFeatures = listOf(
    IntroFeature("01", "Calendar", "the team's week"),
    IntroFeature("02", "Leaves", "request & track"),
    IntroFeature("03", "Remote", "log WFH days"),
)

/**
 * Root pre-auth screen: "Work, organized." headline, a numbered three-row feature list,
 * and the "Get started" CTA that pushes the sign-in screen. Mirrors iOS `IntroView`.
 */
@Composable
fun IntroScreen(onGetStarted: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = LoginMetrics.hInset)
            .padding(top = Theme.Spacing.sm, bottom = 18.dp),
    ) {
        IntroHeader()

        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(LoginMetrics.blockGap)) {
            IntroHeadline()
            IntroFeatureList()
        }

        Spacer(Modifier.weight(1f))

        InkCapsuleButton(title = "Get started", onClick = onGetStarted)
    }
}

@Composable
private fun IntroHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Theme.Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painterResource(R.drawable.hris_logo),
            contentDescription = null,
            modifier = Modifier.size(26.dp),
        )
        Text(
            "HRIS",
            fontFamily = Poppins,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = LoginPalette.ink,
        )
    }
}

@Composable
private fun IntroHeadline() {
    // The mock's line-height is 1.06 (48.8 at 46sp type) — much tighter than Poppins' default,
    // so the style pins lineHeight and trims the font's built-in padding.
    Text(
        "Work,\norganized.",
        style = TextStyle(
            fontFamily = Poppins,
            fontSize = 46.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1.2).sp,
            lineHeight = 49.sp,
            lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        ),
        color = LoginPalette.ink,
    )
}

@Composable
private fun IntroFeatureList() {
    val hairline = LoginPalette.hairline
    Column {
        for (feature in introFeatures) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = LoginMetrics.rowVPad)
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    feature.number,
                    fontFamily = Poppins,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LoginPalette.listNumber,
                )
                Text(
                    feature.name,
                    style = LoginValueTextStyle,
                    fontWeight = FontWeight.SemiBold,
                    color = LoginPalette.ink,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    feature.detail,
                    fontFamily = Poppins,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HairlineRule(hairline)
        }
    }
}

@Composable
private fun HairlineRule(color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}
