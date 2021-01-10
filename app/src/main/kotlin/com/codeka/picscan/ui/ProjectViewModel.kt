package com.codeka.picscan.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codeka.picscan.App
import com.codeka.picscan.model.Page
import com.codeka.picscan.model.Project
import com.codeka.picscan.model.ProjectRepository
import com.codeka.picscan.model.ProjectWithPages
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.chrono.Chronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.*

/**
 * The [ViewModel] that contains all of the details needed to work with a project.
 */
class ProjectViewModel : ViewModel() {
  private val repo = ProjectRepository.create(App)

  val project = MutableLiveData<ProjectWithPages>()

  /**
   * Create a new project, completely overwriting any that we are current editing. You must call
   * either this or [load] before you can do anything else.
   */
  fun create() {
    viewModelScope.launch {
      val locale = Locale.getDefault()
      val format = DateTimeFormatter.ofPattern(
        DateTimeFormatterBuilder.getLocalizedDateTimePattern(
          FormatStyle.MEDIUM, FormatStyle.SHORT, Chronology.ofLocale(locale), locale),
        locale)
      val now = LocalDateTime.now()
      val proj = Project(
        id = 0,
        draft = true,
        createDate = now.toEpochSecond(ZoneOffset.UTC),
        name = String.format(locale, "scan ", format.format(now)))

      val projectWithPages = ProjectWithPages(proj, pages = arrayListOf())
      repo.save(projectWithPages)
      project.value = projectWithPages
    }
  }

  /**
   * Load the project with the given [id]. You must call either this or [create] before you can
   * do anything else.
   */
  fun load(id: Long) {
    // TODO
  }

  fun addPhoto(uri: Uri) {
    viewModelScope.launch {
      val proj = project.value!!
      val page = Page(id = 0, projectId = proj.project.id, photoUri = uri.toString())

      val pages = ArrayList(proj.pages)
      pages.add(page)
      proj.pages = pages

      project.value = proj
    }
  }
}
