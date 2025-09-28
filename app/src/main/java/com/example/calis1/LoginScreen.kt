package com.example.calis1

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.calis1.ui.theme.Calis1Theme
import com.example.calis1.viewmodel.AuthViewModel
import com.example.calis1.viewmodel.AuthState
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLoginSuccess: () -> Unit = {} // Ahora es opcional, el estado se maneja automáticamente
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()

    // Estado para alternar entre login y registro
    var isLoginMode by remember { mutableStateOf(true) }

    // Variables para el formulario
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }

    // Validaciones en tiempo real
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }

    // Limpiar mensajes cuando cambia el estado
    LaunchedEffect(authState) {
        println("🔍 DEBUG: Estado cambiado a: $authState")
        when (authState) {
            is AuthState.Error -> {
                println("❌ DEBUG: Error en estado: ${(authState as AuthState.Error).message}")
                errorMessage = (authState as AuthState.Error).message
                successMessage = ""
            }
            is AuthState.RegistrationSuccess -> {
                println("✅ DEBUG: Registro exitoso: ${(authState as AuthState.RegistrationSuccess).message}")
                successMessage = (authState as AuthState.RegistrationSuccess).message
                errorMessage = ""
                // Cambiar a modo login después de registro exitoso
                isLoginMode = true
                // Limpiar formulario
                email = ""
                password = ""
                confirmPassword = ""
            }
            is AuthState.Loading -> {
                println("🔄 DEBUG: Estado Loading")
            }
            is AuthState.SignedIn -> {
                println("✅ DEBUG: SignedIn con usuario: ${(authState as AuthState.SignedIn).user.email}")
            }
            is AuthState.TraditionalSignedIn -> {
                println("✅ DEBUG: TraditionalSignedIn con usuario: ${(authState as AuthState.TraditionalSignedIn).username}")
            }
            is AuthState.EmailSignedIn -> {
                println("✅ DEBUG: EmailSignedIn con email: ${(authState as AuthState.EmailSignedIn).email}")
            }
            is AuthState.SignedOut -> {
                println("🔓 DEBUG: SignedOut")
                if (errorMessage.isNotEmpty() && authState !is AuthState.Error) {
                    errorMessage = ""
                }
                if (successMessage.isNotEmpty()) {
                    successMessage = ""
                }
            }
        }
    }

    // Limpiar mensajes de éxito después de 5 segundos
    LaunchedEffect(successMessage) {
        if (successMessage.isNotEmpty()) {
            kotlinx.coroutines.delay(5000)
            successMessage = ""
        }
    }

    // Validaciones en tiempo real
    LaunchedEffect(email) {
        emailError = when {
            email.isNotEmpty() && !authViewModel.isValidEmail(email) -> "El email debe contener un @ válido"
            email.isNotEmpty() && !isLoginMode && email == "admin@gmail.com" -> "Este email está reservado"
            email.isNotEmpty() && !isLoginMode && email != "admin@gmail.com" && authViewModel.isEmailRegistered(email) -> "Este email ya está registrado"
            else -> ""
        }
    }

    LaunchedEffect(password) {
        passwordError = when {
            password.isNotEmpty() && password.length < 6 -> "Mínimo 6 caracteres"
            else -> ""
        }
    }

    LaunchedEffect(confirmPassword, password) {
        confirmPasswordError = when {
            !isLoginMode && confirmPassword.isNotEmpty() && confirmPassword != password -> "Las contraseñas no coinciden"
            else -> ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Título de la app
        Text(
            text = "BeerBattle",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = if (isLoginMode) "Bienvenido" else "Crear cuenta",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Card contenedor del formulario
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isLoginMode) "Iniciar Sesión" else "Registrarse",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Selector de modo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            isLoginMode = true
                            errorMessage = ""
                            successMessage = ""
                            authViewModel.clearError()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isLoginMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(
                            text = "Iniciar Sesión",
                            fontWeight = if (isLoginMode) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    TextButton(
                        onClick = {
                            isLoginMode = false
                            errorMessage = ""
                            successMessage = ""
                            authViewModel.clearError()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (!isLoginMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    ) {
                        Text(
                            text = "Registrarse",
                            fontWeight = if (!isLoginMode) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                HorizontalDivider()

                // Manejo de estados de Google Sign-In
                when (authState) {
                    is AuthState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text(
                            text = if (isLoginMode) "Iniciando sesión..." else "Registrando usuario...",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {
                        GoogleSignInButton(
                            onClick = {
                                println("🔍 DEBUG: Click en botón de Google")
                                errorMessage = ""
                                successMessage = ""
                                // Limpiar sesión anterior antes de hacer login con Google
                                authViewModel.clearPreviousSession()
                                println("🔍 DEBUG: Sesión anterior limpiada")

                                // Ahora llamar directamente al ViewModel
                                println("🔍 DEBUG: Llamando signInWithGoogle...")
                                authViewModel.signInWithGoogle(context)
                                println("🔍 DEBUG: signInWithGoogle llamado")
                            },
                            enabled = authState !is AuthState.Loading
                        )
                    }
                }

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "  O  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // Campo de email (para registro y login)
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = ""
                        successMessage = ""
                        authViewModel.clearError()
                    },
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email"
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = authState !is AuthState.Loading,
                    isError = emailError.isNotEmpty(),
                    supportingText = if (emailError.isNotEmpty()) {
                        { Text(emailError, color = MaterialTheme.colorScheme.error) }
                    } else null
                )

                // Campo de contraseña
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = ""
                        successMessage = ""
                        authViewModel.clearError()
                    },
                    label = { Text("Contraseña") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Contraseña"
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = authState !is AuthState.Loading,
                    isError = passwordError.isNotEmpty(),
                    supportingText = if (passwordError.isNotEmpty()) {
                        { Text(passwordError, color = MaterialTheme.colorScheme.error) }
                    } else null
                )

                // Campo de confirmar contraseña (solo para registro)
                if (!isLoginMode) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = ""
                            successMessage = ""
                            authViewModel.clearError()
                        },
                        label = { Text("Confirmar contraseña") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Confirmar contraseña"
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = authState !is AuthState.Loading,
                        isError = confirmPasswordError.isNotEmpty(),
                        supportingText = if (confirmPasswordError.isNotEmpty()) {
                            { Text(confirmPasswordError, color = MaterialTheme.colorScheme.error) }
                        } else null
                    )
                }

                // Mensaje de error
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // Mensaje de éxito
                if (successMessage.isNotEmpty()) {
                    Text(
                        text = successMessage,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botón principal
                Button(
                    onClick = {
                        if (isLoginMode) {
                            // Lógica de login
                            when {
                                email.isBlank() -> {
                                    errorMessage = "Por favor ingresa tu email"
                                }
                                password.isBlank() -> {
                                    errorMessage = "Por favor ingresa tu contraseña"
                                }
                                email == "admin@gmail.com" -> {
                                    // Login tradicional para admin
                                    authViewModel.clearPreviousSession()
                                    authViewModel.signInTraditional(email, password)
                                }
                                else -> {
                                    // Login con email registrado
                                    authViewModel.clearPreviousSession()
                                    authViewModel.signInWithEmail(email, password)
                                }
                            }
                        } else {
                            // Lógica de registro
                            when {
                                email.isBlank() -> {
                                    errorMessage = "Por favor ingresa tu email"
                                }
                                password.isBlank() -> {
                                    errorMessage = "Por favor ingresa tu contraseña"
                                }
                                confirmPassword.isBlank() -> {
                                    errorMessage = "Por favor confirma tu contraseña"
                                }
                                emailError.isNotEmpty() || passwordError.isNotEmpty() || confirmPasswordError.isNotEmpty() -> {
                                    errorMessage = "Por favor corrige los errores antes de continuar"
                                }
                                else -> {
                                    authViewModel.registerUser(email, password, confirmPassword)
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = authState !is AuthState.Loading
                ) {
                    Text(
                        text = if (isLoginMode) "Iniciar Sesión" else "Registrarse",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Información adicional
                if (isLoginMode) {
                    Text(
                        text = "No compartas tu email y contraseña con nadie :)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = "Al registrarte podrás usar tu email para iniciar sesión",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = Color.Black
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle, // Puedes reemplazar con un icono de Google
                contentDescription = "Google",
                modifier = Modifier.size(20.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Continuar con Google",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Black
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    Calis1Theme {
        LoginScreen(onLoginSuccess = {})
    }
}