package com.example.calis1.repository

import androidx.lifecycle.LiveData
import com.example.calis1.data.dao.UsuarioDao
import com.example.calis1.data.entity.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UsuarioRepository(private val usuarioDao: UsuarioDao) {
    private val firestore = FirebaseFirestore.getInstance()
    private val usuariosCollection = firestore.collection("usuarios")
    private var listenerRegistration: ListenerRegistration? = null

    // Obtener todos los usuarios de Room
    fun getAllUsuarios(): LiveData<List<Usuario>> = usuarioDao.getAllUsuarios()

    // Insertar usuario en Room y Firebase
    suspend fun insertUsuario(usuario: Usuario) {
        // Guardar en Room
        usuarioDao.insertUsuario(usuario)

        // Guardar en Firebase
        try {
            usuariosCollection.document(usuario.id).set(usuario.toMap()).await()
        } catch (e: Exception) {
            // Manejo de errores de Firebase
            e.printStackTrace()
        }
    }

    // Eliminar usuario
    suspend fun deleteUsuario(usuario: Usuario) {
        usuarioDao.deleteUsuario(usuario)
        try {
            usuariosCollection.document(usuario.id).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Sincronización inicial de Firebase a Room
    suspend fun syncFromFirebase() {
        try {
            val snapshot = usuariosCollection.get().await()

            // Obtener todos los IDs de Firebase
            val firebaseIds = snapshot.documents.mapNotNull { it.id }.toSet()

            // Obtener todos los usuarios locales
            val localUsuarios = usuarioDao.getAllUsuariosSync() // Necesitamos crear este método
            val localIds = localUsuarios.map { it.id }.toSet()

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
        }
    }

    // Configurar listener en tiempo real para sincronización automática
    suspend fun startRealtimeSync() {
        listenerRegistration = usuariosCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }

            if (snapshot != null) {
                // Ejecutar sincronización en segundo plano
                kotlinx.coroutines.GlobalScope.launch {
                    try {
                        syncFromSnapshot(snapshot)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // Sincronizar desde un snapshot específico
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

    // Detener el listener cuando ya no se necesite
    fun stopRealtimeSync() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    // Limpiar recursos
    fun cleanup() {
        stopRealtimeSync()
    }
}