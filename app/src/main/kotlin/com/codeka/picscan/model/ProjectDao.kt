package com.codeka.picscan.model

import androidx.room.*

@Dao
interface ProjectDao {
  @Transaction
  @Query("SELECT * FROM projects WHERE id=:id")
  fun get(id: Int): ProjectWithPages

  @Transaction
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun save(project: Project): Long

  @Transaction
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun savePage(page: Page): Long
}
