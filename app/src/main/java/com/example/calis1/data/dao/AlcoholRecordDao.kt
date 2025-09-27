package com.example.calis1.data.dao

import androidx.room.*
import com.example.calis1.data.entity.AlcoholRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface AlcoholRecordDao {

    // Obtener todos los registros de una semana específica para un usuario
    @Query("SELECT * FROM alcohol_records WHERE userId = :userId AND semanaInicio = :semanaInicio ORDER BY diaSemana ASC, timestamp ASC")
    fun getRegistrosSemana(userId: String, semanaInicio: String): Flow<List<AlcoholRecord>>

    // Obtener registros de una semana específica de forma síncrona (para sincronización)
    @Query("SELECT * FROM alcohol_records WHERE userId = :userId AND semanaInicio = :semanaInicio ORDER BY diaSemana ASC, timestamp ASC")
    suspend fun getRegistrosSemanaSync(userId: String, semanaInicio: String): List<AlcoholRecord>

    // NUEVO: Obtener TODOS los registros de un día específico (múltiples registros por día)
    @Query("SELECT * FROM alcohol_records WHERE userId = :userId AND semanaInicio = :semanaInicio AND diaSemana = :diaSemana ORDER BY timestamp ASC")
    suspend fun getRegistrosPorDia(userId: String, semanaInicio: String, diaSemana: Int): List<AlcoholRecord>

    // NUEVO: Obtener TODOS los registros de un día específico como Flow (reactivo)
    @Query("SELECT * FROM alcohol_records WHERE userId = :userId AND semanaInicio = :semanaInicio AND diaSemana = :diaSemana ORDER BY timestamp ASC")
    fun getRegistrosPorDiaFlow(userId: String, semanaInicio: String, diaSemana: Int): Flow<List<AlcoholRecord>>

    // Obtener un registro específico por día de la semana (DEPRECADO - mantenido por compatibilidad)
    @Query("SELECT * FROM alcohol_records WHERE userId = :userId AND semanaInicio = :semanaInicio AND diaSemana = :diaSemana LIMIT 1")
    suspend fun getRegistroPorDia(userId: String, semanaInicio: String, diaSemana: Int): AlcoholRecord?

    // Obtener registro por ID
    @Query("SELECT * FROM alcohol_records WHERE id = :id")
    suspend fun getRegistroById(id: String): AlcoholRecord?

    // Insertar o actualizar registro
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistro(registro: AlcoholRecord)

    // Insertar múltiples registros
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistros(registros: List<AlcoholRecord>)

    // Actualizar registro existente
    @Update
    suspend fun updateRegistro(registro: AlcoholRecord)

    // Eliminar registro
    @Delete
    suspend fun deleteRegistro(registro: AlcoholRecord)

    // Eliminar registros de una semana completa
    @Query("DELETE FROM alcohol_records WHERE userId = :userId AND semanaInicio = :semanaInicio")
    suspend fun deleteRegistrosSemana(userId: String, semanaInicio: String)

    // Eliminar todos los registros de un usuario
    @Query("DELETE FROM alcohol_records WHERE userId = :userId")
    suspend fun deleteAllRegistrosUsuario(userId: String)

    // Obtener todas las semanas registradas para un usuario (para historial)
    @Query("SELECT DISTINCT semanaInicio FROM alcohol_records WHERE userId = :userId ORDER BY semanaInicio DESC")
    suspend fun getSemanasConRegistros(userId: String): List<String>

    // Obtener total de alcohol puro consumido en una semana
    @Query("SELECT SUM((mililitros * porcentajeAlcohol) / 100.0) FROM alcohol_records WHERE userId = :userId AND semanaInicio = :semanaInicio")
    suspend fun getTotalAlcoholPuroSemana(userId: String, semanaInicio: String): Double?

    // Obtener conteo de registros en una semana
    @Query("SELECT COUNT(*) FROM alcohol_records WHERE userId = :userId AND semanaInicio = :semanaInicio")
    fun getConteoRegistrosSemana(userId: String, semanaInicio: String): Flow<Int>

    // Búsqueda por nombre de bebida
    @Query("SELECT * FROM alcohol_records WHERE userId = :userId AND nombreBebida LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun buscarPorNombreBebida(userId: String, query: String): List<AlcoholRecord>

    // Obtener las bebidas más consumidas (para sugerencias)
    @Query("SELECT nombreBebida, COUNT(*) as frecuencia FROM alcohol_records WHERE userId = :userId AND nombreBebida != '' GROUP BY nombreBebida ORDER BY frecuencia DESC LIMIT 5")
    suspend fun getBebidasMasConsumidas(userId: String): List<BebidasFrecuencia>

    // Obtener estadísticas semanales
    @Query("""
        SELECT 
            COUNT(*) as totalRegistros,
            SUM(mililitros) as totalMililitros,
            SUM((mililitros * porcentajeAlcohol) / 100.0) as totalAlcoholPuro,
            AVG(porcentajeAlcohol) as promedioAlcohol
        FROM alcohol_records 
        WHERE userId = :userId AND semanaInicio = :semanaInicio
    """)
    suspend fun getEstadisticasSemana(userId: String, semanaInicio: String): EstadisticasSemana?
}

// Clase de datos para bebidas más frecuentes
data class BebidasFrecuencia(
    val nombreBebida: String,
    val frecuencia: Int
)

// Clase de datos para estadísticas de semana
data class EstadisticasSemana(
    val totalRegistros: Int,
    val totalMililitros: Int,
    val totalAlcoholPuro: Double,
    val promedioAlcohol: Double
)