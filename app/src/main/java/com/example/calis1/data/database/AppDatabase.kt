package com.example.calis1.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.calis1.data.dao.AlcoholRecordDao
import com.example.calis1.data.dao.NotaDao // Importar el nuevo DAO
import com.example.calis1.data.dao.UsuarioDao
import com.example.calis1.data.entity.AlcoholRecord
import com.example.calis1.data.entity.Nota // Importar la nueva entidad
import com.example.calis1.data.entity.Usuario

@Database(
    entities = [Usuario::class, AlcoholRecord::class, Nota::class], // 1. Agregar Nota::class
    version = 3, // 2. Incrementar la versión a 3
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun alcoholRecordDao(): AlcoholRecordDao
    abstract fun notaDao(): NotaDao // 3. Agregar el método abstracto para el DAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migración existente
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
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
                """)
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_alcohol_records_userId_semanaInicio` 
                    ON `alcohol_records` (`userId`, `semanaInicio`)
                """)
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_alcohol_records_userId_diaSemana` 
                    ON `alcohol_records` (`userId`, `diaSemana`)
                """)
            }
        }

        // 4. Crear la nueva migración de versión 2 a 3
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notas` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // 5. Agregar la nueva migración
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}