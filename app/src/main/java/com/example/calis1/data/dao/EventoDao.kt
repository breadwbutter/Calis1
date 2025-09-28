package com.example.calis1.data.dao

import androidx.room.*
import com.example.calis1.data.entity.Evento
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoDao {

    // Flow reactivo para obtener todos los eventos de un usuario
    @Query("SELECT * FROM eventos WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllEventos(userId: String): Flow<List<Evento>>

    // Método síncrono para obtener todos los eventos (para sincronización)
    @Query("SELECT * FROM eventos WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllEventosSync(userId: String): List<Evento>

    // Obtener evento por ID
    @Query("SELECT * FROM eventos WHERE id = :id")
    suspend fun getEventoById(id: String): Evento?

    // Insertar o actualizar evento
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvento(evento: Evento)

    // Insertar múltiples eventos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventos(eventos: List<Evento>)

    // Actualizar evento
    @Update
    suspend fun updateEvento(evento: Evento)

    // Eliminar evento
    @Delete
    suspend fun deleteEvento(evento: Evento)

    // Eliminar todos los eventos de un usuario
    @Query("DELETE FROM eventos WHERE userId = :userId")
    suspend fun deleteAllEventosUsuario(userId: String)

    // Contar eventos de un usuario
    @Query("SELECT COUNT(*) FROM eventos WHERE userId = :userId")
    fun getEventosCount(userId: String): Flow<Int>

    // MEJORADO: Buscar eventos por título, descripción O fecha (búsqueda completa)
    @Query("""
        SELECT * FROM eventos 
        WHERE userId = :userId 
        AND (
            titulo LIKE '%' || :query || '%' 
            OR descripcion LIKE '%' || :query || '%' 
            OR fecha LIKE '%' || :query || '%'
        ) 
        ORDER BY timestamp DESC
    """)
    suspend fun buscarEventos(userId: String, query: String): List<Evento>

    // NUEVO: Búsqueda con Flow reactivo para la pantalla de búsqueda
    @Query("""
        SELECT * FROM eventos 
        WHERE userId = :userId 
        AND (
            titulo LIKE '%' || :query || '%' 
            OR descripcion LIKE '%' || :query || '%' 
            OR fecha LIKE '%' || :query || '%'
        ) 
        ORDER BY timestamp DESC
    """)
    fun buscarEventosFlow(userId: String, query: String): Flow<List<Evento>>

    // Obtener eventos por fecha exacta
    @Query("SELECT * FROM eventos WHERE userId = :userId AND fecha = :fecha ORDER BY timestamp DESC")
    suspend fun getEventosPorFecha(userId: String, fecha: String): List<Evento>

    // NUEVO: Búsqueda avanzada por campos específicos
    @Query("""
        SELECT * FROM eventos 
        WHERE userId = :userId 
        AND (
            (:buscarTitulo = 1 AND titulo LIKE '%' || :query || '%') 
            OR (:buscarDescripcion = 1 AND descripcion LIKE '%' || :query || '%') 
            OR (:buscarFecha = 1 AND fecha LIKE '%' || :query || '%')
        ) 
        ORDER BY timestamp DESC
    """)
    suspend fun buscarEventosAvanzado(
        userId: String,
        query: String,
        buscarTitulo: Boolean = true,
        buscarDescripcion: Boolean = true,
        buscarFecha: Boolean = true
    ): List<Evento>
}