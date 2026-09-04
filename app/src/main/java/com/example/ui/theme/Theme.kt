package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
  primary = TerracottaPrimary,
  onPrimary = Color.White,
  primaryContainer = TerracottaContainer,
  onPrimaryContainer = OnTerracottaContainer,
  secondary = DeepIndigo,
  onSecondary = Color.White,
  secondaryContainer = IndigoContainer,
  onSecondaryContainer = OnIndigoContainer,
  tertiary = TurmericGold,
  onTertiary = Color(0xFF2E1C02),
  tertiaryContainer = TurmericContainer,
  onTertiaryContainer = OnTurmericContainer,
  background = NaturalLinen,
  onBackground = CharcoalText,
  surface = LinenCard,
  onSurface = CharcoalText,
  surfaceVariant = LinenSurfaceVariant,
  onSurfaceVariant = CharcoalMuted,
  outline = CraftBorder,
  outlineVariant = Color(0xFFDDD3C4)
)

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFFE88A58),
  onPrimary = Color(0xFF4E1D06),
  primaryContainer = Color(0xFF732E0E),
  onPrimaryContainer = TerracottaContainer,
  secondary = Color(0xFF8BA5D4),
  onSecondary = Color(0xFF10203D),
  secondaryContainer = Color(0xFF1F3155),
  onSecondaryContainer = IndigoContainer,
  tertiary = Color(0xFFF3BF6B),
  onTertiary = Color(0xFF3B2302),
  tertiaryContainer = Color(0xFF5B3908),
  onTertiaryContainer = TurmericContainer,
  background = Color(0xFF1F1C18),
  onBackground = Color(0xFFEDE6DC),
  surface = Color(0xFF28241F),
  onSurface = Color(0xFFEDE6DC),
  surfaceVariant = Color(0xFF38332C),
  onSurfaceVariant = Color(0xFFC7BFAF),
  outline = Color(0xFF5A5247)
)

@Composable
fun AIKalakarTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Keep craft branding authentic; disable dynamic tint override
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  AIKalakarTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

