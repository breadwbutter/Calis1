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


    fun getRegistrosSemana(userId: String, semanaInicio: String): Flow<List<AlcoholRecord>> {
        return alcoholRecordDao.getRegistrosSemana(userId, semanaInicio)
    }


    fun getAllAlcoholRecords(userId: String): Flow<List<AlcoholRecord>> {
        return alcoholRecordDao.getAllAlcoholRecords(userId)
    }


    suspend fun getRegistroById(registroId: String): AlcoholRecord? {
        return alcoholRecordDao.getRegistroById(registroId)
    }


    suspend fun updateRegistro(registro: AlcoholRecord) {
        try {
            alcoholRecordDao.updateRegistro(registro)

            alcoholRecordsCollection.document(registro.id).set(registro.toMap()).await()

            AlcoholSyncWorker.syncNow(context)

        } catch (e: Exception) {
            e.printStackTrace()
            AlcoholSyncWorker.syncNow(context)
        }
    }

    suspend fun insertRegistro(registro: AlcoholRecord) {
        try {
            alcoholRecordDao.insertRegistro(registro)

            alcoholRecordsCollection.document(registro.id).set(registro.toMap()).await()

            AlcoholSyncWorker.syncNow(context)

        } catch (e: Exception) {
            e.printStackTrace()
            AlcoholSyncWorker.syncNow(context)
        }
    }

    suspend fun deleteRegistro(registro: AlcoholRecord) {
        try {
            alcoholRecordDao.deleteRegistro(registro)

            alcoholRecordsCollection.document(registro.id).delete().await()

            AlcoholSyncWorker.syncNow(context)

        } catch (e: Exception) {
            e.printStackTrace()
            AlcoholSyncWorker.syncNow(context)
        }
    }


    suspend fun syncFromFirebase(userId: String) {
        try {
            val snapshot = alcoholRecordsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val firebaseIds = snapshot.documents.mapNotNull { it.id }.toSet()

            val localRegistros = alcoholRecordDao.getRegistrosSemanaSync(userId, "") // Obtener todos los registros

            val registrosAEliminar = localRegistros.filter { !firebaseIds.contains(it.id) }
            registrosAEliminar.forEach { registro ->
                alcoholRecordDao.deleteRegistro(registro)
            }

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


    suspend fun startRealtimeSync(userId: String) {
        listenerRegistration = alcoholRecordsCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()

                    AlcoholSyncWorker.syncNow(context)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
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


    private suspend fun syncFromSnapshot(
        snapshot: com.google.firebase.firestore.QuerySnapshot,
        userId: String
    ) {
        val firebaseIds = snapshot.documents.mapNotNull { it.id }.toSet()

        val localRegistros = getAllLocalRecordsForUser(userId)
        val localIds = localRegistros.map { it.id }.toSet()

        val registrosAEliminar = localRegistros.filter { !firebaseIds.contains(it.id) }
        registrosAEliminar.forEach { registro ->
            alcoholRecordDao.deleteRegistro(registro)
        }

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

                val registroLocal = localRegistros.find { it.id == registro.id }
                if (registroLocal == null || registroLocal != registro) {
                    alcoholRecordDao.insertRegistro(registro)
                }
            }
        }
    }

    private suspend fun getAllLocalRecordsForUser(userId: String): List<AlcoholRecord> {
        return try {
            emptyList<AlcoholRecord>()
        } catch (e: Exception) {
            emptyList()
        }
    }

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


    fun stopRealtimeSync() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }


    fun cleanup() {
        stopRealtimeSync()
        AlcoholSyncWorker.stopAllSync(context)
    }


    fun forceSync() {
        AlcoholSyncWorker.syncNow(context)
    }
}