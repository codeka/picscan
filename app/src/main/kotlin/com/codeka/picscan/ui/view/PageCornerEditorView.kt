package com.codeka.picscan.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatImageView
import com.codeka.picscan.model.PageCorners
import kotlin.math.min
import kotlin.math.sqrt

class PageCornerEditorView  @JvmOverloads constructor(
  context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
  AppCompatImageView(context, attrs, defStyleAttr) {

  private var corners: PageCorners? = null
  private val linePaint = Paint()
  private val fillPaint = Paint()
  private val draggingPaint = Paint()
  private val nodeRadius: Float

  private enum class CornerName {
    None,
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight
  }
  private var draggingCorner = CornerName.None

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

    canvas.drawCircle(
      corners.topLeft.x, corners.topLeft.y, nodeRadius, getFillPaintFor(CornerName.TopLeft))
    canvas.drawCircle(corners.topLeft.x, corners.topLeft.y, nodeRadius, linePaint)

    canvas.drawCircle(
      corners.topRight.x, corners.topRight.y, nodeRadius, getFillPaintFor(CornerName.TopRight))
    canvas.drawCircle(corners.topRight.x, corners.topRight.y, nodeRadius, linePaint)

    canvas.drawCircle(
      corners.bottomRight.x,
      corners.bottomRight.y,
      nodeRadius,
      getFillPaintFor(CornerName.BottomRight))
    canvas.drawCircle(corners.bottomRight.x, corners.bottomRight.y, nodeRadius, linePaint)

    canvas.drawCircle(
      corners.bottomLeft.x,
      corners.bottomLeft.y,
      nodeRadius,
      getFillPaintFor(CornerName.BottomLeft))
    canvas.drawCircle(corners.bottomLeft.x, corners.bottomLeft.y, nodeRadius, linePaint)

    canvas.restoreToCount(saveCount)
  }

  private fun getFillPaintFor(corner: CornerName): Paint {
    return if (draggingCorner == corner) {
      draggingPaint
    } else {
      fillPaint
    }
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    if (event == null) {
      return super.onTouchEvent(event)
    }

    val touchPoint = viewToImage(PointF(event.x, event.y))
    val corners = this.corners ?: return super.onTouchEvent(event)

    when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        // Find the closest corner to start dragging.
        val distanceToTopLeft = distance(touchPoint, corners.topLeft)
        val distanceToTopRight = distance(touchPoint, corners.topRight)
        val distanceToBottomLeft = distance(touchPoint, corners.bottomLeft)
        val distanceToBottomRight = distance(touchPoint, corners.bottomRight)
        val minDistance =
          min(
            min(distanceToTopLeft, distanceToTopRight),
            min(distanceToBottomLeft, distanceToBottomRight)
          )

        draggingCorner = when (minDistance.toInt()) {
          distanceToTopLeft.toInt() -> {
            CornerName.TopLeft
          }
          distanceToTopRight.toInt() -> {
            CornerName.TopRight
          }
          distanceToBottomLeft.toInt() -> {
            CornerName.BottomLeft
          }
          distanceToBottomRight.toInt() -> {
            CornerName.BottomRight
          }
          else -> {
            // ?? shouldn't happen?
            CornerName.None
          }
        }
        invalidate()
        return true
      }
      MotionEvent.ACTION_MOVE -> {
        when (draggingCorner) {
          CornerName.TopLeft -> {
            corners.topLeft.x = touchPoint.x
            corners.topLeft.y = touchPoint.y
          }
          CornerName.TopRight -> {
            corners.topRight.x = touchPoint.x
            corners.topRight.y = touchPoint.y
          }
          CornerName.BottomLeft -> {
            corners.bottomLeft.x = touchPoint.x
            corners.bottomLeft.y = touchPoint.y
          }
          CornerName.BottomRight -> {
            corners.bottomRight.x = touchPoint.x
            corners.bottomRight.y = touchPoint.y
          }
        }
        invalidate()
      }
      MotionEvent.ACTION_UP -> {
        draggingCorner = CornerName.None
        invalidate()
        return true
      }
    }
    return super.onTouchEvent(event)
  }

  /** Convert a coordinate in "view" space to "image" space. */
  private fun viewToImage(pt: PointF): PointF {
    val coords = arrayOf(pt.x, pt.y).toFloatArray()
    val inverseImageMatrix = Matrix()
    imageMatrix.invert(inverseImageMatrix)
    inverseImageMatrix.mapPoints(coords)
    return PointF(coords[0], coords[1])
  }

  private fun imageToView(pt: PointF): PointF {
    val coords = arrayOf(pt.x, pt.y).toFloatArray()
    imageMatrix.mapPoints(coords)
    return PointF(coords[0], coords[1])
  }

  private fun distance(pt1: PointF, pt2: PointF): Float {
    return sqrt((pt1.x - pt2.x) * (pt1.x - pt2.x) + (pt1.y - pt2.y) * (pt1.y - pt2.y))
  }

  init {
    linePaint.strokeWidth =
      TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 2.0f, context.resources.displayMetrics)
    linePaint.color = Color.GREEN
    linePaint.style = Paint.Style.STROKE

    fillPaint.style = Paint.Style.FILL
    fillPaint.color = Color.WHITE

    draggingPaint.style = Paint.Style.FILL
    draggingPaint.color = Color.YELLOW

    nodeRadius =
      TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 32.0f, context.resources.displayMetrics)
  }
}
