package com.example.calis1.repository

import android.content.Context
import com.example.calis1.data.dao.UsuarioDao
import com.example.calis1.data.entity.Usuario
import com.example.calis1.worker.UsuarioSyncWorker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UsuarioRepository(
    private val usuarioDao: UsuarioDao,
    private val context: Context
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val usuariosCollection = firestore.collection("usuarios")
    private var listenerRegistration: ListenerRegistration? = null

    fun getAllUsuarios(): Flow<List<Usuario>> = usuarioDao.getAllUsuarios()

    fun getUsuariosCount(): Flow<Int> = usuarioDao.getUsuariosCount()


    suspend fun insertUsuario(usuario: Usuario) {
        try {
            usuarioDao.insertUsuario(usuario)

            usuariosCollection.document(usuario.id).set(usuario.toMap()).await()

            UsuarioSyncWorker.syncNow(context)

        } catch (e: Exception) {
            e.printStackTrace()
            UsuarioSyncWorker.syncNow(context)
        }
    }

    suspend fun deleteUsuario(usuario: Usuario) {
        try {
            usuarioDao.deleteUsuario(usuario)

            usuariosCollection.document(usuario.id).delete().await()

            UsuarioSyncWorker.syncNow(context)

        } catch (e: Exception) {
            e.printStackTrace()
            UsuarioSyncWorker.syncNow(context)
        }
    }

    suspend fun syncFromFirebase() {
        try {
            val snapshot = usuariosCollection.get().await()

            val firebaseIds = snapshot.documents.mapNotNull { it.id }.toSet()

            val localUsuarios = usuarioDao.getAllUsuariosSync()

            val usuariosAEliminar = localUsuarios.filter { !firebaseIds.contains(it.id) }
            usuariosAEliminar.forEach { usuario ->
                usuarioDao.deleteUsuario(usuario)
            }

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
            UsuarioSyncWorker.syncNow(context)
        }
    }

    suspend fun startRealtimeSync() {
        listenerRegistration = usuariosCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                UsuarioSyncWorker.syncNow(context)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        syncFromSnapshot(snapshot)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        UsuarioSyncWorker.syncNow(context)
                    }
                }
            }
        }
    }

    private suspend fun syncFromSnapshot(snapshot: com.google.firebase.firestore.QuerySnapshot) {
        // Obtener todos los IDs de Firebase del snapshot actual
        val firebaseIds = snapshot.documents.mapNotNull { it.id }.toSet()

        val localUsuarios = usuarioDao.getAllUsuariosSync()
        val localIds = localUsuarios.map { it.id }.toSet()

        val usuariosAEliminar = localUsuarios.filter { !firebaseIds.contains(it.id) }
        usuariosAEliminar.forEach { usuario ->
            usuarioDao.deleteUsuario(usuario)
        }

        for (document in snapshot.documents) {
            val data = document.data
            if (data != null) {
                val usuario = Usuario(
                    id = data["id"] as? String ?: "",
                    nombre = data["nombre"] as? String ?: "",
                    edad = (data["edad"] as? Long)?.toInt() ?: 0,
                    timestamp = data["timestamp"] as? Long ?: 0L
                )

                val usuarioLocal = localUsuarios.find { it.id == usuario.id }
                if (usuarioLocal == null || usuarioLocal != usuario) {
                    usuarioDao.insertUsuario(usuario)
                }
            }
        }
    }

    fun setupCompleteSync() {
        UsuarioSyncWorker.setupPeriodicSync(context)

        UsuarioSyncWorker.syncNow(context)

        CoroutineScope(Dispatchers.IO).launch {
            startRealtimeSync()
        }
    }

    fun stopRealtimeSync() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    fun cleanup() {
        stopRealtimeSync()
        UsuarioSyncWorker.stopAllSync(context)
    }

    fun forceSync() {
        UsuarioSyncWorker.syncNow(context)
    }
}