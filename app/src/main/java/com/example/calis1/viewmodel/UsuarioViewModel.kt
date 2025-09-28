package com.example.calis1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calis1.data.database.AppDatabase
import com.example.calis1.data.entity.Usuario
import com.example.calis1.repository.UsuarioRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class UsuarioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UsuarioRepository

    private val _uiState = MutableStateFlow(UsuarioUiState())
    val uiState: StateFlow<UsuarioUiState> = _uiState.asStateFlow()

    val allUsuarios: StateFlow<List<Usuario>>

    val usuariosCount: StateFlow<Int>

    init {
        val usuarioDao = AppDatabase.getDatabase(application).usuarioDao()
        repository = UsuarioRepository(usuarioDao, application.applicationContext)

        allUsuarios = repository.getAllUsuarios()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        usuariosCount = repository.getUsuariosCount()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

        setupCompleteSync()

        observeUsuarios()
    }

    private fun observeUsuarios() {
        viewModelScope.launch {
            allUsuarios.collect { usuarios ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    usuariosCount = usuarios.size,
                    hasUsers = usuarios.isNotEmpty()
                )
            }
        }
    }

    fun insertUsuario(nombre: String, edad: Int) {
        if (nombre.isBlank()) {
            updateError("El nombre no puede estar vacío")
            return
        }

        if (edad < 0 || edad > 150) {
            updateError("La edad debe estar entre 0 y 150 años")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val usuario = Usuario(
                    nombre = nombre.trim(),
                    edad = edad
                )

                repository.insertUsuario(usuario)

                // Actualizar estado de éxito
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastAction = "Usuario '$nombre' agregado exitosamente"
                )

            } catch (e: Exception) {
                updateError("Error al guardar usuario: ${e.message}")
            }
        }
    }

    fun deleteUsuario(usuario: Usuario) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                repository.deleteUsuario(usuario)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastAction = "Usuario '${usuario.nombre}' eliminado"
                )

            } catch (e: Exception) {
                updateError("Error al eliminar usuario: ${e.message}")
            }
        }
    }

    private fun setupCompleteSync() {
        repository.setupCompleteSync()
    }

    // Método para sincronización manual
    fun manualSync() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                repository.syncFromFirebase()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastAction = "Sincronización manual completada"
                )

            } catch (e: Exception) {
                updateError("Error en sincronización: ${e.message}")
            }
        }
    }

    fun forceSync() {
        repository.forceSync()
        _uiState.value = _uiState.value.copy(
            lastAction = "Sincronización con WorkManager iniciada"
        )
    }

    // Limpiar mensajes de error o éxito
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            lastAction = null
        )
    }

    private fun updateError(error: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = error,
            lastAction = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}

/**
 * Estado de UI para la pantalla de usuarios
 */
data class UsuarioUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastAction: String? = null,
    val usuariosCount: Int = 0,
    val hasUsers: Boolean = false
)