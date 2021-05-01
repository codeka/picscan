package com.codeka.picscan.export

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.codeka.picscan.App
import com.codeka.picscan.model.Page
import com.codeka.picscan.model.ProjectWithPages
import com.codeka.picscan.model.imageFile
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfExporter {
  suspend fun export(projectWithPages: ProjectWithPages) : MutableLiveData<File> {
    val file = MutableLiveData<File>()

    withContext(Dispatchers.IO) {
      val doc = PdfDocument()

      var pageNo = 1
      for (page in projectWithPages.pages) {
        // US Letter size page: TODO: support more page size
        val pageInfo = PdfDocument.PageInfo.Builder(612, 792, pageNo)
        val pdfPage = doc.startPage(pageInfo.create())
        drawPage(page, pdfPage)
        doc.finishPage(pdfPage)
        pageNo++
      }

      val dir = File(App.filesDir, "pdfs")
      dir.mkdirs()
      // TODO: use the project's name?
      val f = File(dir, "%06d.pdf".format(projectWithPages.project.id))
      f.outputStream().use {
        doc.writeTo(it)
      }
      doc.close()

      withContext(Dispatchers.Main) {
        file.value = f
      }
    }

    return file
  }

  private fun drawPage(page: Page, pdfPage: PdfDocument.Page) {
    val canvas = pdfPage.canvas
    drawPage(page, canvas)
  }

  companion object {
    private const val TAG = "PdfExporter"
  }
}