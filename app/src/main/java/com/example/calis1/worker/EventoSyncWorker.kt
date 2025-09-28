package com.example.calis1.worker

import android.content.Context
import androidx.work.*
import com.example.calis1.data.database.AppDatabase
import com.example.calis1.data.entity.Evento
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class EventoSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = AppDatabase.getDatabase(context)
    private val eventoDao = database.eventoDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val eventosCollection = firestore.collection("eventos")

    override suspend fun doWork(): Result {
        return try {
            // Intentar sincronización
            syncEventosData()

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

    private suspend fun syncEventosData() {
        // 1. Obtener datos de Firebase
        val snapshot = eventosCollection.get().await()
        val firebaseEventos = mutableMapOf<String, Evento>()

        // Procesar datos de Firebase
        for (document in snapshot.documents) {
            val data = document.data
            if (data != null) {
                val evento = Evento(
                    id = data["id"] as? String ?: "",
                    userId = data["userId"] as? String ?: "",
                    titulo = data["titulo"] as? String ?: "",
                    descripcion = data["descripcion"] as? String ?: "",
                    fecha = data["fecha"] as? String ?: "",
                    timestamp = data["timestamp"] as? Long ?: 0L
                )
                firebaseEventos[evento.id] = evento
            }
        }

        // 2. Procesar sincronización bidireccional
        syncFirebaseToLocal(firebaseEventos)
    }

    private suspend fun syncFirebaseToLocal(firebaseEventos: Map<String, Evento>) {
        // Insertar/actualizar eventos de Firebase en local
        for ((id, evento) in firebaseEventos) {
            try {
                // Verificar si el evento existe localmente
                val eventoLocal = eventoDao.getEventoById(id)

                // Solo insertar si no existe localmente o si ha cambiado
                if (eventoLocal == null || eventoLocal != evento) {
                    eventoDao.insertEvento(evento)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Continuar con el siguiente evento si uno falla
            }
        }
    }

    private fun scheduleNextSync() {
        // Programar siguiente sincronización en 15 minutos
        val syncRequest = OneTimeWorkRequestBuilder<EventoSyncWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "sync_eventos_data",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
    }

    companion object {
        // Método para iniciar sincronización inmediata
        fun syncNow(context: Context) {
            val syncRequest = OneTimeWorkRequestBuilder<EventoSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "sync_eventos_immediate",
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
        }

        // Método para configurar sincronización periódica
        fun setupPeriodicSync(context: Context) {
            val periodicSyncRequest = PeriodicWorkRequestBuilder<EventoSyncWorker>(
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
                    "periodic_eventos_sync",
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicSyncRequest
                )
        }

        // Método para detener todas las sincronizaciones
        fun stopAllSync(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag("eventos_sync")
        }
    }
}