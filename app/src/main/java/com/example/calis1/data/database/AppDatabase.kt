package com.example.calis1.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.calis1.data.dao.AlcoholRecordDao
import com.example.calis1.data.dao.EventoDao // NUEVO: Importar EventoDao
import com.example.calis1.data.dao.NotaDao
import com.example.calis1.data.dao.UsuarioDao
import com.example.calis1.data.entity.AlcoholRecord
import com.example.calis1.data.entity.Evento // NUEVO: Importar Evento
import com.example.calis1.data.entity.Nota
import com.example.calis1.data.entity.Usuario

@Database(
    entities = [Usuario::class, AlcoholRecord::class, Nota::class, Evento::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun alcoholRecordDao(): AlcoholRecordDao
    abstract fun notaDao(): NotaDao
    abstract fun eventoDao(): EventoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null


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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `eventos` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `titulo` TEXT NOT NULL,
                        `descripcion` TEXT NOT NULL,
                        `fecha` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_eventos_userId` 
                    ON `eventos` (`userId`)
                """)

                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_eventos_fecha` 
                    ON `eventos` (`fecha`)
                """)
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}