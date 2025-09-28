package com.example.calis1.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter

@Entity(tableName = "alcohol_records")
data class AlcoholRecord(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val fecha: String = "",
    val diaSemana: Int = 1,
    val nombreBebida: String = "",
    val mililitros: Int = 0,
    val porcentajeAlcohol: Double = 0.0,
    val semanaInicio: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    // Constructor sin argumentos para Firebase
    constructor() : this("", "", "", 1, "", 0, 0.0, "", 0L)

    // Convertir a Map para Firebase
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "userId" to userId,
            "fecha" to fecha,
            "diaSemana" to diaSemana,
            "nombreBebida" to nombreBebida,
            "mililitros" to mililitros,
            "porcentajeAlcohol" to porcentajeAlcohol,
            "semanaInicio" to semanaInicio,
            "timestamp" to timestamp
        )
    }

    // Calcular alcohol puro en mililitros
    fun calcularAlcoholPuro(): Double {
        return (mililitros * porcentajeAlcohol) / 100.0
    }

    // Obtener nombre del día en español
    fun getNombreDia(): String {
        return when (diaSemana) {
            1 -> "Domingo"
            2 -> "Lunes"
            3 -> "Martes"
            4 -> "Miércoles"
            5 -> "Jueves"
            6 -> "Viernes"
            7 -> "Sábado"
            else -> "Desconocido"
        }
    }

    companion object {
        // Obtener el domingo de la semana actual
        fun getSemanaInicio(fecha: LocalDate = LocalDate.now()): LocalDate {
            val diasHastaDomingo = fecha.dayOfWeek.value % 7
            return fecha.minusDays(diasHastaDomingo.toLong())
        }

        // Obtener el sábado de la semana actual
        fun getSemanaFin(fecha: LocalDate = LocalDate.now()): LocalDate {
            return getSemanaInicio(fecha).plusDays(6)
        }

        // Convertir DayOfWeek a nuestro sistema (1=Domingo)
        fun dayOfWeekToInt(dayOfWeek: DayOfWeek): Int {
            return when (dayOfWeek) {
                DayOfWeek.SUNDAY -> 1
                DayOfWeek.MONDAY -> 2
                DayOfWeek.TUESDAY -> 3
                DayOfWeek.WEDNESDAY -> 4
                DayOfWeek.THURSDAY -> 5
                DayOfWeek.FRIDAY -> 6
                DayOfWeek.SATURDAY -> 7
            }
        }

        // Obtener día de la semana como entero (1=Domingo)
        fun getDiaSemana(fecha: LocalDate): Int {
            return when (fecha.dayOfWeek) {
                DayOfWeek.SUNDAY -> 1
                DayOfWeek.MONDAY -> 2
                DayOfWeek.TUESDAY -> 3
                DayOfWeek.WEDNESDAY -> 4
                DayOfWeek.THURSDAY -> 5
                DayOfWeek.FRIDAY -> 6
                DayOfWeek.SATURDAY -> 7
            }
        }

        // Formatear fecha para mostrar
        fun formatearFecha(fecha: LocalDate): String {
            val formatter = DateTimeFormatter.ofPattern("dd/MM")
            return fecha.format(formatter)
        }

        // Formatear rango de semana
        fun formatearRangoSemana(inicio: LocalDate, fin: LocalDate): String {
            val mesInicio = when (inicio.monthValue) {
                1 -> "Enero"
                2 -> "Febrero"
                3 -> "Marzo"
                4 -> "Abril"
                5 -> "Mayo"
                6 -> "Junio"
                7 -> "Julio"
                8 -> "Agosto"
                9 -> "Septiembre"
                10 -> "Octubre"
                11 -> "Noviembre"
                12 -> "Diciembre"
                else -> ""
            }

            val mesFin = when (fin.monthValue) {
                1 -> "Enero"
                2 -> "Febrero"
                3 -> "Marzo"
                4 -> "Abril"
                5 -> "Mayo"
                6 -> "Junio"
                7 -> "Julio"
                8 -> "Agosto"
                9 -> "Septiembre"
                10 -> "Octubre"
                11 -> "Noviembre"
                12 -> "Diciembre"
                else -> ""
            }

            return if (inicio.month == fin.month && inicio.year == fin.year) {
                "${inicio.dayOfMonth} - ${fin.dayOfMonth} de $mesInicio ${inicio.year}"
            } else if (inicio.year == fin.year) {
                "${inicio.dayOfMonth} $mesInicio - ${fin.dayOfMonth} $mesFin ${inicio.year}"
            } else {
                "${inicio.dayOfMonth} $mesInicio ${inicio.year} - ${fin.dayOfMonth} $mesFin ${fin.year}"
            }
        }
    }
}