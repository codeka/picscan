package com.codeka.picscan.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import com.codeka.picscan.model.Page
import com.codeka.picscan.model.imageFile
import com.squareup.picasso.Picasso

const val TAG = "ExportHelpers"

fun drawPage(page: Page, canvas: Canvas) {
  val bmp = Picasso.get().load(page.imageFile()).get()

  val srcRect = Rect(0, 0, bmp.width, bmp.height)
  val destRect = if (srcRect.width() > srcRect.height()) {
    val ratio = srcRect.height().toDouble() / srcRect.width()
    val destHeight = canvas.width * ratio
    Log.i(
      TAG, "height < width ratio=$ratio canvas.width=${canvas.width} " +
        "canvas.height=${canvas.height} destHeight=$destHeight")
    Rect(
      0, (canvas.height / 2.0 - destHeight / 2.0).toInt(),
      canvas.width, (canvas.height / 2.0 + destHeight / 2.0).toInt())
  } else {
    val ratio = srcRect.width().toDouble() / srcRect.height()
    val destWidth = canvas.height * ratio
    Log.i(
      TAG, "width < height ratio=$ratio srcRect.width=${srcRect.width()} " +
        "srcRect.height=${srcRect.height()} canvas.width=${canvas.width} " +
        "canvas.height=${canvas.height} destWidth=$destWidth")
    Rect(
      (canvas.width / 2 - destWidth / 2).toInt(), 0,
      (canvas.width / 2 + destWidth / 2).toInt(), canvas.height)
  }

  Log.i(TAG, "drawing bitmap: src=${srcRect.toShortString()} dest=${destRect.toShortString()}")
  canvas.drawBitmap(bmp, srcRect, destRect, Paint())
}
