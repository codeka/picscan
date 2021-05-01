package com.codeka.picscan.model

import android.graphics.Bitmap
import androidx.core.graphics.applyCanvas
import com.codeka.picscan.export.drawPage
import java.lang.Integer.max

class PreviewGenerator {

  fun generatePreview(project: ProjectWithPages, size: Int): Bitmap {
    // TODO: Create a bitmap based on the size of the final icon in dp.
    val preview = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

    preview.applyCanvas {
      val canvas = this // Easier to read.

      canvas.scale(0.9f, 0.9f)
      canvas.translate(0.05f * canvas.width, 0.05f * canvas.height)

      val startIndex = max(0, project.pages.size - 3)
      val numPages = project.pages.size - startIndex

      canvas.rotate(-10f * (numPages - 1), canvas.width / 2f, canvas.height / 2f)
      for (i in startIndex until project.pages.size) {
        val page = project.pages[i]

        // TODO: draw a border

        drawPage(page, canvas)

        if (i != project.pages.size - 1) {
          canvas.rotate(10f, canvas.width / 2f, canvas.height / 2f)
        }
      }
    }
    // TODO: implement me

    return preview
  }
}
