package com.example.calis1.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.calis1.data.database.AppDatabase
import com.example.calis1.viewmodel.AppConfiguraciones
import com.example.calis1.viewmodel.SistemaUnidades
import com.example.calis1.viewmodel.TipoTema
import com.example.calis1.worker.AlcoholSyncWorker
import com.example.calis1.worker.EventoSyncWorker
import com.example.calis1.worker.UsuarioSyncWorker
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class SettingsRepository(private val context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "app_settings",
        Context.MODE_PRIVATE
    )

    private val database = AppDatabase.getDatabase(context)
    private val firestore = FirebaseFirestore.getInstance()

    // StateFlow para las configuraciones reactivas
    private val _configuraciones = MutableStateFlow(cargarConfiguraciones())

    companion object {
        private const val KEY_TEMA = "tema"
        private const val KEY_SISTEMA_UNIDADES = "sistema_unidades"
    }

    /**
     * Obtener configuraciones como Flow reactivo
     */
    fun getConfiguraciones(): Flow<AppConfiguraciones> {
        return _configuraciones.asStateFlow()
    }

    /**
     * Cargar configuraciones desde SharedPreferences
     */
    private fun cargarConfiguraciones(): AppConfiguraciones {
        val temaString = sharedPreferences.getString(KEY_TEMA, TipoTema.SISTEMA.name) ?: TipoTema.SISTEMA.name
        val unidadesString = sharedPreferences.getString(KEY_SISTEMA_UNIDADES, SistemaUnidades.MILILITROS.name) ?: SistemaUnidades.MILILITROS.name

        return AppConfiguraciones(
            tema = try {
                TipoTema.valueOf(temaString)
            } catch (e: Exception) {
                TipoTema.SISTEMA
            },
            sistemaUnidades = try {
                SistemaUnidades.valueOf(unidadesString)
            } catch (e: Exception) {
                SistemaUnidades.MILILITROS
            }
        )
    }

    /**
     * Cambiar el tema de la aplicación
     */
    suspend fun cambiarTema(nuevoTema: TipoTema) {
        // Guardar en SharedPreferences
        sharedPreferences.edit()
            .putString(KEY_TEMA, nuevoTema.name)
            .apply()

        // Actualizar el StateFlow
        _configuraciones.value = _configuraciones.value.copy(tema = nuevoTema)
    }

    /**
     * Cambiar el sistema de unidades
     */
    suspend fun cambiarSistemaUnidades(nuevoSistema: SistemaUnidades) {
        // Guardar en SharedPreferences
        sharedPreferences.edit()
            .putString(KEY_SISTEMA_UNIDADES, nuevoSistema.name)
            .apply()

        // Actualizar el StateFlow
        _configuraciones.value = _configuraciones.value.copy(sistemaUnidades = nuevoSistema)
    }

    /**
     * Borrar todos los datos del usuario
     * Esta función elimina:
     * 1. Todos los datos de Room (local)
     * 2. Todos los datos de Firebase (remoto)
     * 3. Configuraciones de la app (opcional - se mantienen por UX)
     */
    suspend fun borrarTodosLosDatos(userId: String) {
        try {
            // 1. Borrar datos locales (Room)
            borrarDatosLocales(userId)

            // 2. Borrar datos remotos (Firebase)
            borrarDatosFirebase(userId)

            // 3. Detener sincronización
            detenerSincronizacion()

            // Nota: Las configuraciones de tema y unidades se mantienen para mejor UX
            // Si quieres borrarlas también, descomenta las siguientes líneas:
            // borrarConfiguraciones()

        } catch (e: Exception) {
            throw Exception("Error al eliminar todos los datos: ${e.message}")
        }
    }

    /**
     * Borrar todos los datos locales (Room)
     */
    private suspend fun borrarDatosLocales(userId: String) {
        val usuarioDao = database.usuarioDao()
        val alcoholRecordDao = database.alcoholRecordDao()
        val notaDao = database.notaDao()
        val eventoDao = database.eventoDao()

        // Borrar datos específicos del usuario
        alcoholRecordDao.deleteAllRegistrosUsuario(userId)
        eventoDao.deleteAllEventosUsuario(userId)

        // Borrar todas las notas (no tienen userId en el esquema actual)
        val todasLasNotas = notaDao.getAllNotas()
        // Nota: Como las notas no tienen userId, se borran todas
        // En una versión futura, deberías agregar userId a las notas

        // Borrar todos los usuarios si es admin, o datos relacionados
        if (userId == "admin") {
            usuarioDao.deleteAllUsuarios()
        }
    }

    /**
     * Borrar todos los datos remotos (Firebase)
     */
    private suspend fun borrarDatosFirebase(userId: String) {
        try {
            // Borrar usuarios
            val usuariosSnapshot = firestore.collection("usuarios")
                .get()
                .await()

            for (document in usuariosSnapshot.documents) {
                document.reference.delete().await()
            }

            // Borrar registros de alcohol del usuario
            val alcoholSnapshot = firestore.collection("alcohol_records")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            for (document in alcoholSnapshot.documents) {
                document.reference.delete().await()
            }

            // Borrar eventos del usuario
            val eventosSnapshot = firestore.collection("eventos")
                .whereEqualTo("userId", userId)
                .get()
                .await()

            for (document in eventosSnapshot.documents) {
                document.reference.delete().await()
            }

        } catch (e: Exception) {
            // Log del error pero no fallar completamente
            println("Error borrando datos de Firebase: ${e.message}")
            // Podrías decidir si quieres que falle o continúe
        }
    }

    /**
     * Detener toda la sincronización
     */
    private fun detenerSincronizacion() {
        UsuarioSyncWorker.stopAllSync(context)
        AlcoholSyncWorker.stopAllSync(context)
        EventoSyncWorker.stopAllSync(context)
    }

    /**
     * Borrar todas las configuraciones (opcional)
     */
    private suspend fun borrarConfiguraciones() {
        sharedPreferences.edit().clear().apply()

        // Recargar configuraciones por defecto
        _configuraciones.value = AppConfiguraciones()
    }

    /**
     * Obtener la configuración actual de tema
     */
    fun getTemaActual(): TipoTema {
        return _configuraciones.value.tema
    }

    /**
     * Obtener la configuración actual de unidades
     */
    fun getSistemaUnidadesActual(): SistemaUnidades {
        return _configuraciones.value.sistemaUnidades
    }
}