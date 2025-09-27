package com.example.calis1.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.example.calis1.data.dao.UsuarioDao
import com.example.calis1.data.dao.AlcoholRecordDao
import com.example.calis1.data.entity.Usuario
import com.example.calis1.data.entity.AlcoholRecord

@Database(
    entities = [Usuario::class, AlcoholRecord::class],
    version = 2, // Incrementamos la versión
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun alcoholRecordDao(): AlcoholRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración de versión 1 a 2 (agregar tabla alcohol_records)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Crear tabla alcohol_records
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `alcohol_records` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `fecha` TEXT NOT NULL,
                        `diaSemana` INTEGER NOT NULL,
                        `nombreBebida` TEXT NOT NULL,
                        `mililitros` INTEGER NOT NULL,
                        `porcentajeAlcohol` REAL NOT NULL,
                        `semanaInicio` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // Crear índices para mejorar el rendimiento
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_alcohol_records_userId_semanaInicio` 
                    ON `alcohol_records` (`userId`, `semanaInicio`)
                """.trimIndent())

                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_alcohol_records_userId_diaSemana` 
                    ON `alcohol_records` (`userId`, `diaSemana`)
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2) // Agregar la migración
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}