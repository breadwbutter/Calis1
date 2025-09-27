package com.example.calis1.viewmodel

import android.content.Context
import androidx.credentials.*
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.calis1.SessionManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.*
import com.example.calis1.R

class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private var sessionManager: SessionManager? = null

    /**
     * Inicializar con contexto y verificar sesión existente
     */
    fun initialize(context: Context) {
        println("🔍 DEBUG: Inicializando AuthViewModel...")
        if (sessionManager == null) {
            sessionManager = SessionManager(context)
            println("🔍 DEBUG: SessionManager creado")
            checkExistingSession()
        } else {
            println("🔍 DEBUG: SessionManager ya existe")
        }
    }

    /**
     * Verificar si hay una sesión activa al iniciar la app
     */
    private fun checkExistingSession() {
        println("🔍 DEBUG: Verificando sesión existente...")
        viewModelScope.launch {
            try {
                val sessionManager = this@AuthViewModel.sessionManager
                if (sessionManager == null) {
                    println("🔍 DEBUG: SessionManager es null")
                    _authState.value = AuthState.SignedOut
                    return@launch
                }

                // Sincronizar estado con Firebase
                val hasValidSession = sessionManager.syncFirebaseState()
                println("🔍 DEBUG: ¿Sesión válida? $hasValidSession")

                if (hasValidSession) {
                    val userData = sessionManager.getUserData()
                    println("🔍 DEBUG: Datos de usuario: $userData")

                    when (userData?.loginType) {
                        SessionManager.LOGIN_TYPE_TRADITIONAL -> {
                            println("🔍 DEBUG: Restaurando sesión tradicional: ${userData.username}")
                            _authState.value = AuthState.TraditionalSignedIn(userData.username)
                        }
                        SessionManager.LOGIN_TYPE_GOOGLE -> {
                            userData.firebaseUser?.let { user ->
                                println("🔍 DEBUG: Restaurando sesión Google: ${user.email}")
                                _authState.value = AuthState.SignedIn(user)
                            } ?: run {
                                println("🔍 DEBUG: Usuario Firebase es null")
                                _authState.value = AuthState.SignedOut
                            }
                        }
                        else -> {
                            println("🔍 DEBUG: Tipo de login desconocido: ${userData?.loginType}")
                            _authState.value = AuthState.SignedOut
                        }
                    }
                } else {
                    println("🔍 DEBUG: No hay sesión válida")
                    _authState.value = AuthState.SignedOut
                }
            } catch (e: Exception) {
                println("❌ DEBUG: Error verificando sesión: ${e.message}")
                e.printStackTrace()
                _authState.value = AuthState.SignedOut
            }
        }
    }

    /**
     * Limpiar sesión anterior (para cambio de tipo de login)
     */
    fun clearPreviousSession() {
        println("🔍 DEBUG: Limpiando sesión anterior...")
        sessionManager?.clearSession()
        println("🔍 DEBUG: Sesión anterior limpiada")
    }

    /**
     * Login tradicional (sin Firebase) con persistencia
     */
    fun signInTraditional(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            // Simular pequeño delay para mostrar loading
            kotlinx.coroutines.delay(500)

            // Validación simple
            if (username == "admin" && password == "123456") {
                // Guardar sesión
                sessionManager?.saveSession(
                    loginType = SessionManager.LOGIN_TYPE_TRADITIONAL,
                    username = username
                )

                _authState.value = AuthState.TraditionalSignedIn(username)
            } else {
                _authState.value = AuthState.Error("Usuario o contraseña incorrectos")
            }
        }
    }

    /**
     * Login con Google con persistencia (versión mejorada sin cancelación)
     */
    fun signInWithGoogle(context: Context) {
        println("🔍 DEBUG: Iniciando login con Google desde ViewModel...")

        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                println("🔍 DEBUG: Estado cambiado a Loading")

                val credentialManager = CredentialManager.create(context)

                // Generate secure nonce for replay attack prevention
                val rawNonce = UUID.randomUUID().toString()
                val hashedNonce = MessageDigest.getInstance("SHA-256")
                    .digest(rawNonce.toByteArray())
                    .fold("") { str, it -> str + "%02x".format(it) }

                println("🔍 DEBUG: Nonce generado")

                val webClientId = context.getString(R.string.web_client_id)
                println("🔍 DEBUG: Web Client ID completo: $webClientId")

                // Configure Google Sign-In option
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false) // Allow new users
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .setNonce(hashedNonce)
                    .build()

                println("🔍 DEBUG: Google ID Option configurado")

                // Create credential request
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                println("🔍 DEBUG: Request creado, obteniendo credenciales...")

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                println("🔍 DEBUG: Credenciales obtenidas, procesando resultado...")
                handleSignInResult(result)

            } catch (e: GetCredentialCancellationException) {
                println("❌ DEBUG: Login cancelado por usuario")
                _authState.value = AuthState.Error("Inicio de sesión cancelado por el usuario")
            } catch (e: NoCredentialException) {
                println("❌ DEBUG: No hay credenciales: ${e.message}")
                _authState.value = AuthState.Error("No se encontraron cuentas de Google. Por favor agrega una cuenta de Google a tu dispositivo.")
            } catch (e: GetCredentialException) {
                println("❌ DEBUG: Error de credenciales: ${e.message}")
                _authState.value = AuthState.Error("Error de autenticación: ${e.message}")
            } catch (e: kotlinx.coroutines.CancellationException) {
                println("❌ DEBUG: Corrutina cancelada - Esto NO debería pasar ahora")
                _authState.value = AuthState.Error("Login cancelado inesperadamente")
            } catch (e: Exception) {
                println("❌ DEBUG: Error inesperado: ${e.message}")
                e.printStackTrace()
                _authState.value = AuthState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    /**
     * Manejar resultado del login de Google
     */
    private suspend fun handleSignInResult(result: GetCredentialResponse) {
        println("🔍 DEBUG: Manejando resultado del login...")

        when (val credential = result.credential) {
            is CustomCredential -> {
                println("🔍 DEBUG: Credencial customizada recibida")
                println("🔍 DEBUG: Tipo de credencial: ${credential.type}")

                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        println("🔍 DEBUG: Procesando token de Google...")

                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken

                        println("🔍 DEBUG: Token extraído, autenticando con Firebase...")

                        // Create Firebase credential and sign in
                        val firebaseCredential = GoogleAuthProvider
                            .getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(firebaseCredential).await()

                        println("🔍 DEBUG: Autenticación con Firebase completada")

                        authResult.user?.let { user ->
                            println("🔍 DEBUG: Usuario obtenido: ${user.displayName} (${user.email})")

                            // Guardar sesión exitosa
                            sessionManager?.saveSession(
                                loginType = SessionManager.LOGIN_TYPE_GOOGLE,
                                user = user
                            )

                            println("🔍 DEBUG: Sesión guardada, actualizando estado...")
                            _authState.value = AuthState.SignedIn(user)
                            println("✅ DEBUG: Login completado exitosamente!")
                        } ?: run {
                            println("❌ DEBUG: Usuario nulo después de autenticación")
                            _authState.value = AuthState.Error("Error al iniciar sesión: Usuario nulo")
                        }

                    } catch (e: GoogleIdTokenParsingException) {
                        println("❌ DEBUG: Error parseando token: ${e.message}")
                        _authState.value = AuthState.Error("Token de Google ID inválido")
                    } catch (e: Exception) {
                        println("❌ DEBUG: Error en autenticación Firebase: ${e.message}")
                        e.printStackTrace()
                        _authState.value = AuthState.Error("Error al autenticar con Firebase: ${e.message}")
                    }
                } else {
                    println("❌ DEBUG: Tipo de credencial no reconocido: ${credential.type}")
                    _authState.value = AuthState.Error("Tipo de credencial no soportado")
                }
            }
            else -> {
                println("❌ DEBUG: Credencial no es CustomCredential: ${credential::class.java}")
                _authState.value = AuthState.Error("Tipo de credencial no reconocido")
            }
        }
    }

    /**
     * Cerrar sesión (limpiar persistencia)
     */
    fun signOut(context: Context) {
        viewModelScope.launch {
            try {
                println("🔍 DEBUG: Iniciando logout...")

                // Limpiar sesión persistente
                sessionManager?.clearSession()
                println("🔍 DEBUG: Sesión persistente limpiada")

                // Limpiar Firebase Auth (por si había usuario de Google)
                auth.signOut()
                println("🔍 DEBUG: Firebase Auth limpiado")

                // Limpiar Credential Manager
                try {
                    val credentialManager = CredentialManager.create(context)
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    println("🔍 DEBUG: Credential Manager limpiado")
                } catch (e: ClearCredentialException) {
                    // Log error but continue with logout
                    println("⚠️ DEBUG: Error limpiando credential state: ${e.message}")
                }

                // Limpiar estado local
                _authState.value = AuthState.SignedOut
                println("✅ DEBUG: Logout completado")

            } catch (e: Exception) {
                println("❌ DEBUG: Error durante logout: ${e.message}")
                // Forzar logout incluso si hay error
                sessionManager?.clearSession()
                _authState.value = AuthState.SignedOut
            }
        }
    }

    /**
     * Método para limpiar errores
     */
    fun clearError() {
        println("🔍 DEBUG: Limpiando error y regresando a SignedOut")
        _authState.value = AuthState.SignedOut
    }

    /**
     * Verificar si el usuario está logueado
     */
    fun isUserLoggedIn(): Boolean {
        return sessionManager?.isLoggedIn() ?: false
    }

    /**
     * Obtener datos del usuario actual
     */
    fun getCurrentUserData(): SessionManager.SessionData? {
        return sessionManager?.getUserData()
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object SignedOut : AuthState()
    data class SignedIn(val user: FirebaseUser) : AuthState()
    data class TraditionalSignedIn(val username: String) : AuthState()
    data class Error(val message: String) : AuthState()
}