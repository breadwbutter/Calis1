package com.example.calis1.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.calis1.data.dao.UsuarioDao
import com.example.calis1.data.entity.Usuario
import com.example.calis1.worker.SyncWorker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UsuarioRepository(
    private val usuarioDao: UsuarioDao,
    private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val usuariosCollection = firestore.collection("usuarios")
    private var listenerRegistration: ListenerRegistration? = null

    // Obtener todos los usuarios de Room
    fun getAllUsuarios(): LiveData<List<Usuario>> = usuarioDao.getAllUsuarios()

    // Insertar usuario en Room y Firebase
    suspend fun insertUsuario(usuario: Usuario) {
        try {
            // Guardar en Room primero (cache local)
            usuarioDao.insertUsuario(usuario)

            // Intentar guardar en Firebase
            usuariosCollection.document(usuario.id).set(usuario.toMap()).await()

            // Programar sincronización para asegurar consistencia
            SyncWorker.syncNow(context)

        } catch (e: Exception) {
            // Si Firebase falla, programar sincronización posterior
            e.printStackTrace()
            SyncWorker.syncNow(context)
        }
    }

    // Eliminar usuario
    suspend fun deleteUsuario(usuario: Usuario) {
        try {
            // Eliminar localmente primero
            usuarioDao.deleteUsuario(usuario)

            // Intentar eliminar en Firebase
            usuariosCollection.document(usuario.id).delete().await()

            // Programar sincronización para asegurar consistencia
            SyncWorker.syncNow(context)

        } catch (e: Exception) {
            // Si Firebase falla, programar sincronización posterior
            e.printStackTrace()
            SyncWorker.syncNow(context)
        }
    }

    // Sincronización inicial manual
    suspend fun syncFromFirebase() {
        try {
            val snapshot = usuariosCollection.get().await()

            // Obtener todos los IDs de Firebase
            val firebaseIds = snapshot.documents.mapNotNull { it.id }.toSet()

            // Obtener todos los usuarios locales
            val localUsuarios = usuarioDao.getAllUsuariosSync()

            // Eliminar usuarios locales que ya no existen en Firebase
            val usuariosAEliminar = localUsuarios.filter { !firebaseIds.contains(it.id) }
            usuariosAEliminar.forEach { usuario ->
                usuarioDao.deleteUsuario(usuario)
            }

            // Agregar/actualizar usuarios de Firebase
            for (document in snapshot.documents) {
                val data = document.data
                if (data != null) {
                    val usuario = Usuario(
                        id = data["id"] as? String ?: "",
                        nombre = data["nombre"] as? String ?: "",
                        edad = (data["edad"] as? Long)?.toInt() ?: 0,
                        timestamp = data["timestamp"] as? Long ?: 0L
                    )
                    usuarioDao.insertUsuario(usuario)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Si falla, programar sincronización con WorkManager
            SyncWorker.syncNow(context)
        }
    }

    // Configurar listener en tiempo real SOLO cuando la app está activa
    suspend fun startRealtimeSync() {
        listenerRegistration = usuariosCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                // Si hay error, programar sincronización con WorkManager
                SyncWorker.syncNow(context)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Ejecutar sincronización rápida en tiempo real
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        syncFromSnapshot(snapshot)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Si falla, usar WorkManager como backup
                        SyncWorker.syncNow(context)
                    }
                }
            }
        }
    }

    // Sincronizar desde un snapshot específico (tiempo real)
    private suspend fun syncFromSnapshot(snapshot: com.google.firebase.firestore.QuerySnapshot) {
        // Obtener todos los IDs de Firebase del snapshot actual
        val firebaseIds = snapshot.documents.mapNotNull { it.id }.toSet()

        // Obtener todos los usuarios locales
        val localUsuarios = usuarioDao.getAllUsuariosSync()
        val localIds = localUsuarios.map { it.id }.toSet()

        // Eliminar usuarios locales que ya no existen en Firebase
        val usuariosAEliminar = localUsuarios.filter { !firebaseIds.contains(it.id) }
        usuariosAEliminar.forEach { usuario ->
            usuarioDao.deleteUsuario(usuario)
        }

        // Agregar/actualizar usuarios de Firebase que no están localmente o han cambiado
        for (document in snapshot.documents) {
            val data = document.data
            if (data != null) {
                val usuario = Usuario(
                    id = data["id"] as? String ?: "",
                    nombre = data["nombre"] as? String ?: "",
                    edad = (data["edad"] as? Long)?.toInt() ?: 0,
                    timestamp = data["timestamp"] as? Long ?: 0L
                )

                // Solo insertar si no existe localmente o si ha cambiado
                val usuarioLocal = localUsuarios.find { it.id == usuario.id }
                if (usuarioLocal == null || usuarioLocal != usuario) {
                    usuarioDao.insertUsuario(usuario)
                }
            }
        }
    }

    // Configurar sincronización completa (tiempo real + WorkManager)
    fun setupCompleteSync() {
        // 1. Configurar sincronización periódica en segundo plano
        SyncWorker.setupPeriodicSync(context)

        // 2. Iniciar sincronización inmediata
        SyncWorker.syncNow(context)

        // 3. Configurar listener en tiempo real para cuando la app esté activa
        CoroutineScope(Dispatchers.IO).launch {
            startRealtimeSync()
        }
    }

    // Detener solo el listener en tiempo real (WorkManager sigue funcionando)
    fun stopRealtimeSync() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    // Limpiar todos los recursos y detener toda sincronización
    fun cleanup() {
        stopRealtimeSync()
        SyncWorker.stopAllSync(context)
    }

    // Forzar sincronización manual inmediata
    fun forceSync() {
        SyncWorker.syncNow(context)
    }
}