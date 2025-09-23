package com.example.calis1.data.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val nombre: String = "",
    val edad: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    // Constructor sin argumentos para Firebase
    constructor() : this("", "", 0, 0L)

    // Convertir a Map para Firebase
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "nombre" to nombre,
            "edad" to edad,
            "timestamp" to timestamp
        )
    }
}