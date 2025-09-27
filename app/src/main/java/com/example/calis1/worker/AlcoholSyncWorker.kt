package com.example.calis1.worker

import android.content.Context
import androidx.work.*
import com.example.calis1.data.database.AppDatabase
import com.example.calis1.data.entity.AlcoholRecord
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AlcoholSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = AppDatabase.getDatabase(context)
    private val alcoholRecordDao = database.alcoholRecordDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val alcoholRecordsCollection = firestore.collection("alcohol_records")

    override suspend fun doWork(): Result {
        return try {
            // Intentar sincronización
            syncAlcoholData()

            // Programar próxima sincronización
            scheduleNextSync()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()

            // Si falló, programar reintento
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private suspend fun syncAlcoholData() {
        // 1. Obtener datos de Firebase
        val snapshot = alcoholRecordsCollection.get().await()
        val firebaseRegistros = mutableMapOf<String, AlcoholRecord>()

        // Procesar datos de Firebase
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
                firebaseRegistros[registro.id] = registro
            }
        }

        // 2. Obtener datos locales (necesitamos implementar un método que obtenga todos los registros)
        // Por ahora, sincronizaremos por usuario específico si tenemos la información

        // 3. Procesar sincronización bidireccional
        syncFirebaseToLocal(firebaseRegistros)
    }

    private suspend fun syncFirebaseToLocal(firebaseRegistros: Map<String, AlcoholRecord>) {
        // Insertar/actualizar registros de Firebase en local
        for ((id, registro) in firebaseRegistros) {
            try {
                // Verificar si el registro existe localmente
                val registroLocal = alcoholRecordDao.getRegistroById(id)

                // Solo insertar si no existe localmente o si ha cambiado
                if (registroLocal == null || registroLocal != registro) {
                    alcoholRecordDao.insertRegistro(registro)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Continuar con el siguiente registro si uno falla
            }
        }
    }

    private fun scheduleNextSync() {
        // Programar siguiente sincronización en 15 minutos
        val syncRequest = OneTimeWorkRequestBuilder<AlcoholSyncWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "sync_alcohol_data",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
    }

    companion object {
        // Método para iniciar sincronización inmediata
        fun syncNow(context: Context) {
            val syncRequest = OneTimeWorkRequestBuilder<AlcoholSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "sync_alcohol_immediate",
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
        }

        // Método para configurar sincronización periódica
        fun setupPeriodicSync(context: Context) {
            val periodicSyncRequest = PeriodicWorkRequestBuilder<AlcoholSyncWorker>(
                15, TimeUnit.MINUTES, // Repetir cada 15 minutos
                5, TimeUnit.MINUTES   // Ventana de flexibilidad de 5 minutos
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true) // Solo cuando la batería no esté baja
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "periodic_alcohol_sync",
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicSyncRequest
                )
        }

        // Método para detener todas las sincronizaciones
        fun stopAllSync(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag("alcohol_sync")
        }
    }
}