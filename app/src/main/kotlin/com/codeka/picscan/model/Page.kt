package com.codeka.picscan.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents one page of the document in a [Project].
 */
@Entity(tableName = "pages")
data class Page (
  /** A unique ID for this page. */
  @PrimaryKey(autoGenerate = true) var id: Long,

  /** The ID of the project this page is associated with. */
  val projectId: Long,

  /** The URI of the original photo that was taken for this page. */
  val photoUri: String,

  @Embedded
  var corners: PageCorners,

  /** The [ImageFilterType] that we used to filter the image. */
  var filter: ImageFilterType
)
