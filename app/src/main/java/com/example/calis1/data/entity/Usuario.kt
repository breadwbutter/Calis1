package com.example.calis1.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "usuarios")
data class Usuario(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    val edad: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", 0, 0L)

    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "nombre" to nombre,
            "edad" to edad,
            "timestamp" to timestamp
        )
    }
}