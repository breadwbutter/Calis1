package com.example.calis1.viewmodel

import android.content.Context
import androidx.credentials.*
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.*
import com.example.calis1.R

class AuthViewModel : ViewModel() {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    init {
        _authState.value = if (auth.currentUser != null) {
            AuthState.SignedIn(auth.currentUser!!)
        } else {
            AuthState.SignedOut
        }
    }

    suspend fun signInWithGoogle(
        context: Context,
        credentialManager: CredentialManager
    ) {
        try {
            _authState.value = AuthState.Loading

            // Generate secure nonce for replay attack prevention
            val rawNonce = UUID.randomUUID().toString()
            val hashedNonce = MessageDigest.getInstance("SHA-256")
                .digest(rawNonce.toByteArray())
                .fold("") { str, it -> str + "%02x".format(it) }

            // Configure Google Sign-In option
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Allow new users
                .setServerClientId(context.getString(R.string.web_client_id))
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            // Create credential request
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            handleSignInResult(result)

        } catch (e: GetCredentialCancellationException) {
            _authState.value = AuthState.Error("Sign-in cancelled by user")
        } catch (e: NoCredentialException) {
            _authState.value = AuthState.Error("No Google accounts found. Please add a Google account to your device.")
        } catch (e: GetCredentialException) {
            _authState.value = AuthState.Error("Authentication failed: ${e.message}")
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Unexpected error: ${e.message}")
        }
    }

    private suspend fun handleSignInResult(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken

                        // Create Firebase credential and sign in
                        val firebaseCredential = GoogleAuthProvider
                            .getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(firebaseCredential).await()

                        authResult.user?.let { user ->
                            _authState.value = AuthState.SignedIn(user)
                        } ?: run {
                            _authState.value = AuthState.Error("Sign-in failed: User is null")
                        }

                    } catch (e: GoogleIdTokenParsingException) {
                        _authState.value = AuthState.Error("Invalid Google ID token")
                    }
                }
            }
        }
    }

    suspend fun signOut(credentialManager: CredentialManager) {
        try {
            auth.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            _authState.value = AuthState.SignedOut
        } catch (e: ClearCredentialException) {
            _authState.value = AuthState.Error("Sign-out failed: ${e.message}")
        }
    }
}

sealed class AuthState {
    object Loading : AuthState()
    object SignedOut : AuthState()
    data class SignedIn(val user: FirebaseUser) : AuthState()
    data class Error(val message: String) : AuthState()
}