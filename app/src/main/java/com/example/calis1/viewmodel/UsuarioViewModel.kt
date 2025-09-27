package com.example.calis1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.calis1.data.database.AppDatabase
import com.example.calis1.data.entity.Usuario
import com.example.calis1.repository.UsuarioRepository
import kotlinx.coroutines.launch

class UsuarioViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UsuarioRepository
    val allUsuarios: LiveData<List<Usuario>>

    init {
        val usuarioDao = AppDatabase.getDatabase(application).usuarioDao()
        repository = UsuarioRepository(usuarioDao)
        allUsuarios = repository.getAllUsuarios()

        // Sincronizar datos al iniciar
        syncFromFirebase()

        // Iniciar sincronización en tiempo real
        startRealtimeSync()
    }

    fun insertUsuario(nombre: String, edad: Int) = viewModelScope.launch {
        val usuario = Usuario(
            nombre = nombre,
            edad = edad
        )
        repository.insertUsuario(usuario)
    }

    fun deleteUsuario(usuario: Usuario) = viewModelScope.launch {
        repository.deleteUsuario(usuario)
    }

    private fun syncFromFirebase() = viewModelScope.launch {
        repository.syncFromFirebase()
    }

    // Iniciar sincronización en tiempo real
    private fun startRealtimeSync() = viewModelScope.launch {
        repository.startRealtimeSync()
    }

    // Método para sincronización manual (opcional)
    fun manualSync() = viewModelScope.launch {
        repository.syncFromFirebase()
    }

    // Limpiar recursos cuando el ViewModel se destruya
    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}