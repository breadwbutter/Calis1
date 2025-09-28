package com.example.calis1.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.calis1.repository.SettingsRepository
import com.example.calis1.viewmodel.AppConfiguraciones
import com.example.calis1.viewmodel.TipoTema

// Esquema de colores oscuro para BeerBattle
private val BeerBattleDarkColorScheme = darkColorScheme(
    primary = BeerBattleDarkPrimary,           // Ámbar cerveza
    onPrimary = Color.Black,
    primaryContainer = MilitaryDark,           // Verde militar oscuro
    onPrimaryContainer = Color.White,

    secondary = BeerBattleDarkSecondary,       // Verde militar oscuro
    onSecondary = Color.White,
    secondaryContainer = SteelGray,            // Gris acero
    onSecondaryContainer = Color.White,

    tertiary = BeerBattleDarkTertiary,         // Naranja batalla
    onTertiary = Color.White,
    tertiaryContainer = BattleRed,             // Rojo batalla
    onTertiaryContainer = Color.White,

    error = BattleRed,                         // Rojo batalla para errores
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color.White,

    background = MetalDark,                    // Metal oscuro
    onBackground = Color.White,

    surface = Color(0xFF1A1C1E),              // Superficie oscura
    onSurface = Color.White,
    surfaceVariant = SteelGray,               // Gris acero para variantes
    onSurfaceVariant = Color.White,

    outline = SmokeGray,                      // Gris humo para bordes
    outlineVariant = Color(0xFF424242)
)

// Esquema de colores claro para BeerBattle
private val BeerBattleLightColorScheme = lightColorScheme(
    primary = BeerBattleLightPrimary,         // Dorado cerveza
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFFFF3C4),    // Contenedor dorado claro
    onPrimaryContainer = Color.Black,

    secondary = BeerBattleLightSecondary,     // Verde militar
    onSecondary = Color.White,
    secondaryContainer = MilitaryLight,       // Verde militar claro
    onSecondaryContainer = Color.Black,

    tertiary = BeerBattleLightTertiary,       // Azul acero
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBBDEFB),   // Azul claro para contenedor
    onTertiaryContainer = Color.Black,

    error = BattleRed,                        // Rojo batalla para errores
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color.Black,

    background = Color(0xFFFFFBFE),          // Fondo claro
    onBackground = Color.Black,

    surface = Color.White,                    // Superficie blanca
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFF3F3F3),      // Superficie variante gris claro
    onSurfaceVariant = Color.Black,

    outline = SteelGray,                     // Gris acero para bordes
    outlineVariant = SmokeGray               // Gris humo para bordes variantes
)

@Composable
fun Calis1Theme(
    // NUEVO: Parámetro para controlar el tema desde configuraciones
    settingsRepository: SettingsRepository? = null,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Cambiado a false para usar nuestros colores temáticos
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // NUEVO: Obtener configuraciones de tema si está disponible el repository
    val configuraciones by (settingsRepository?.getConfiguraciones()
        ?.collectAsState(initial = AppConfiguraciones()) // <-- CORRECCIÓN 1 AQUÍ
        ?: return@Calis1Theme MaterialTheme(content = content))

    // NUEVO: Determinar el tema basado en configuraciones
    val darkTheme = when (configuraciones.tema) {
        TipoTema.CLARO -> false
        TipoTema.OSCURO -> true
        TipoTema.SISTEMA -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> BeerBattleDarkColorScheme
        else -> BeerBattleLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * NUEVO: Versión simplificada para usar en contextos donde no necesitas configuraciones dinámicas
 */
@Composable
fun Calis1ThemeStatic(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> BeerBattleDarkColorScheme
        else -> BeerBattleLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * NUEVO: Hook para obtener el tema actual basado en configuraciones
 */
@Composable
fun rememberThemeState(settingsRepository: SettingsRepository): Boolean {
    val configuraciones by settingsRepository.getConfiguraciones()
        .collectAsState(initial = AppConfiguraciones()) // <-- CORRECCIÓN 2 AQUÍ

    return when (configuraciones.tema) {
        TipoTema.CLARO -> false
        TipoTema.OSCURO -> true
        TipoTema.SISTEMA -> isSystemInDarkTheme()
    }
}