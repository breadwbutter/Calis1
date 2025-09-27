package com.example.calis1

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "user_session",
        Context.MODE_PRIVATE
    )

    // SharedPreferences separado para usuarios registrados
    private val userPrefs: SharedPreferences = context.getSharedPreferences(
        "registered_users",
        Context.MODE_PRIVATE
    )

    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_LOGIN_TYPE = "login_type"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ID = "user_id"

        // Tipos de login
        const val LOGIN_TYPE_TRADITIONAL = "traditional"
        const val LOGIN_TYPE_GOOGLE = "google"
        const val LOGIN_TYPE_EMAIL = "email"
    }

    /**
     * Registrar nuevo usuario con email y contraseña
     */
    fun registerUser(email: String, password: String): RegisterResult {
        // Validar formato de email
        if (!isValidEmail(email)) {
            return RegisterResult.InvalidEmail
        }

        // Validar contraseña (mínimo 6 caracteres)
        if (password.length < 6) {
            return RegisterResult.WeakPassword
        }

        // Verificar si el email ya está registrado
        if (userPrefs.contains(email)) {
            return RegisterResult.EmailAlreadyExists
        }

        // Guardar usuario (en producción deberías hashear la contraseña)
        userPrefs.edit().apply {
            putString(email, password)
            apply()
        }

        return RegisterResult.Success
    }

    /**
     * Verificar credenciales de usuario registrado
     */
    fun verifyUserCredentials(email: String, password: String): Boolean {
        val storedPassword = userPrefs.getString(email, null)
        return storedPassword == password
    }

    /**
     * Validar formato de email
     */
    fun isValidEmail(email: String): Boolean {
        return email.contains("@") &&
                email.indexOf("@") > 0 &&
                email.indexOf("@") < email.length - 1 &&
                email.count { it == '@' } == 1
    }

    /**
     * Obtener lista de usuarios registrados (solo emails)
     */
    fun getRegisteredEmails(): Set<String> {
        return userPrefs.all.keys
    }

    /**
     * Verificar si un email está registrado
     */
    fun isEmailRegistered(email: String): Boolean {
        return userPrefs.contains(email)
    }

    /**
     * Guardar sesión después de login exitoso
     */
    fun saveSession(loginType: String, username: String? = null, user: FirebaseUser? = null, email: String? = null) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_LOGIN_TYPE, loginType)

            when (loginType) {
                LOGIN_TYPE_TRADITIONAL -> {
                    putString(KEY_USERNAME, username ?: "")
                    putString(KEY_USER_EMAIL, email ?: "")
                }
                LOGIN_TYPE_GOOGLE -> {
                    putString(KEY_USER_EMAIL, user?.email ?: "")
                    putString(KEY_USER_ID, user?.uid ?: "")
                    putString(KEY_USERNAME, user?.displayName ?: user?.email ?: "")
                }
                LOGIN_TYPE_EMAIL -> {
                    putString(KEY_USER_EMAIL, email ?: "")
                    putString(KEY_USERNAME, email?.substringBefore("@") ?: "")
                }
            }
            apply()
        }
    }

    /**
     * Verificar si hay una sesión activa
     */
    fun isLoggedIn(): Boolean {
        val hasLocalSession = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        val loginType = getLoginType()

        return when (loginType) {
            LOGIN_TYPE_TRADITIONAL, LOGIN_TYPE_EMAIL -> {
                // Para login tradicional y email, solo verificar local
                hasLocalSession
            }
            LOGIN_TYPE_GOOGLE -> {
                // Para Google, verificar local Y Firebase
                hasLocalSession && auth.currentUser != null
            }
            else -> false
        }
    }

    /**
     * Obtener tipo de login actual
     */
    fun getLoginType(): String? {
        return prefs.getString(KEY_LOGIN_TYPE, null)
    }

    /**
     * Obtener datos del usuario guardado
     */
    fun getUserData(): SessionData? {
        if (!isLoggedIn()) return null

        return SessionData(
            loginType = getLoginType() ?: "",
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            email = prefs.getString(KEY_USER_EMAIL, "") ?: "",
            userId = prefs.getString(KEY_USER_ID, "") ?: "",
            firebaseUser = auth.currentUser
        )
    }

    /**
     * Limpiar sesión (solo en logout explícito)
     */
    fun clearSession() {
        println("🔍 DEBUG: SessionManager - Limpiando SharedPreferences...")
        prefs.edit().clear().apply()
        println("🔍 DEBUG: SessionManager - SharedPreferences limpiado")
    }

    /**
     * Verificar y sincronizar estado de Firebase
     */
    fun syncFirebaseState(): Boolean {
        val loginType = getLoginType()

        if (loginType == LOGIN_TYPE_GOOGLE) {
            // Si era login de Google pero Firebase perdió la sesión, limpiar local
            if (auth.currentUser == null) {
                clearSession()
                return false
            }
        }

        return isLoggedIn()
    }

    /**
     * Resultados del registro
     */
    sealed class RegisterResult {
        object Success : RegisterResult()
        object InvalidEmail : RegisterResult()
        object WeakPassword : RegisterResult()
        object EmailAlreadyExists : RegisterResult()
    }

    /**
     * Datos de sesión
     */
    data class SessionData(
        val loginType: String,
        val username: String,
        val email: String,
        val userId: String,
        val firebaseUser: FirebaseUser?
    )
}