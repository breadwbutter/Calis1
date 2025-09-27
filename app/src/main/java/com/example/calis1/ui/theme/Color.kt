package com.example.calis1.ui.theme

import androidx.compose.ui.graphics.Color

// ===== COLORES ORIGINALES (mantener por compatibilidad) =====
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ===== PALETA TEMÁTICA BEER BATTLE =====

// Colores principales (cerveza y oro)
val BeerGold = Color(0xFFFFB74D)       // Dorado cerveza
val BeerAmber = Color(0xFFFF8F00)      // Ámbar cerveza
val BeerDeep = Color(0xFFE65100)       // Cerveza oscura

// Colores militares (tanque y guerra)
val MilitaryGreen = Color(0xFF4CAF50)   // Verde militar principal
val MilitaryDark = Color(0xFF2E7D32)    // Verde militar oscuro
val MilitaryLight = Color(0xFF81C784)   // Verde militar claro

// Colores de acción (batalla)
val BattleRed = Color(0xFFD32F2F)       // Rojo batalla
val BattleOrange = Color(0xFFFF5722)    // Naranja explosión
val BattleBlue = Color(0xFF1976D2)      // Azul acero

// Colores neutros temáticos
val SteelGray = Color(0xFF607D8B)       // Gris acero
val SmokeGray = Color(0xFF90A4AE)       // Gris humo
val MetalDark = Color(0xFF37474F)       // Metal oscuro

// ===== ESQUEMA DE COLORES LIGHT MODE =====
val BeerBattleLightPrimary = BeerGold
val BeerBattleLightSecondary = MilitaryGreen
val BeerBattleLightTertiary = BattleBlue

// ===== ESQUEMA DE COLORES DARK MODE =====
val BeerBattleDarkPrimary = BeerAmber
val BeerBattleDarkSecondary = MilitaryDark
val BeerBattleDarkTertiary = BattleOrange

/**
 * Para usar estos colores, actualiza Theme.kt:
 *
 * private val LightColorScheme = lightColorScheme(
 *     primary = BeerBattleLightPrimary,
 *     secondary = BeerBattleLightSecondary,
 *     tertiary = BeerBattleLightTertiary,
 *     // ... otros colores
 * )
 *
 * private val DarkColorScheme = darkColorScheme(
 *     primary = BeerBattleDarkPrimary,
 *     secondary = BeerBattleDarkSecondary,
 *     tertiary = BeerBattleDarkTertiary,
 *     // ... otros colores
 * )
 */