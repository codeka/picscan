package com.codeka.picscan.ui.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.GestureDetectorCompat
import kotlin.math.absoluteValue

/**
 * GestureImageView is an ImageView with additional support for zoom/pan gestures.
 */
class GestureImageView(context: Context, attrs: AttributeSet?) : AppCompatImageView(context, attrs) {
  private var panX = 0.0f
  private var panY = 0.0f
  private var scale = 1.0f;
  private var flingVelocityX = 0.0f
  private var flingVelocityY = 0.0f

  private val gestureListener = object : GestureDetector.SimpleOnGestureListener() {
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, dx: Float, dy: Float ): Boolean {
      panX -= dx / scale
      panY -= dy / scale
      invalidate()
      return true
    }

    override fun onFling( e1: MotionEvent?, e2: MotionEvent?, vx: Float, vy: Float ): Boolean {
      flingVelocityX = vx
      flingVelocityY = vy
      invalidate()
      return true
    }
  }

  private val scaleGestureListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
    override fun onScale(detector: ScaleGestureDetector?): Boolean {
      scale *= detector?.scaleFactor ?: 1.0f

      // Don't let it get too big or too small.
      scale = scale.coerceAtLeast(0.1f).coerceAtMost(10.0f)

      invalidate()
      return true
    }
  }

  private val gestureDetector = GestureDetectorCompat(context, gestureListener)
  private val scaleGestureDetector = ScaleGestureDetector(context, scaleGestureListener)

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    scaleGestureDetector.onTouchEvent(event)
    gestureDetector.onTouchEvent(event)
    return true
  }

  override fun onDraw(canvas: Canvas?) {
    if (canvas == null) {
      return
    }

    val canvasSaveCount = canvas.saveCount
    canvas.save()

    canvas.scale(scale, scale, width / 2.0f, height / 2.0f)
    canvas.translate(panX, panY)

    super.onDraw(canvas)

    canvas.restoreToCount(canvasSaveCount)

    if (flingVelocityX.absoluteValue > 0.01f || flingVelocityY.absoluteValue > 0.01f) {
      Log.i("DEANH", "fling velocity = $flingVelocityX, $flingVelocityY")
      panX += (flingVelocityX / 60f /* fps */) / scale
      panY += (flingVelocityY / 60f /* fps */) / scale
      flingVelocityX *= FLING_DECELERATION
      flingVelocityY *= FLING_DECELERATION
      invalidate()
    } else {
      flingVelocityX = 0.0f
      flingVelocityY = 0.0f
    }
  }

  companion object {
    private const val FLING_DECELERATION = 0.9f;
  }
}
