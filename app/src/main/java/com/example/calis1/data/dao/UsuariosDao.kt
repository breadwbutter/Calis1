package com.example.calis1.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.calis1.data.entity.Usuario
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    // StateFlow/Flow para datos reactivos
    @Query("SELECT * FROM usuarios ORDER BY timestamp DESC")
    fun getAllUsuarios(): Flow<List<Usuario>>

    // Método síncrono para obtener todos los usuarios (para sincronización)
    @Query("SELECT * FROM usuarios ORDER BY timestamp DESC")
    suspend fun getAllUsuariosSync(): List<Usuario>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsuario(usuario: Usuario)

    @Delete
    suspend fun deleteUsuario(usuario: Usuario)

    @Query("DELETE FROM usuarios")
    suspend fun deleteAllUsuarios()

    @Query("SELECT * FROM usuarios WHERE id = :id")
    suspend fun getUsuarioById(id: String): Usuario?

    // Método adicional para contar usuarios (útil para StateFlow)
    @Query("SELECT COUNT(*) FROM usuarios")
    fun getUsuariosCount(): Flow<Int>
}