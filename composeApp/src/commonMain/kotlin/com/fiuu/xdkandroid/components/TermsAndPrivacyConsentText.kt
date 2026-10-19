package com.fiuu.xdkandroid.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.fiuu.xdkandroid.ui.model.TextSpan
import com.fiuu.xdkandroid.ui.theme.AppColors

enum class TermsAndPrivacyStyle {
    // Centered, 16sp, blue normal-weight links, line break after "agree to" — used by
    // SinglePaymentScreen's channel-payment card.
    Comfortable,

    // Left-aligned, 13sp, gray body text with bold green links and a trailing period — used by
    // native card-entry placeholders (CardManualEntryScreen, ClickToPayRegistrationRequiredView).
    Compact
}

// Shared "By continuing..." consent line used on every screen that links out to Fiuu's Terms of
// Service and Privacy Policy. The Terms of Service URL is country-specific — pass the raw,
// merchant-supplied country code and this resolves it to a supported slug (falling back to "my")
// rather than making every caller re-implement that; pass termsUrl directly instead when a
// screen has no country in scope (it takes priority over country when both are given).
@Composable
fun TermsAndPrivacyConsentText(
    country: String? = null,
    style: TermsAndPrivacyStyle = TermsAndPrivacyStyle.Comfortable,
    modifier: Modifier = Modifier
) {
    val privacyUrl = "https://fiuu.com/privacy-policy/"
    val resolvedTermsUrl = "https://fiuu.com/terms-of-services/${termsOfServiceCountrySlug(country)}"

    val spans = when (style) {
        TermsAndPrivacyStyle.Comfortable -> listOf(
            TextSpan(
                text = "By continuing you have read and agree to\n",
                style = SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
            ),
            TextSpan(
                text = "Terms & Conditions",
                style = SpanStyle(fontSize = 16.sp, color = AppColors.Primary, fontWeight = FontWeight.Normal),
                url = resolvedTermsUrl
            ),
            TextSpan(
                text = " & ",
                style = SpanStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
            ),
            TextSpan(
                text = "Privacy Policy",
                style = SpanStyle(fontSize = 16.sp, color = AppColors.Primary, fontWeight = FontWeight.Normal),
                url = privacyUrl
            )
        )
        TermsAndPrivacyStyle.Compact -> listOf(
            TextSpan(
                text = "By continuing you have read and agree to the ",
                style = SpanStyle(fontSize = 13.sp, color = AppColors.TextSecondary)
            ),
            TextSpan(
                text = "Terms & Conditions",
                style = SpanStyle(fontSize = 13.sp, color = AppColors.Secondary, fontWeight = FontWeight.Bold),
                url = resolvedTermsUrl
            ),
            TextSpan(
                text = " & ",
                style = SpanStyle(fontSize = 13.sp, color = AppColors.TextSecondary)
            ),
            TextSpan(
                text = "Privacy Policy",
                style = SpanStyle(fontSize = 13.sp, color = AppColors.Secondary, fontWeight = FontWeight.Bold),
                url = privacyUrl
            ),
            TextSpan(
                text = ".",
                style = SpanStyle(fontSize = 13.sp, color = AppColors.TextSecondary)
            )
        )
    }

    CustomText(
        modifier = modifier,
        textAlign = when (style) {
            TermsAndPrivacyStyle.Comfortable -> TextAlign.Center
            TermsAndPrivacyStyle.Compact -> TextAlign.Start
        },
        spans = spans
    )
}

// mp_country is merchant-supplied and unvalidated — fall back to "my" for anything that isn't
// one of Fiuu's supported markets rather than building a URL from an arbitrary/blank string.
private val supportedTermsOfServiceCountries =
    setOf("my", "sg", "th", "id", "ph", "vn", "tw", "hk")

private fun termsOfServiceCountrySlug(country: String?): String {
    val normalized = country?.trim()?.lowercase()
    return if (normalized in supportedTermsOfServiceCountries) normalized!! else "my"
}
