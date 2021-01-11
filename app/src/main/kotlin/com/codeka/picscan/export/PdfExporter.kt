package com.codeka.picscan.export

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import androidx.lifecycle.MutableLiveData
import com.codeka.picscan.App
import com.codeka.picscan.model.Page
import com.codeka.picscan.model.ProjectWithPages
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

      withContext(Dispatchers.Main) {
        file.value = f
      }
    }

    return file
  }

  private fun drawPage(page: Page, pdfPage: PdfDocument.Page) {
    val canvas = pdfPage.canvas

    val dir = File(App.filesDir, "images")
    val file = File(dir, "%06d.jpg".format(page.id))
    val bmp = Picasso.get().load(file).get()

    val srcRect = Rect(0, 0, bmp.width, bmp.height)
    val destRect = if (srcRect.width() > srcRect.height()) {
      val ratio = srcRect.height() / srcRect.width()
      val destHeight = canvas.width * ratio
      Rect(
        0, canvas.height / 2 - destHeight / 2, canvas.width, canvas.height / 2 + destHeight / 2)
    } else {
      val ratio = srcRect.width() / srcRect.height()
      val destWidth = canvas.height * ratio
      Rect(
        canvas.width / 2 - destWidth / 2, 0, canvas.width / 2 + destWidth / 2, canvas.height)
    }

    canvas.drawBitmap(bmp, srcRect, destRect, Paint())
  }
}