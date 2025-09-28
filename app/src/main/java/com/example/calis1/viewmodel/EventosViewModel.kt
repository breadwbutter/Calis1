package com.example.calis1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calis1.data.database.AppDatabase
import com.example.calis1.data.entity.Evento
import com.example.calis1.repository.EventosRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EventosViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: EventosRepository

    // Estados reactivos con StateFlow
    private val _uiState = MutableStateFlow(EventosUiState())
    val uiState: StateFlow<EventosUiState> = _uiState.asStateFlow()

    // Usuario actual (será establecido desde MainActivity)
    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    // NUEVO: Estado para búsqueda
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Evento>>(emptyList())
    val searchResults: StateFlow<List<Evento>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Flow de eventos directo desde repository
    val allEventos: StateFlow<List<Evento>>

    // Flow de conteo de eventos
    val eventosCount: StateFlow<Int>

    init {
        val eventoDao = AppDatabase.getDatabase(application).eventoDao()
        repository = EventosRepository(eventoDao, application.applicationContext)

        // Convertir Flow a StateFlow para mejor control
        allEventos = currentUserId.flatMapLatest { userId ->
            if (userId.isNotEmpty()) {
                repository.getAllEventos(userId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        eventosCount = currentUserId.flatMapLatest { userId ->
            if (userId.isNotEmpty()) {
                repository.getEventosCount(userId)
            } else {
                flowOf(0)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

        // Observar cambios en la lista para actualizar estado
        observeEventos()
    }

    private fun observeEventos() {
        viewModelScope.launch {
            combine(
                allEventos,
                eventosCount
            ) { eventos, count ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    eventosCount = count,
                    hasEventos = eventos.isNotEmpty()
                )
            }.collect()
        }
    }

    /**
     * Establecer el usuario actual
     */
    fun setCurrentUser(userId: String) {
        _currentUserId.value = userId

        // Configurar sincronización completa cuando se establece el usuario
        if (userId.isNotEmpty()) {
            repository.setupCompleteSync(userId)
        }
    }

    /**
     * NUEVO: Actualizar query de búsqueda
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * NUEVO: Ejecutar búsqueda de eventos
     */
    fun buscarEventos(query: String = _searchQuery.value) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        viewModelScope.launch {
            try {
                _isSearching.value = true
                _uiState.value = _uiState.value.copy(error = null)

                val userId = _currentUserId.value
                if (userId.isNotEmpty()) {
                    val resultados = repository.buscarEventos(userId, query)
                    _searchResults.value = resultados

                    _uiState.value = _uiState.value.copy(
                        lastAction = if (resultados.isNotEmpty()) {
                            "Se encontraron ${resultados.size} evento${if (resultados.size != 1) "s" else ""}"
                        } else {
                            "No se encontraron eventos que coincidan con '$query'"
                        }
                    )
                } else {
                    updateError("Usuario no válido")
                }

            } catch (e: Exception) {
                updateError("Error en la búsqueda: ${e.message}")
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * NUEVO: Búsqueda avanzada con opciones específicas
     */
    fun buscarEventosAvanzado(
        query: String,
        buscarTitulo: Boolean = true,
        buscarDescripcion: Boolean = true,
        buscarFecha: Boolean = true
    ) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        viewModelScope.launch {
            try {
                _isSearching.value = true
                _uiState.value = _uiState.value.copy(error = null)

                val userId = _currentUserId.value
                if (userId.isNotEmpty()) {
                    val resultados = repository.buscarEventosAvanzado(
                        userId = userId,
                        query = query,
                        buscarTitulo = buscarTitulo,
                        buscarDescripcion = buscarDescripcion,
                        buscarFecha = buscarFecha
                    )
                    _searchResults.value = resultados

                    val camposBuscados = mutableListOf<String>()
                    if (buscarTitulo) camposBuscados.add("título")
                    if (buscarDescripcion) camposBuscados.add("descripción")
                    if (buscarFecha) camposBuscados.add("fecha")

                    _uiState.value = _uiState.value.copy(
                        lastAction = if (resultados.isNotEmpty()) {
                            "Se encontraron ${resultados.size} evento${if (resultados.size != 1) "s" else ""} en: ${camposBuscados.joinToString(", ")}"
                        } else {
                            "No se encontraron eventos en ${camposBuscados.joinToString(", ")} que coincidan con '$query'"
                        }
                    )
                } else {
                    updateError("Usuario no válido")
                }

            } catch (e: Exception) {
                updateError("Error en la búsqueda avanzada: ${e.message}")
                _searchResults.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * NUEVO: Limpiar resultados de búsqueda
     */
    fun limpiarBusqueda() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false
        _uiState.value = _uiState.value.copy(lastAction = null)
    }

    /**
     * Insertar nuevo evento
     */
    fun insertEvento(titulo: String, descripcion: String, fecha: String) {
        if (titulo.isBlank()) {
            updateError("El título no puede estar vacío")
            return
        }

        if (descripcion.isBlank()) {
            updateError("La descripción no puede estar vacía")
            return
        }

        if (fecha.isBlank()) {
            updateError("La fecha no puede estar vacía")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val evento = Evento(
                    userId = _currentUserId.value,
                    titulo = titulo.trim(),
                    descripcion = descripcion.trim(),
                    fecha = fecha.trim()
                )

                repository.insertEvento(evento)

                // Actualizar estado de éxito
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastAction = "Evento '$titulo' agregado exitosamente"
                )

            } catch (e: Exception) {
                updateError("Error al guardar evento: ${e.message}")
            }
        }
    }

    /**
     * Actualizar evento existente
     */
    fun updateEvento(eventoId: String, titulo: String, descripcion: String, fecha: String) {
        if (titulo.isBlank()) {
            updateError("El título no puede estar vacío")
            return
        }

        if (descripcion.isBlank()) {
            updateError("La descripción no puede estar vacía")
            return
        }

        if (fecha.isBlank()) {
            updateError("La fecha no puede estar vacía")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val eventoExistente = repository.getEventoById(eventoId)
                if (eventoExistente != null) {
                    val eventoActualizado = eventoExistente.copy(
                        titulo = titulo.trim(),
                        descripcion = descripcion.trim(),
                        fecha = fecha.trim(),
                        timestamp = System.currentTimeMillis() // Actualizar timestamp
                    )

                    repository.updateEvento(eventoActualizado)

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastAction = "Evento actualizado exitosamente"
                    )
                } else {
                    updateError("Evento no encontrado")
                }

            } catch (e: Exception) {
                updateError("Error al actualizar evento: ${e.message}")
            }
        }
    }

    /**
     * Eliminar evento
     */
    fun deleteEvento(evento: Evento) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                repository.deleteEvento(evento)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastAction = "Evento '${evento.titulo}' eliminado"
                )

            } catch (e: Exception) {
                updateError("Error al eliminar evento: ${e.message}")
            }
        }
    }

    /**
     * Eliminar evento por ID
     */
    fun deleteEventoById(eventoId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val evento = repository.getEventoById(eventoId)
                if (evento != null) {
                    repository.deleteEvento(evento)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        lastAction = "Evento eliminado"
                    )
                } else {
                    updateError("Evento no encontrado")
                }

            } catch (e: Exception) {
                updateError("Error al eliminar evento: ${e.message}")
            }
        }
    }

    /**
     * Método para sincronización manual
     */
    fun manualSync() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val userId = _currentUserId.value
                if (userId.isNotEmpty()) {
                    repository.syncFromFirebase(userId)
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    lastAction = "Sincronización manual completada"
                )

            } catch (e: Exception) {
                updateError("Error en sincronización: ${e.message}")
            }
        }
    }

    /**
     * Método para forzar sincronización inmediata con WorkManager
     */
    fun forceSync() {
        repository.forceSync()
        _uiState.value = _uiState.value.copy(
            lastAction = "Sincronización con WorkManager iniciada"
        )
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
     * Limpiar recursos cuando el ViewModel se destruya
     */
    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}

/**
 * Estado de UI para la pantalla de eventos
 */
data class EventosUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val lastAction: String? = null,
    val eventosCount: Int = 0,
    val hasEventos: Boolean = false
)