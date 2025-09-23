package com.example.calis1.repository

import androidx.lifecycle.LiveData
import com.example.calis1.data.dao.UsuarioDao
import com.example.calis1.data.entity.Usuario
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UsuarioRepository(private val usuarioDao: UsuarioDao) {
    private val firestore = FirebaseFirestore.getInstance()
    private val usuariosCollection = firestore.collection("usuarios")

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

    // Sincronizar datos de Firebase a Room
    suspend fun syncFromFirebase() {
        try {
            val snapshot = usuariosCollection.get().await()
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

    // Eliminar usuario
    suspend fun deleteUsuario(usuario: Usuario) {
        usuarioDao.deleteUsuario(usuario)
        try {
            usuariosCollection.document(usuario.id).delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}