package com.codeka.picscan.export

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.util.Log
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
      doc.close()

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
      val ratio = srcRect.height().toDouble() / srcRect.width()
      val destHeight = canvas.width * ratio
      Log.i(TAG, "height < width ratio=$ratio canvas.width=${canvas.width} " +
          "canvas.height=${canvas.height} destHeight=$destHeight")
      Rect(
        0, (canvas.height / 2.0 - destHeight / 2.0).toInt(),
        canvas.width, (canvas.height / 2.0 + destHeight / 2.0).toInt())
    } else {
      val ratio = srcRect.width().toDouble() / srcRect.height()
      val destWidth = canvas.height * ratio
      Log.i(TAG, "width < height ratio=$ratio srcRect.width=${srcRect.width()} " +
          "srcRect.height=${srcRect.height()} canvas.width=${canvas.width} " +
          "canvas.height=${canvas.height} destWidth=$destWidth")
      Rect(
        (canvas.width / 2 - destWidth / 2).toInt(), 0,
        (canvas.width / 2 + destWidth / 2).toInt(), canvas.height)
    }

    Log.i(TAG, "drawing bitmap: src=${srcRect.toShortString()} dest=${destRect.toShortString()}")
    canvas.drawBitmap(bmp, srcRect, destRect, Paint())
  }

  companion object {
    private const val TAG = "PdfExporter"
  }
}