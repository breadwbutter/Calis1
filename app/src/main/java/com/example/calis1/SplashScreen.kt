package com.example.calis1

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calis1.ui.theme.Calis1Theme
import kotlinx.coroutines.delay


@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Estados de animación
    var startAnimation by remember { mutableStateOf(false) }

    // Configuración de animaciones
    val logoScale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = EaseOutQuart
        ),
        label = "logo_alpha"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            delayMillis = 600, // Aparece después del logo
            easing = EaseOutQuart
        ),
        label = "text_alpha"
    )

    // Efecto para iniciar animación y controlar duración
    LaunchedEffect(Unit) {
        startAnimation = true
        delay(3000) // Duración total del splash: 3 segundos
        onSplashFinished()
    }

    // Fondo con gradiente temático (colores de cerveza y batalla)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E), // Azul profundo
                        Color(0xFF3F51B5), // Azul medio
                        Color(0xFF5C6BC0), // Azul claro
                        Color(0xFFFFB74D)  // Dorado (color cerveza)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo animado (tanque con cerveza)
            Image(
                painter = painterResource(id = R.drawable.beer_battle_logo), // Reemplaza con tu imagen
                contentDescription = "BeerBattle Logo",
                modifier = Modifier
                    .size(200.dp)
                    .scale(logoScale)
                    .alpha(logoAlpha),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Título animado
            Text(
                text = "BeerBattle",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtítulo opcional
            Text(
                text = "¡Que comience la batalla!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha)
            )
        }

        // Indicador de carga en la parte inferior (opcional)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = "Cargando...",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.alpha(textAlpha)
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    Calis1Theme {
        SplashScreen(onSplashFinished = {})
    }
}