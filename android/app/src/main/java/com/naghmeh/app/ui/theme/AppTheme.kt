package com.naghmeh.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.naghmeh.app.R

// Day Theme: Warm cream/ivory paper with deep Persian teal/emerald accents
val DayBackground = Color(0xFFF8F6F1)
val DaySurface = Color(0xFFFFFFFF)
val DaySurfaceVariant = Color(0xFFEFECE4)
val DayInk = Color(0xFF1B1E24)
val DayMutedInk = Color(0xFF525866)
val DayPrimary = Color(0xFF0E6B64)
val DayPrimaryContainer = Color(0xFFD6ECE9)
val DayOnPrimaryContainer = Color(0xFF063A36)
val DaySecondary = Color(0xFFA86416)
val DaySecondaryContainer = Color(0xFFF6E7D3)
val DayOnSecondaryContainer = Color(0xFF4A2B05)
val DayOutline = Color(0xFFDDD6C8)
val DayError = Color(0xFFB3261E)

// Night Theme: Midnight slate/navy with warm, eye-friendly evening rehearsal accents
val NightBackground = Color(0xFF0E141B)
val NightSurface = Color(0xFF161F2A)
val NightSurfaceVariant = Color(0xFF1F2B38)
val NightInk = Color(0xFFF1EDE4)
val NightMutedInk = Color(0xFF9FB0C2)
val NightPrimary = Color(0xFF3CB8AC)
val NightOnPrimary = Color(0xFF072623)
val NightPrimaryContainer = Color(0xFF163D3A)
val NightOnPrimaryContainer = Color(0xFFBFECE6)
val NightSecondary = Color(0xFFE5A952)
val NightOnSecondary = Color(0xFF2D1B04)
val NightSecondaryContainer = Color(0xFF3B2B15)
val NightOnSecondaryContainer = Color(0xFFF8DFB8)
val NightOutline = Color(0xFF2A394A)
val NightError = Color(0xFFF28B82)

enum class PersianFontOption(
    val storageKey: String,
    val titleFa: String,
    val shortLabelFa: String,
    val lineHeightMultiplier: Float
) {
    VAZIRMATN(
        storageKey = "vazirmatn",
        titleFa = "فونت وزیر (مدرن و خوانا)",
        shortLabelFa = "فونت وزیر",
        lineHeightMultiplier = 1.9f
    ),
    NASTALIQ(
        storageKey = "nastaliq",
        titleFa = "فونت نستعلیق (مناسب شعر سنتی و آواز)",
        shortLabelFa = "فونت نستعلیق",
        lineHeightMultiplier = 2.45f
    ),
    SAHEL(
        storageKey = "sahel",
        titleFa = "فونت ساحل (نرم و خواندنی)",
        shortLabelFa = "فونت ساحل",
        lineHeightMultiplier = 1.95f
    );

    companion object {
        fun fromKey(key: String?): PersianFontOption {
            return entries.firstOrNull { it.storageKey == key } ?: VAZIRMATN
        }
    }
}

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_bold, FontWeight.Bold)
)

val NastaliqFontFamily = FontFamily(
    Font(R.font.iran_nastaliq, FontWeight.Normal)
)

val SahelFontFamily = FontFamily(
    Font(R.font.sahel_regular, FontWeight.Normal),
    Font(R.font.sahel_bold, FontWeight.Bold)
)

fun fontFamilyFor(option: PersianFontOption): FontFamily = when (option) {
    PersianFontOption.VAZIRMATN -> VazirmatnFontFamily
    PersianFontOption.NASTALIQ -> NastaliqFontFamily
    PersianFontOption.SAHEL -> SahelFontFamily
}

private val NaghmehDayColorScheme = lightColorScheme(
    primary = DayPrimary,
    onPrimary = Color.White,
    primaryContainer = DayPrimaryContainer,
    onPrimaryContainer = DayOnPrimaryContainer,
    secondary = DaySecondary,
    onSecondary = Color.White,
    secondaryContainer = DaySecondaryContainer,
    onSecondaryContainer = DayOnSecondaryContainer,
    background = DayBackground,
    onBackground = DayInk,
    surface = DaySurface,
    onSurface = DayInk,
    surfaceVariant = DaySurfaceVariant,
    onSurfaceVariant = DayMutedInk,
    outline = DayOutline,
    error = DayError,
    onError = Color.White
)

private val NaghmehNightColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = NightPrimaryContainer,
    onPrimaryContainer = NightOnPrimaryContainer,
    secondary = NightSecondary,
    onSecondary = NightOnSecondary,
    secondaryContainer = NightSecondaryContainer,
    onSecondaryContainer = NightOnSecondaryContainer,
    background = NightBackground,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightMutedInk,
    outline = NightOutline,
    error = NightError,
    onError = Color(0xFF3E0A06)
)

fun buildNaghmehTypography(fontFamily: FontFamily): Typography = Typography(
    headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 35.sp
    ),
    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 31.sp
    ),
    titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 28.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp,
        lineHeight = 32.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )
)

@Composable
fun NaghmehTheme(
    darkTheme: Boolean = false,
    fontOption: PersianFontOption = PersianFontOption.VAZIRMATN,
    content: @Composable () -> Unit
) {
    val activeColorScheme = if (darkTheme) NaghmehNightColorScheme else NaghmehDayColorScheme
    val activeFontFamily = fontFamilyFor(fontOption)

    MaterialTheme(
        colorScheme = activeColorScheme,
        typography = buildNaghmehTypography(activeFontFamily),
        content = content
    )
}
