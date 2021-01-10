package com.codeka.picscan.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.LocalDateTime

/**
 * Project is the root object in a 'project', where we define all of the images, transformations
 * and so on, that allows us to export a document.
 */
@Entity(tableName = "projects")
data class Project(
  @PrimaryKey(autoGenerate = true) var id: Long,
  /**
   * If true, this project is in "draft" mode, which happens before the first picture is completed
   * and the project becomes permanent. If you restart the app with a project in draft, we'll
   * delete the project and you'll have to start over.
   */
  var draft: Boolean,

  /**
   * The name of the project. We start off with an auto-generated one but you can edit this to be
   * whatever you want.
   */
  var name: String,

  /**
   * The date (as seconds from epoch) this project was initially created.
   */
  val createDate: Long,
)
