package com.example.calis1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calis1.data.database.AppDatabase
import com.example.calis1.data.entity.AlcoholRecord
import com.example.calis1.repository.AlcoholTrackingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class AlcoholTrackingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AlcoholTrackingRepository

    // Estados reactivos con StateFlow
    private val _uiState = MutableStateFlow(AlcoholTrackingUiState())
    val uiState: StateFlow<AlcoholTrackingUiState> = _uiState.asStateFlow()

    // Usuario actual (será establecido desde MainActivity)
    private val _currentUserId = MutableStateFlow("")
    private val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    // Semana actual
    private val _semanaActual = MutableStateFlow(AlcoholRecord.getSemanaInicio())
    val semanaActual: StateFlow<LocalDate> = _semanaActual.asStateFlow()

    // Registros de la semana actual
    val registrosSemana: StateFlow<List<AlcoholRecord>>

    // Total de alcohol puro de la semana
    val totalAlcoholPuro: StateFlow<Double>

    init {
        val alcoholRecordDao = AppDatabase.getDatabase(application).alcoholRecordDao()
        repository = AlcoholTrackingRepository(alcoholRecordDao, application.applicationContext)

        // Configurar Flow combinado para registros de semana
        registrosSemana = combine(
            currentUserId,
            semanaActual
        ) { userId, semana ->
            if (userId.isNotEmpty()) {
                repository.getRegistrosSemana(userId, semana.toString())
            } else {
                flowOf(emptyList())
            }
        }.flatMapLatest { it }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        // Calcular total de alcohol puro
        totalAlcoholPuro = registrosSemana
            .map { registros ->
                registros.sumOf { it.calcularAlcoholPuro() }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0.0
            )

        // Observar cambios para actualizar UI state
        observeChanges()
    }

    private fun observeChanges() {
        viewModelScope.launch {
            combine(
                registrosSemana,
                totalAlcoholPuro
            ) { registros, total ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    registrosPorDia = crearMapaRegistrosPorDia(registros),
                    totalAlcoholPuro = total,
                    estadoSalud = calcularEstadoSalud(total),
                    tieneRegistros = registros.isNotEmpty()
                )
            }.collect()
        }
    }

    /**
     * Establecer el usuario actual
     */
    fun setCurrentUser(userId: String) {
        _currentUserId.value = userId
    }

    /**
     * Agregar o actualizar registro de un día específico
     */
    fun actualizarRegistro(
        diaSemana: Int,
        nombreBebida: String,
        mililitros: String,
        porcentajeAlcohol: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Validaciones
                val ml = mililitros.toIntOrNull()
                val porcentaje = porcentajeAlcohol.toDoubleOrNull()

                when {
                    nombreBebida.isBlank() -> {
                        updateError("El nombre de la bebida no puede estar vacío")
                        return@launch
                    }
                    ml == null || ml < 0 -> {
                        updateError("Los mililitros deben ser un número válido mayor o igual a 0")
                        return@launch
                    }
                    ml > 5000 -> {
                        updateError("Los mililitros no pueden exceder 5000ml por día")
                        return@launch
                    }
                    porcentaje == null || porcentaje < 0 -> {
                        updateError("El porcentaje de alcohol debe ser un número válido mayor o igual a 0")
                        return@launch
                    }
                    porcentaje > 100 -> {
                        updateError("El porcentaje de alcohol no puede exceder 100%")
                        return@launch
                    }
                }

                val fechaDia = _semanaActual.value.plusDays((diaSemana - 1).toLong())

                val registro = AlcoholRecord(
                    userId = _currentUserId.value,
                    fecha = fechaDia.toString(),
                    diaSemana = diaSemana,
                    nombreBebida = nombreBebida.trim(),
                    mililitros = ml,
                    porcentajeAlcohol = porcentaje,
                    semanaInicio = _semanaActual.value.toString()
                )

                repository.insertRegistro(registro)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastAction = "Registro actualizado para ${registro.getNombreDia()}"
                )

            } catch (e: Exception) {
                updateError("Error al guardar registro: ${e.message}")
            }
        }
    }

    /**
     * Eliminar registro de un día específico
     */
    fun eliminarRegistro(diaSemana: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val registro = repository.getRegistroPorDia(
                    _currentUserId.value,
                    _semanaActual.value.toString(),
                    diaSemana
                )

                if (registro != null) {
                    repository.deleteRegistro(registro)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastAction = "Registro eliminado para ${registro.getNombreDia()}"
                    )
                } else {
                    updateError("No hay registro para eliminar en este día")
                }

            } catch (e: Exception) {
                updateError("Error al eliminar registro: ${e.message}")
            }
        }
    }

    /**
     * Navegar a semana anterior
     */
    fun irSemanaAnterior() {
        _semanaActual.value = _semanaActual.value.minusWeeks(1)
        _uiState.value = _uiState.value.copy(
            lastAction = "Navegando a semana anterior"
        )
    }

    /**
     * Navegar a semana siguiente
     */
    fun irSemanaSiguiente() {
        _semanaActual.value = _semanaActual.value.plusWeeks(1)
        _uiState.value = _uiState.value.copy(
            lastAction = "Navegando a semana siguiente"
        )
    }

    /**
     * Ir a la semana actual
     */
    fun irSemanaActual() {
        _semanaActual.value = AlcoholRecord.getSemanaInicio()
        _uiState.value = _uiState.value.copy(
            lastAction = "Regresando a semana actual"
        )
    }

    /**
     * Crear mapa de registros organizados por día (1-7)
     */
    private fun crearMapaRegistrosPorDia(registros: List<AlcoholRecord>): Map<Int, AlcoholRecord?> {
        val mapa = mutableMapOf<Int, AlcoholRecord?>()
        for (i in 1..7) {
            mapa[i] = registros.find { it.diaSemana == i }
        }
        return mapa
    }

    /**
     * Calcular estado de salud basado en alcohol consumido
     */
    private fun calcularEstadoSalud(totalAlcoholPuro: Double): EstadoSalud {
        return when {
            totalAlcoholPuro <= 140.0 -> EstadoSalud.SALUDABLE
            totalAlcoholPuro <= 280.0 -> EstadoSalud.EN_RIESGO
            else -> EstadoSalud.EXCESO
        }
    }

    /**
     * Limpiar mensajes de error o éxito
     */
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

    /**
     * Obtener información del rango de semana actual
     */
    fun getRangoSemana(): String {
        val inicio = _semanaActual.value
        val fin = inicio.plusDays(6)
        return AlcoholRecord.formatearRangoSemana(inicio, fin)
    }

    /**
     * Verificar si estamos en la semana actual
     */
    fun esSemanaActual(): Boolean {
        return _semanaActual.value == AlcoholRecord.getSemanaInicio()
    }
}

/**
 * Estado de UI para el seguimiento de alcohol
 */
data class AlcoholTrackingUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastAction: String? = null,
    val registrosPorDia: Map<Int, AlcoholRecord?> = emptyMap(),
    val totalAlcoholPuro: Double = 0.0,
    val estadoSalud: EstadoSalud = EstadoSalud.SALUDABLE,
    val tieneRegistros: Boolean = false
)

/**
 * Estados de salud basados en consumo de alcohol
 */
enum class EstadoSalud(val mensaje: String, val descripcion: String) {
    SALUDABLE("Semana saludable", "Tu consumo está dentro de los límites recomendados"),
    EN_RIESGO("En riesgo de exceso", "Tu consumo está por encima de lo recomendado"),
    EXCESO("Exceso de alcohol", "Tu consumo supera significativamente los límites saludables")
}