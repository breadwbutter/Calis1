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
        repository = UsuarioRepository(usuarioDao, application.applicationContext)
        allUsuarios = repository.getAllUsuarios()

        // Configurar sincronización completa (tiempo real + WorkManager)
        setupCompleteSync()
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

    // Configurar sincronización completa
    private fun setupCompleteSync() {
        repository.setupCompleteSync()
    }

    // Método para sincronización manual
    fun manualSync() = viewModelScope.launch {
        repository.syncFromFirebase()
    }

    // Método para forzar sincronización inmediata con WorkManager
    fun forceSync() {
        repository.forceSync()
    }

    // Limpiar recursos cuando el ViewModel se destruya
    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}