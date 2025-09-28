package com.example.calis1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calis1.data.database.AppDatabase
import com.example.calis1.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SettingsRepository

    // Estados reactivos con StateFlow
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // Usuario actual
    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    // Configuraciones de la aplicación
    val configuraciones: StateFlow<AppConfiguraciones>

    init {
        repository = SettingsRepository(application.applicationContext)

        // Cargar configuraciones desde el repository
        configuraciones = repository.getConfiguraciones()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = AppConfiguraciones()
            )

        // Observar cambios en las configuraciones
        observarCambios()
    }

    private fun observarCambios() {
        viewModelScope.launch {
            configuraciones.collect { config ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false
                )
            }
        }
    }

    /**
     * Configurar el usuario actual
     */
    fun configurarUsuario(userId: String) {
        _currentUserId.value = userId
    }

    /**
     * Cambiar el tema de la aplicación
     */
    fun cambiarTema(nuevoTema: TipoTema) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                repository.cambiarTema(nuevoTema)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastAction = "Tema cambiado a ${nuevoTema.descripcion}"
                )

            } catch (e: Exception) {
                updateError("Error al cambiar tema: ${e.message}")
            }
        }
    }

    /**
     * Cambiar el sistema de unidades
     */
    fun cambiarUnidades(nuevoSistema: SistemaUnidades) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                repository.cambiarSistemaUnidades(nuevoSistema)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastAction = "Unidades cambiadas a ${nuevoSistema.descripcion}"
                )

            } catch (e: Exception) {
                updateError("Error al cambiar unidades: ${e.message}")
            }
        }
    }

    /**
     * Borrar todos los datos del usuario
     */
    fun borrarTodosLosDatos() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val userId = _currentUserId.value
                if (userId.isEmpty()) {
                    updateError("Usuario no válido")
                    return@launch
                }

                // Borrar todos los datos del usuario
                repository.borrarTodosLosDatos(userId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastAction = "Todos los datos han sido eliminados exitosamente"
                )

            } catch (e: Exception) {
                updateError("Error al eliminar datos: ${e.message}")
            }
        }
    }

    /**
     * Limpiar mensajes de error o éxito
     */
    fun limpiarMensajes() {
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
}

/**
 * Estado de UI para la pantalla de configuraciones
 */
data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastAction: String? = null
)

/**
 * Configuraciones de la aplicación
 */
data class AppConfiguraciones(
    val tema: TipoTema = TipoTema.SISTEMA,
    val sistemaUnidades: SistemaUnidades = SistemaUnidades.MILILITROS
)

/**
 * Tipos de tema disponibles
 */
enum class TipoTema(val descripcion: String) {
    CLARO("Modo Claro"),
    OSCURO("Modo Oscuro"),
    SISTEMA("Seguir Sistema")
}

/**
 * Sistemas de unidades disponibles
 */
enum class SistemaUnidades(val descripcion: String, val simbolo: String, val factorConversion: Double) {
    MILILITROS("Mililitros", "ml", 1.0),
    ONZAS("Onzas", "oz", 0.033814022558919045) // 1 ml = 0.033814 oz
}

/**
 * Clase de utilidad para conversión de unidades
 */
object ConvertirUnidades {
    /**
     * Convertir mililitros a onzas
     */
    fun mlAOnzas(ml: Double): Double = ml * SistemaUnidades.ONZAS.factorConversion

    /**
     * Convertir onzas a mililitros
     */
    fun onzasAMl(onzas: Double): Double = onzas / SistemaUnidades.ONZAS.factorConversion

    /**
     * Convertir a la unidad deseada
     */
    fun convertir(valor: Double, desde: SistemaUnidades, hacia: SistemaUnidades): Double {
        return when {
            desde == hacia -> valor
            desde == SistemaUnidades.MILILITROS && hacia == SistemaUnidades.ONZAS -> mlAOnzas(valor)
            desde == SistemaUnidades.ONZAS && hacia == SistemaUnidades.MILILITROS -> onzasAMl(valor)
            else -> valor
        }
    }

    /**
     * Formatear valor con unidades
     */
    fun formatearConUnidades(valor: Double, sistema: SistemaUnidades, decimales: Int = 1): String {
        return "${String.format("%.${decimales}f", valor)} ${sistema.simbolo}"
    }
}