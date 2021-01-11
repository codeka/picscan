package com.codeka.picscan.model

import android.content.Context
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProjectRepository(private val projectDao: ProjectDao) {
  fun get(id: Int): ProjectWithPages = projectDao.get(id)

  /** Saves the given [Project] to the data store. */
  suspend fun save(project: ProjectWithPages) {
    return withContext(Dispatchers.IO) {
      project.project.id = projectDao.save(project.project)
      for (page in project.pages) {
        page.id = projectDao.savePage(page)
      }
    }
  }

  /** Saves just the given [Page] to the data store. */
  suspend fun savePage(page: Page) {
    // You can't use this method the first  time you save a page, you've got to add it to the
    // project first and save the project.
    if (page.id == 0L || page.projectId == 0L) {
      throw IllegalArgumentException("Cannot save a page if it has never been saved before.")
    }

    withContext(Dispatchers.IO) {
      projectDao.savePage(page)
    }
  }

  companion object {
    fun create(context: Context): ProjectRepository {
      return ProjectRepository(Store.get(context).projects())
    }
  }
}
