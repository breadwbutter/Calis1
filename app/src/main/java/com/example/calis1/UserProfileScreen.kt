package com.example.calis1

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calis1.ui.theme.Calis1Theme
import com.example.calis1.viewmodel.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    authState: AuthState,
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit = {} // NUEVO: Callback para navegar a configuraciones
) {
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }

    val userData = getUserDataFromAuthState(authState)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Avatar y información principal
        UserAvatarSection(userData)

        // Información del usuario
        UserInfoCard(userData)

        // Configuraciones y acciones - MODIFICADO
        UserActionsCard(
            onLogoutClick = { showLogoutDialog = true },
            onSettingsClick = onNavigateToSettings // NUEVO: Agregar callback
        )

        Spacer(modifier = Modifier.weight(1f))

        // Información de la app
        AppInfoSection()
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text("Cerrar sesión")
            },
            text = {
                Text("¿Estás seguro de que quieres cerrar tu sesión?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Sí, cerrar sesión")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun UserAvatarSection(userData: UserDisplayData) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = userData.avatarIcon,
                contentDescription = "Avatar",
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Text(
            text = userData.displayName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = userData.badgeColor.copy(alpha = 0.1f)
            )
        ) {
            Text(
                text = userData.accountType,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = userData.badgeColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun UserInfoCard(userData: UserDisplayData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Información de cuenta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (userData.email.isNotEmpty()) {
                InfoRow(
                    icon = Icons.Default.Email,
                    label = "Email",
                    value = userData.email
                )
            }

            InfoRow(
                icon = Icons.Default.AccountCircle,
                label = "Usuario",
                value = userData.username
            )

            InfoRow(
                icon = Icons.Default.Login,
                label = "Tipo de acceso",
                value = userData.loginMethod
            )

            if (userData.userId.isNotEmpty()) {
                InfoRow(
                    icon = Icons.Default.Fingerprint,
                    label = "ID de usuario",
                    value = userData.userId.take(8) + "..."
                )
            }
        }
    }
}

@Composable
fun UserActionsCard(
    onLogoutClick: () -> Unit,
    onSettingsClick: () -> Unit // NUEVO: Parámetro para configuraciones
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Acciones",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // NUEVO: Botón de configuraciones
            OutlinedButton(
                onClick = onSettingsClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configuraciones")
            }

            // Botón existente de configuraciones (ahora como divider visual)
            OutlinedButton(
                onClick = { /* TODO: Implementar otras configuraciones */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = false // Deshabilitado temporalmente
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Preferencias (Próximamente)")
            }

            // Espaciador visual
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // Botón de cerrar sesión
            Button(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar sesión")
            }
        }
    }
}

@Composable
fun AppInfoSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "BeerBattle v1.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "Control semanal de alcohol",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun getUserDataFromAuthState(authState: AuthState): UserDisplayData {
    return when (authState) {
        is AuthState.SignedIn -> {
            val user = authState.user
            UserDisplayData(
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Usuario Google",
                username = user.displayName ?: user.email?.substringBefore("@") ?: "user",
                email = user.email ?: "",
                userId = user.uid,
                accountType = "Cuenta Google",
                loginMethod = "Google Sign-In",
                avatarIcon = Icons.Default.AccountCircle,
                badgeColor = MaterialTheme.colorScheme.primary
            )
        }
        is AuthState.TraditionalSignedIn -> {
            UserDisplayData(
                displayName = "Administrador",
                username = authState.username,
                email = "admin@gmail.com",
                userId = "admin_001",
                accountType = "Administrador",
                loginMethod = "Credenciales tradicionales",
                avatarIcon = Icons.Default.AdminPanelSettings,
                badgeColor = MaterialTheme.colorScheme.tertiary
            )
        }
        is AuthState.EmailSignedIn -> {
            UserDisplayData(
                displayName = authState.username.replaceFirstChar { it.uppercase() },
                username = authState.username,
                email = authState.email,
                userId = authState.email.hashCode().toString(),
                accountType = "Cuenta registrada",
                loginMethod = "Email y contraseña",
                avatarIcon = Icons.Default.Person,
                badgeColor = MaterialTheme.colorScheme.secondary
            )
        }
        else -> {
            UserDisplayData()
        }
    }
}

data class UserDisplayData(
    val displayName: String = "Usuario",
    val username: String = "user",
    val email: String = "",
    val userId: String = "",
    val accountType: String = "Cuenta básica",
    val loginMethod: String = "Desconocido",
    val avatarIcon: ImageVector = Icons.Default.Person,
    val badgeColor: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Gray
)

@Preview(showBackground = true)
@Composable
fun UserProfileScreenPreview() {
    Calis1Theme {
        UserProfileScreen(
            authState = AuthState.TraditionalSignedIn("admin"),
            onLogout = {},
            onNavigateToSettings = {} // NUEVO: Agregar en preview
        )
    }
}