package `in`.localdukaan.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * LocalDukaan "Premium Dukaan" design system.
 * Deep indigo brand + gold accent — rich, trustworthy, premium feel.
 */
object DukaanColors {
    // Flipkart-style commerce palette: YELLOW brand hero + deep-green actions + clean neutrals.
    // Peela rang header, buttons aur highlights me; kaala text peele par; hara sirf success/stock.
    val FlipkartYellow = Color(0xFFFFD814)     // brand primary (top bar, buttons, highlights)
    val FlipkartYellowDark = Color(0xFFF5C236) // gradient end / pressed
    val FlipkartBlue = Color(0xFF2874F0)       // links + accents (Flipkart blue)
    val AmazonDark = Color(0xFF131921)         // premium headings (Amazon dark)
    val BlinkitYellow = Color(0xFFF8CB46)   // legacy brand alias (ab FlipkartYellow jaisa)
    val BlinkitGreen = Color(0xFF0C831F)    // CTA / success
    val Green = BlinkitGreen                // legacy alias
    val GreenDark = Color(0xFF0A6B19)       // pressed CTA / gradient end
    val Dark = Color(0xFF1A1A1A)            // primary text (blinkit jaisa near-black)
    val Gray = Color(0xFF5F6368)            // secondary text
    val LightGreen = Color(0xFFE3F4E6)      // success tint
    val LightYellow = Color(0xFFFDF3D7)     // warning/info tint

    val Blue = Color(0xFF2874F0)            // links/actions ab Flipkart blue
    val Navy = Color(0xFF0A6B19)            // legacy primary ab deep green
    val LightBlue = Color(0xFFE3F4E6)       // legacy tint ab light green

    val Red = Color(0xFFD93025)             // danger
    val LightRed = Color(0xFFFCE8E6)
    val Gold = Color(0xFFF9AB00)            // warning accent
    val LightAmber = Color(0xFFFDF3D7)

    // Neutrals
    val Slate900 = Color(0xFF1A1A1A)
    val Slate800 = Color(0xFF2A2A2A)
    val Slate700 = Color(0xFF3C3C3C)
    val Slate600 = Color(0xFF5F6368)
    val Slate500 = Color(0xFF7A8699)
    val Slate400 = Color(0xFF9AA6B8)
    val Slate200 = Color(0xFFE8E8E8)
    val Slate100 = Color(0xFFF1F3F6)
    val Slate50 = Color(0xFFFAFAFA)

    // Gradients — hero + saare bade buttons peele (Flipkart style), text hamesha kaala
    val HeroGradient = Brush.verticalGradient(listOf(Color(0xFFFFD814), Color(0xFFF5C236)))
    val ButtonGradient = Brush.verticalGradient(listOf(Color(0xFFFFD814), Color(0xFFF5C236)))
    val OnYellow = Color(0xFF1A1A1A)
    val DrawerGradient = Brush.verticalGradient(listOf(Color(0xFFFFD814), Color(0xFFF5C236)))
}

private val LightColorScheme = lightColorScheme(
    primary = DukaanColors.BlinkitGreen,
    onPrimary = Color.White,
    primaryContainer = DukaanColors.LightGreen,
    onPrimaryContainer = DukaanColors.GreenDark,
    secondary = DukaanColors.BlinkitGreen,
    onSecondary = Color.White,
    secondaryContainer = DukaanColors.LightGreen,
    onSecondaryContainer = DukaanColors.GreenDark,
    tertiary = DukaanColors.Gold,
    onTertiary = Color.White,
    tertiaryContainer = DukaanColors.LightAmber,
    onTertiaryContainer = Color(0xFF6B4D00),
    background = DukaanColors.Slate50,
    onBackground = DukaanColors.Dark,
    surface = Color.White,
    onSurface = DukaanColors.Dark,
    surfaceVariant = DukaanColors.Slate100,
    onSurfaceVariant = DukaanColors.Slate700,
    error = DukaanColors.Red,
    errorContainer = DukaanColors.LightRed,
    onError = Color.White
)

@Composable
fun NewDukaanTheme(
    content: @Composable () -> Unit
) {
    // Force-light: saare surfaces hardcoded white hain; dark scheme text contrast todta hai.
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
