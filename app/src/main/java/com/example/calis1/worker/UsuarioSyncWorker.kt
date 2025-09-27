package com.example.calis1.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.calis1.data.database.AppDatabase
import com.example.calis1.data.entity.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import kotlin.collections.iterator

class UsuarioSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val database = AppDatabase.Companion.getDatabase(context)
    private val usuarioDao = database.usuarioDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val usuariosCollection = firestore.collection("usuarios")

    override suspend fun doWork(): Result {
        return try {
            // Intentar sincronización
            syncData()

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

    private suspend fun syncData() {
        // 1. Obtener datos de Firebase
        val snapshot = usuariosCollection.get().await()
        val firebaseUsuarios = mutableMapOf<String, Usuario>()

        // Procesar datos de Firebase
        for (document in snapshot.documents) {
            val data = document.data
            if (data != null) {
                val usuario = Usuario(
                    id = data["id"] as? String ?: "",
                    nombre = data["nombre"] as? String ?: "",
                    edad = (data["edad"] as? Long)?.toInt() ?: 0,
                    timestamp = data["timestamp"] as? Long ?: 0L
                )
                firebaseUsuarios[usuario.id] = usuario
            }
        }

        // 2. Obtener datos locales
        val localUsuarios = usuarioDao.getAllUsuariosSync()
        val localUsuariosMap = localUsuarios.associateBy { it.id }

        // 3. Sincronizar eliminaciones (usuarios locales que ya no están en Firebase)
        for (localUsuario in localUsuarios) {
            if (!firebaseUsuarios.containsKey(localUsuario.id)) {
                usuarioDao.deleteUsuario(localUsuario)
            }
        }

        // 4. Sincronizar inserciones/actualizaciones (usuarios de Firebase que no están localmente o han cambiado)
        for ((id, firebaseUsuario) in firebaseUsuarios) {
            val localUsuario = localUsuariosMap[id]

            // Insertar si no existe localmente o si ha cambiado
            if (localUsuario == null || localUsuario != firebaseUsuario) {
                usuarioDao.insertUsuario(firebaseUsuario)
            }
        }
    }

    private fun scheduleNextSync() {
        // Programar siguiente sincronización en 15 minutos
        val syncRequest = OneTimeWorkRequestBuilder<UsuarioSyncWorker>()
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(
                "sync_data",
                ExistingWorkPolicy.REPLACE,
                syncRequest
            )
    }

    companion object {
        // Método para iniciar sincronización inmediata
        fun syncNow(context: Context) {
            val syncRequest = OneTimeWorkRequestBuilder<UsuarioSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    "sync_immediate",
                    ExistingWorkPolicy.REPLACE,
                    syncRequest
                )
        }

        // Método para configurar sincronización periódica
        fun setupPeriodicSync(context: Context) {
            val periodicSyncRequest = PeriodicWorkRequestBuilder<UsuarioSyncWorker>(
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
                    "periodic_sync",
                    ExistingPeriodicWorkPolicy.KEEP,
                    periodicSyncRequest
                )
        }

        // Método para detener todas las sincronizaciones
        fun stopAllSync(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag("sync")
        }
    }
}