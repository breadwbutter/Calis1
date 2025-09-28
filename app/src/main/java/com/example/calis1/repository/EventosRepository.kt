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


    fun getAllEventos(userId: String): Flow<List<Evento>> {
        return eventoDao.getAllEventos(userId)
    }


    fun getEventosCount(userId: String): Flow<Int> {
        return eventoDao.getEventosCount(userId)
    }


    suspend fun buscarEventos(userId: String, query: String): List<Evento> {
        return if (query.isBlank()) {
            emptyList()
        } else {
            eventoDao.buscarEventos(userId, query.trim())
        }
    }


    fun buscarEventosFlow(userId: String, query: String): Flow<List<Evento>> {
        return eventoDao.buscarEventosFlow(userId, query.trim())
    }


    suspend fun buscarEventosAvanzado(
        userId: String,
        query: String,
        buscarTitulo: Boolean = true,
        buscarDescripcion: Boolean = true,
        buscarFecha: Boolean = true
    ): List<Evento> {
        return if (query.isBlank()) {
            emptyList()
        } else {
            eventoDao.buscarEventosAvanzado(
                userId = userId,
                query = query.trim(),
                buscarTitulo = buscarTitulo,
                buscarDescripcion = buscarDescripcion,
                buscarFecha = buscarFecha
            )
        }
    }


    suspend fun getEventoById(eventoId: String): Evento? {
        return eventoDao.getEventoById(eventoId)
    }


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
            eventoDao.updateEvento(evento)

            eventosCollection.document(evento.id).set(evento.toMap()).await()

            EventoSyncWorker.syncNow(context)

        } catch (e: Exception) {
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


    suspend fun syncFromFirebase(userId: String) {
        try {
            val snapshot = eventosCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val firebaseIds = snapshot.documents.mapNotNull { it.id }.toSet()

            val localEventos = eventoDao.getAllEventosSync(userId)

            val eventosAEliminar = localEventos.filter { !firebaseIds.contains(it.id) }
            eventosAEliminar.forEach { evento ->
                eventoDao.deleteEvento(evento)
            }

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


    suspend fun startRealtimeSync(userId: String) {
        listenerRegistration = eventosCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    error.printStackTrace()
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

    private suspend fun syncFromSnapshot(
        snapshot: com.google.firebase.firestore.QuerySnapshot,
        userId: String
    ) {
        val firebaseIds = snapshot.documents.mapNotNull { it.id }.toSet()

        val localEventos = eventoDao.getAllEventosSync(userId)
        val localIds = localEventos.map { it.id }.toSet()

        val eventosAEliminar = localEventos.filter { !firebaseIds.contains(it.id) }
        eventosAEliminar.forEach { evento ->
            eventoDao.deleteEvento(evento)
        }

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