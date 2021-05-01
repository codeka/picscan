package com.codeka.picscan.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [Project::class, Page::class],
  version = 4,
  exportSchema = false)
@TypeConverters(Converters::class)
abstract class Store : RoomDatabase() {

  abstract fun projects(): ProjectDao

  companion object {
    @Volatile
    private var i: Store? = null

    private val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE projects ADD COLUMN previewUpToDate INTEGER NOT NULL DEFAULT 0")
      }
    }

    fun get(context: Context): Store {
      return i ?: synchronized(this) {
        val instance =
          Room.databaseBuilder(context.applicationContext, Store::class.java, "store")
            .addMigrations(MIGRATION_3_4)
            // TODO: only ignore migrations while testing...
            //.fallbackToDestructiveMigration()
            .build()
        i = instance
        instance
      }
    }
  }
}