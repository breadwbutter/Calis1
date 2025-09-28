package com.example.calis1.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "eventos")
data class Evento(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "", // Para identificar al usuario logueado
    val titulo: String = "",
    val descripcion: String = "",
    val fecha: String = "", // Formato YYYY-MM-DD o el que prefieras
    val timestamp: Long = System.currentTimeMillis()
) {
    // Constructor sin argumentos para Firebase
    constructor() : this("", "", "", "", "", 0L)

    // Convertir a Map para Firebase
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "titulo" to titulo,
            "descripcion" to descripcion,
            "fecha" to fecha,
            "timestamp" to timestamp
        )
    }
}