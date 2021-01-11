package com.codeka.picscan.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
  entities = [Project::class, Page::class],
  version = 3,
  exportSchema = false)
@TypeConverters(Converters::class)
abstract class Store : RoomDatabase() {

  abstract fun projects(): ProjectDao

  companion object {
    @Volatile
    private var i: Store? = null

    fun get(context: Context): Store {
      return i ?: synchronized(this) {
        val instance =
          Room.databaseBuilder(context.applicationContext, Store::class.java, "store")
            // TODO: only ignore migrations while testing...
            .fallbackToDestructiveMigration()
            .build()
        i = instance
        instance
      }
    }
  }
}