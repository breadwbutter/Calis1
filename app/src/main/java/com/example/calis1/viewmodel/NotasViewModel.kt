package com.example.calis1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calis1.data.database.AppDatabase
import com.example.calis1.data.entity.Nota
import com.example.calis1.repository.NotasRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotasViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: NotasRepository
    val allNotas: StateFlow<List<Nota>>

    init {
        val notaDao = AppDatabase.getDatabase(application).notaDao()
        repository = NotasRepository(notaDao)
        allNotas = repository.getAllNotas().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun addOrUpdateNota(id: String?, title: String, content: String) {
        viewModelScope.launch {
            if (title.isBlank() || content.isBlank()) return@launch

            if (id == null) {
                // Agregar nueva nota
                val nuevaNota = Nota(title = title.trim(), content = content.trim())
                repository.insertNota(nuevaNota)
            } else {
                // Actualizar nota existente
                val notaActualizada = Nota(id = id, title = title.trim(), content = content.trim())
                repository.updateNota(notaActualizada)
            }
        }
    }

    fun deleteNota(nota: Nota) {
        viewModelScope.launch {
            repository.deleteNota(nota)
        }
    }
}