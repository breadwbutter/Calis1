package com.example.calis1.repository

import com.example.calis1.data.dao.NotaDao
import com.example.calis1.data.entity.Nota
import kotlinx.coroutines.flow.Flow

class NotasRepository(private val notaDao: NotaDao) {
    fun getAllNotas(): Flow<List<Nota>> = notaDao.getAllNotas()

    suspend fun insertNota(nota: Nota) {
        notaDao.insertNota(nota)
    }

    suspend fun updateNota(nota: Nota) {
        notaDao.updateNota(nota)
    }

    suspend fun deleteNota(nota: Nota) {
        notaDao.deleteNota(nota)
    }
}