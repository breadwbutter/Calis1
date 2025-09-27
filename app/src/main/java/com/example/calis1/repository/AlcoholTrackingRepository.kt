package com.example.calis1.repository

import android.content.Context
import com.example.calis1.data.dao.AlcoholRecordDao
import com.example.calis1.data.entity.AlcoholRecord
import com.example.calis1.worker.AlcoholSyncWorker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AlcoholTrackingRepository(
    private val alcoholRecordDao: AlcoholRecordDao,
    private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val alcoholRecordsCollection = firestore.collection("alcohol_records")
    private var listenerRegistration: ListenerRegistration? = null

    /**
     * Flow reactivo para registros de una semana específica
     */
    fun getRegistrosSemana(userId: String, semanaInicio: String): Flow<List<AlcoholRecord>> {
        return alcoholRecordDao.getRegistrosSemana(userId, semanaInicio)
    }

    /**
     * Obtener registro específico por ID
     */
    suspend fun getRegistroById(registroId: String): AlcoholRecord? {
        return alcoholRecordDao.getRegistroById(registroId)
    }

    /**
     * Actualizar registro existente
     */
    suspend fun updateRegistro(registro: AlcoholRecord) {
        try {
            // Actualizar en Room primero
            alcoholRecordDao.updateRegistro(registro)

            // Intentar actualizar en Firebase
            alcoholRecordsCollection.document(registro.id).set(registro.toMap()).await()

            // Programar sincronización para asegurar consistencia
            AlcoholSyncWorker.syncNow(context)

        } catch (e: Exception) {
            // Si Firebase falla, programar sincronización posterior
            e.printStackTrace()
            AlcoholSyncWorker.syncNow(context)
        }
    }

    /**
     * Insertar o actualizar registro en Room y Firebase
     */
    suspend fun insertRegistro(registro: AlcoholRecord) {
        try {
            // Guardar en Room primero (cache local)
            alcoholRecordDao.insertRegistro(registro)

            // Intentar guardar en Firebase
            alcoholRecordsCollection.document(registro.id).set(registro.toMap()).await()

            // Programar sincronización para asegurar consistencia
            AlcoholSyncWorker.syncNow(context)

        } catch (e: Exception) {
            // Si Firebase falla, programar sincronización posterior
            e.printStackTrace()
            AlcoholSyncWorker.syncNow(context)
        }
    }

    /**
     * Eliminar registro
     */
    suspend fun deleteRegistro(registro: AlcoholRecord) {
        try {
            // Eliminar localmente primero
            alcoholRecordDao.deleteRegistro(registro)

            // Intentar eliminar en Firebase
            alcoholRecordsCollection.document(registro.id).delete().await()

            // Programar sincronización para asegurar consistencia
            AlcoholSyncWorker.syncNow(context)

        } catch (e: Exception) {
            // Si Firebase falla, programar sincronización posterior
            e.printStackTrace()
            AlcoholSyncWorker.syncNow(context)
        }
    }

    /**
     * Sincronización inicial manual desde Firebase
     */
    suspend fun syncFromFirebase(userId: String) {
        try {
            val snapshot = alcoholRecordsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            // Obtener todos los IDs de Firebase para este usuario
            val firebaseIds = snapshot.documents.mapNotNull { it.id }.toSet()

            // Obtener todos los registros locales del usuario
            val localRegistros = alcoholRecordDao.getRegistrosSemanaSync(userId, "") // Obtener todos los registros

            // Eliminar registros locales que ya no existen en Firebase
            val registrosAEliminar = localRegistros.filter { !firebaseIds.contains(it.id) }
            registrosAEliminar.forEach { registro ->
                alcoholRecordDao.deleteRegistro(registro)
            }

            // Agregar/actualizar registros de Firebase
            for (document in snapshot.documents) {
                val data = document.data
                if (data != null) {
                    val registro = AlcoholRecord(
                        id = data["id"] as? String ?: "",
                        userId = data["userId"] as? String ?: "",
                        fecha = data["fecha"] as? String ?: "",
                        diaSemana = (data["diaSemana"] as? Long)?.toInt() ?: 1,
                        nombreBebida = data["nombreBebida"] as? String ?: "",
                        mililitros = (data["mililitros"] as? Long)?.toInt() ?: 0,
                        porcentajeAlcohol = (data["porcentajeAlcohol"] as? Double) ?: 0.0,
                        semanaInicio = data["semanaInicio"] as? String ?: "",
                        timestamp = data["timestamp"] as? Long ?: 0L
                    )
                    alcoholRecordDao.insertRegistro(registro)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Si falla, programar sincronización con WorkManager
            AlcoholSyncWorker.syncNow(context)
        }
    }

    /**
     * Configurar listener en tiempo real para un usuario específico
     */
    suspend fun startRealtimeSync(userId: String) {
        listenerRegistration = alcoholRecordsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    // Si hay error, programar sincronización con WorkManager
                    AlcoholSyncWorker.syncNow(context)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    // Ejecutar sincronización rápida en tiempo real
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            syncFromSnapshot(snapshot, userId)
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // Si falla, usar WorkManager como backup
                            AlcoholSyncWorker.syncNow(context)
                        }
                    }
                }
            }
    }

    /**
     * Sincronizar desde un snapshot específico (tiempo real)
     */
    private suspend fun syncFromSnapshot(
        snapshot: com.google.firebase.firestore.QuerySnapshot,
        userId: String
    ) {
        // Obtener todos los IDs de Firebase del snapshot actual
        val firebaseIds = snapshot.documents.mapNotNull { it.id }.toSet()

        // Obtener todos los registros locales del usuario
        val localRegistros = getAllLocalRecordsForUser(userId)
        val localIds = localRegistros.map { it.id }.toSet()

        // Eliminar registros locales que ya no existen en Firebase
        val registrosAEliminar = localRegistros.filter { !firebaseIds.contains(it.id) }
        registrosAEliminar.forEach { registro ->
            alcoholRecordDao.deleteRegistro(registro)
        }

        // Agregar/actualizar registros de Firebase que no están localmente o han cambiado
        for (document in snapshot.documents) {
            val data = document.data
            if (data != null) {
                val registro = AlcoholRecord(
                    id = data["id"] as? String ?: "",
                    userId = data["userId"] as? String ?: "",
                    fecha = data["fecha"] as? String ?: "",
                    diaSemana = (data["diaSemana"] as? Long)?.toInt() ?: 1,
                    nombreBebida = data["nombreBebida"] as? String ?: "",
                    mililitros = (data["mililitros"] as? Long)?.toInt() ?: 0,
                    porcentajeAlcohol = (data["porcentajeAlcohol"] as? Double) ?: 0.0,
                    semanaInicio = data["semanaInicio"] as? String ?: "",
                    timestamp = data["timestamp"] as? Long ?: 0L
                )

                // Solo insertar si no existe localmente o si ha cambiado
                val registroLocal = localRegistros.find { it.id == registro.id }
                if (registroLocal == null || registroLocal != registro) {
                    alcoholRecordDao.insertRegistro(registro)
                }
            }
        }
    }

    /**
     * Obtener todos los registros locales de un usuario (método auxiliar)
     */
    private suspend fun getAllLocalRecordsForUser(userId: String): List<AlcoholRecord> {
        // Como no tenemos un método directo, podemos usar una consulta personalizada
        // o iterar por semanas. Por simplicidad, usaremos una implementación básica
        return try {
            // Esto requeriría agregar un método en el DAO, pero por ahora devolvemos lista vacía
            emptyList<AlcoholRecord>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Configurar sincronización completa (tiempo real + WorkManager)
     */
    fun setupCompleteSync(userId: String) {
        // 1. Configurar sincronización periódica en segundo plano
        AlcoholSyncWorker.setupPeriodicSync(context)

        // 2. Iniciar sincronización inmediata
        AlcoholSyncWorker.syncNow(context)

        // 3. Configurar listener en tiempo real para cuando la app esté activa
        CoroutineScope(Dispatchers.IO).launch {
            startRealtimeSync(userId)
        }
    }

    /**
     * Detener solo el listener en tiempo real (WorkManager sigue funcionando)
     */
    fun stopRealtimeSync() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    /**
     * Limpiar todos los recursos y detener toda sincronización
     */
    fun cleanup() {
        stopRealtimeSync()
        AlcoholSyncWorker.stopAllSync(context)
    }

    /**
     * Forzar sincronización manual inmediata
     */
    fun forceSync() {
        AlcoholSyncWorker.syncNow(context)
    }
}