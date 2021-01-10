package com.codeka.picscan.model

import androidx.room.Embedded
import androidx.room.Relation

data class ProjectWithPages (
  @Embedded val project: Project,

  @Relation(
    parentColumn = "id",
    entityColumn = "projectId")
  var pages: List<Page>,
)
