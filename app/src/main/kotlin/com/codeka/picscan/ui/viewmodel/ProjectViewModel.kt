package com.codeka.picscan.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.*
import com.codeka.picscan.App
import com.codeka.picscan.export.PdfExporter
import com.codeka.picscan.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        name = String.format(locale, "scan %s", format.format(now)))

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
    viewModelScope.launch {
      withContext(Dispatchers.IO) {
        val proj = repo.get(id)
        withContext(Dispatchers.Main) {
          project.value = proj
        }
      }
    }
  }

  /** Saves the project to the data store. */
  suspend fun save() {
    val projectWithPages = project.value
    if (projectWithPages != null) {
      repo.save(projectWithPages)
    }
  }

  fun export(context: Context, lifecycleOwner: LifecycleOwner) {
    viewModelScope.launch {
      val exporter = PdfExporter()
      exporter.export(project.value!!).observe(lifecycleOwner) {
        val intent = Intent(Intent.ACTION_SEND)

        intent.type = "application/pdf"

        var fileName = project.value?.project?.name ?: "untitle"
        if (!fileName.endsWith(".pdf")) {
          fileName += ".pdf"
        }

        intent.putExtra(
          Intent.EXTRA_STREAM,
          FileProvider.getUriForFile(context, "com.codeka.picscan.FileProvider", it))
        intent.putExtra(Intent.EXTRA_SUBJECT, fileName)
        context.startActivity(intent)
      }
    }
  }

  fun findPage(pageId: Long): Page? {
    for (p in project.value!!.pages) {
      if (p.id == pageId) {
        return p
      }
    }

    return null
  }

  /** Create a new page, not saved to the data store yet. */
  fun newPage(photoUri: Uri): Page {
    val proj = project.value!!
    return Page(
      id = 0, projectId = proj.project.id, photoUri = photoUri.toString(), corners = PageCorners(),
      filter = ImageFilterType.None)
  }

  /**
   * Adds a page, created by [newPage], to our list we keep track of. Only do this after the page
   * has been filled out with the filtered bitmap, etc.
   */
  fun addPage(page: Page) {
    val proj = project.value!!
    val pages = ArrayList(proj.pages)
    pages.add(page)
    proj.pages = pages
    project.value = proj
  }
}
