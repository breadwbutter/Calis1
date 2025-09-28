package com.example.calis1.data.dao

import androidx.room.*
import com.example.calis1.data.entity.Nota
import kotlinx.coroutines.flow.Flow

@Dao
interface NotaDao {
    @Query("SELECT * FROM notas ORDER BY timestamp DESC")
    fun getAllNotas(): Flow<List<Nota>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNota(nota: Nota)

    @Update
    suspend fun updateNota(nota: Nota)

    @Delete
    suspend fun deleteNota(nota: Nota)
}