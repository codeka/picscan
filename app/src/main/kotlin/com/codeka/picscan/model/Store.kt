package com.codeka.picscan.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [Project::class, Page::class],
  version = 1,
  exportSchema = false)
abstract class Store : RoomDatabase() {

  abstract fun projects(): ProjectDao

  companion object {
    @Volatile
    private var i: Store? = null

    fun get(context: Context): Store {
      return i ?: synchronized(this) {
        val instance =
          Room.databaseBuilder(context.applicationContext, Store::class.java, "store").build()
        i = instance
        instance
      }
    }
  }
}