package com.codeka.picscan.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.BindingAdapter
import com.codeka.picscan.model.PageCorners
import org.opencv.core.Point

@BindingAdapter("app:bitmap")
fun setBitmap(view: PageCornerEditorView, bmp: Bitmap?) {
  view.setImageBitmap(bmp)
}

@BindingAdapter("app:corners")
fun setCorners(view: PageCornerEditorView, corners: PageCorners?) {
  view.setCorners(corners)
}

class PageCornerEditorView  @JvmOverloads constructor(
  context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
  AppCompatImageView(context, attrs, defStyleAttr) {

  private var corners: PageCorners? = null
  private val linePaint = Paint()
  private val fillPaint = Paint()
  private val nodeRadius: Float

  override fun setImageBitmap(bmp: Bitmap?) {
    super.setImageBitmap(bmp)

    if (corners == null && bmp != null) {
      corners = PageCorners(
        PointF(0.0f, 0.0f),
        PointF(bmp.width.toFloat(), 0.0f),
        PointF(bmp.width.toFloat(), bmp.height.toFloat()),
        PointF(0.0f, bmp.height.toFloat())
      )
    }
  }

  fun setCorners(corners: PageCorners?) {
    if (corners == null) {
      // Ignore.
      return
    }

    this.corners = corners
    invalidate()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    val corners = this.corners ?: return
    val saveCount = canvas.saveCount
    canvas.save()
    canvas.concat(imageMatrix)

    canvas.drawLine(
      corners.topLeft.x, corners.topLeft.y,
      corners.topRight.x, corners.topRight.y, linePaint)
    canvas.drawLine(
      corners.topRight.x, corners.topRight.y,
      corners.bottomRight.x, corners.bottomRight.y, linePaint)
    canvas.drawLine(
      corners.bottomRight.x, corners.bottomRight.y,
      corners.bottomLeft.x, corners.bottomLeft.y, linePaint)
    canvas.drawLine(
      corners.bottomLeft.x, corners.bottomLeft.y,
      corners.topLeft.x, corners.topLeft.y, linePaint)

    canvas.drawCircle(corners.topLeft.x, corners.topLeft.y, nodeRadius, fillPaint)
    canvas.drawCircle(corners.topLeft.x, corners.topLeft.y, nodeRadius, linePaint)

    canvas.drawCircle(corners.topRight.x, corners.topRight.y, nodeRadius, fillPaint)
    canvas.drawCircle(corners.topRight.x, corners.topRight.y, nodeRadius, linePaint)

    canvas.drawCircle(corners.bottomRight.x, corners.bottomRight.y, nodeRadius, fillPaint)
    canvas.drawCircle(corners.bottomRight.x, corners.bottomRight.y, nodeRadius, linePaint)

    canvas.drawCircle(corners.bottomLeft.x, corners.bottomLeft.y, nodeRadius, fillPaint)
    canvas.drawCircle(corners.bottomLeft.x, corners.bottomLeft.y, nodeRadius, linePaint)

    canvas.restoreToCount(saveCount)
  }

  init {
    linePaint.strokeWidth =
      TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 2.0f, context.resources.displayMetrics)
    linePaint.color = Color.GREEN
    linePaint.style = Paint.Style.STROKE

    fillPaint.style = Paint.Style.FILL
    fillPaint.color = Color.WHITE

    nodeRadius =
      TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 32.0f, context.resources.displayMetrics)
  }
}
