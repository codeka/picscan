package com.codeka.picscan.model

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ProjectDao {
  @Transaction
  @Query("SELECT * FROM projects WHERE id=:id")
  fun get(id: Long): ProjectWithPages

  @Query("SELECT * FROM projects WHERE draft=0")
  fun getAll(): LiveData<List<Project>>

  @Transaction
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun save(project: Project): Long

  @Transaction
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun savePage(page: Page): Long
}
