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
    }

    /**
     * Guardar sesión después de login exitoso
     */
    fun saveSession(loginType: String, username: String? = null, user: FirebaseUser? = null) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_LOGIN_TYPE, loginType)

            when (loginType) {
                LOGIN_TYPE_TRADITIONAL -> {
                    putString(KEY_USERNAME, username ?: "")
                }
                LOGIN_TYPE_GOOGLE -> {
                    putString(KEY_USER_EMAIL, user?.email ?: "")
                    putString(KEY_USER_ID, user?.uid ?: "")
                    putString(KEY_USERNAME, user?.displayName ?: user?.email ?: "")
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
            LOGIN_TYPE_TRADITIONAL -> {
                // Para login tradicional, solo verificar local
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