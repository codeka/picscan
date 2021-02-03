package com.codeka.picscan.model

import android.content.Context
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProjectRepository(private val projectDao: ProjectDao) {
  fun get(id: Long): ProjectWithPages = projectDao.get(id)

  fun getAll(): LiveData<List<Project>> = projectDao.getAll()

  /** Saves the given [Project] to the data store. */
  suspend fun save(project: ProjectWithPages) {
    // TODO: move this withContext to the callers.
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
