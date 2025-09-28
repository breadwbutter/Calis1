package com.example.calis1.repository

import android.content.Context
import com.example.calis1.data.dao.EventoDao
import com.example.calis1.data.entity.Evento
import com.example.calis1.worker.EventoSyncWorker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EventosRepository(
    private val eventoDao: EventoDao,
    private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val eventosCollection = firestore.collection("eventos")
    private var listenerRegistration: ListenerRegistration? = null

    /**
     * Flow reactivo para obtener todos los eventos de un usuario
     */
    fun getAllEventos(userId: String): Flow<List<Evento>> {
        return eventoDao.getAllEventos(userId)
    }

    /**
     * Flow para contar eventos
     */
    fun getEventosCount(userId: String): Flow<Int> {
        return eventoDao.getEventosCount(userId)
    }

    /**
     * Obtener evento por ID
     */
    suspend fun getEventoById(eventoId: String): Evento? {
        return eventoDao.getEventoById(eventoId)
    }

    /**
     * Insertar evento en Room y Firebase
     */
    suspend fun insertEvento(evento: Evento) {
        try {
            // Guardar en Room primero (cache local)
            eventoDao.insertEvento(evento)

            // Intentar guardar en Firebase
            eventosCollection.document(evento.id).set(evento.toMap()).await()

            // Programar sincronización para asegurar consistencia
            EventoSyncWorker.syncNow(context)

        } catch (e: Exception) {
            // Si Firebase falla, programar sincronización posterior
            e.printStackTrace()
            EventoSyncWorker.syncNow(context)
        }
    }

    /**
     * Actualizar evento existente
     */
    suspend fun updateEvento(evento: Evento) {
        try {
            // Actualizar en Room primero
            eventoDao.updateEvento(evento)

            // Intentar actualizar en Firebase
            eventosCollection.document(evento.id).set(evento.toMap()).await()

            // Programar sincronización para asegurar consistencia
            EventoSyncWorker.syncNow(context)

        } catch (e: Exception) {
            // Si Firebase falla, programar sincronización posterior
            e.printStackTrace()
            EventoSyncWorker.syncNow(context)
        }
    }

    /**
     * Eliminar evento
     */
    suspend fun deleteEvento(evento: Evento) {
        try {
            // Eliminar localmente primero
            eventoDao.deleteEvento(evento)

            // Intentar eliminar en Firebase
            eventosCollection.document(evento.id).delete().await()

            // Programar sincronización para asegurar consistencia
            EventoSyncWorker.syncNow(context)

        } catch (e: Exception) {
            // Si Firebase falla, programar sincronización posterior
            e.printStackTrace()
            EventoSyncWorker.syncNow(context)
        }
    }

    /**
     * Sincronización inicial manual desde Firebase
     */
    suspend fun syncFromFirebase(userId: String) {
        try {
            val snapshot = eventosCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            // Obtener todos los IDs de Firebase para este usuario
            val firebaseIds = snapshot.documents.mapNotNull { it.id }.toSet()

            // Obtener todos los eventos locales del usuario
            val localEventos = eventoDao.getAllEventosSync(userId)

            // Eliminar eventos locales que ya no existen en Firebase
            val eventosAEliminar = localEventos.filter { !firebaseIds.contains(it.id) }
            eventosAEliminar.forEach { evento ->
                eventoDao.deleteEvento(evento)
            }

            // Agregar/actualizar eventos de Firebase
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
                    eventoDao.insertEvento(evento)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Si falla, programar sincronización con WorkManager
            EventoSyncWorker.syncNow(context)
        }
    }

    /**
     * Configurar listener en tiempo real para un usuario específico
     */
    suspend fun startRealtimeSync(userId: String) {
        listenerRegistration = eventosCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
                    // Si hay error, programar sincronización con WorkManager
                    EventoSyncWorker.syncNow(context)
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
                            EventoSyncWorker.syncNow(context)
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

        // Obtener todos los eventos locales del usuario
        val localEventos = eventoDao.getAllEventosSync(userId)
        val localIds = localEventos.map { it.id }.toSet()

        // Eliminar eventos locales que ya no existen en Firebase
        val eventosAEliminar = localEventos.filter { !firebaseIds.contains(it.id) }
        eventosAEliminar.forEach { evento ->
            eventoDao.deleteEvento(evento)
        }

        // Agregar/actualizar eventos de Firebase que no están localmente o han cambiado
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

                // Solo insertar si no existe localmente o si ha cambiado
                val eventoLocal = localEventos.find { it.id == evento.id }
                if (eventoLocal == null || eventoLocal != evento) {
                    eventoDao.insertEvento(evento)
                }
            }
        }
    }

    /**
     * Configurar sincronización completa (tiempo real + WorkManager)
     */
    fun setupCompleteSync(userId: String) {
        // 1. Configurar sincronización periódica en segundo plano
        EventoSyncWorker.setupPeriodicSync(context)

        // 2. Iniciar sincronización inmediata
        EventoSyncWorker.syncNow(context)

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
        EventoSyncWorker.stopAllSync(context)
    }

    /**
     * Forzar sincronización manual inmediata
     */
    fun forceSync() {
        EventoSyncWorker.syncNow(context)
    }
}