package com.codeka.picscan.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codeka.picscan.model.Page
import com.codeka.picscan.model.PageCorners
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils.bitmapToMat
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.*
import java.util.Collections.sort
import kotlin.Comparator
import kotlin.collections.ArrayList


class PageViewModel(val page: Page) : ViewModel() {
  val bmp: MutableLiveData<Bitmap> = MutableLiveData()
  val corners: MutableLiveData<PageCorners> = MutableLiveData()

  // TODO: disable for prod?
  private val enableDebug = true
  val debugBmp: MutableLiveData<Bitmap> = MutableLiveData()

  /** Attempt to find the edges of the page for the current image. */
  fun findEdges() {
    viewModelScope.launch {
      withContext(Dispatchers.Default) {
        Log.i(TAG, "Finding edges of page")

        val origMat = Mat()
        bitmapToMat(bmp.value!!, origMat)

        val edges = Mat()
        Imgproc.cvtColor(origMat, edges, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(edges, edges, Size(11.0, 11.0), 0.0);
        Imgproc.Canny(edges, edges, 75.0, 200.0);
        if (enableDebug) {
          val outBmp =
            Bitmap.createBitmap(bmp.value!!.width, bmp.value!!.height, Bitmap.Config.ARGB_8888)
          matToBitmap(edges, outBmp)
          withContext(Dispatchers.Main) {
            debugBmp.value = outBmp
          }
        }

        val largestContour = findLargestContour(edges)
        if (largestContour != null) {
          Log.i(TAG, "Found a contour, extracting points")
          val points = sortPoints(largestContour.toArray())
          val c = PageCorners()
          c.topLeft.x = points[0].x.toFloat()
          c.topLeft.y = points[0].y.toFloat()
          c.topRight.x = points[1].x.toFloat()
          c.topRight.y = points[1].y.toFloat()
          c.bottomRight.x = points[2].x.toFloat()
          c.bottomRight.y = points[2].y.toFloat()
          c.bottomLeft.x = points[3].x.toFloat()
          c.bottomLeft.y = points[3].y.toFloat()

          Log.i(TAG, "Edges = ${c.topLeft.x},${c.topLeft.y} - ${c.topRight.x},${c.topRight.y}" +
              " - ${c.bottomRight.x},${c.bottomRight.y} - ${c.bottomLeft.x},${c.bottomLeft.y}")

          withContext(Dispatchers.Main) {
            corners.value = c
          }

          largestContour.release()
        }
        edges.release()
        origMat.release()
      }
    }
  }

  // TODO: keep the other contours as well, so we can snap to them when you edit and stuff...
  private fun findLargestContour(src: Mat): MatOfPoint2f? {
    val contours = ArrayList<MatOfPoint>()
    Imgproc.findContours(src, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

    // Get the 5 largest contours
    sort(contours) { o1, o2 ->
      val area1 = Imgproc.contourArea(o1)
      val area2 = Imgproc.contourArea(o2)
      (area2 - area1).toInt()
    }
    if (contours.size > 5) {
      contours.subList(4, contours.size - 1).clear()
    }
    var largest: MatOfPoint2f? = null
    for (contour in contours) {
      val approx = MatOfPoint2f()
      val c = MatOfPoint2f()
      contour.convertTo(c, CvType.CV_32FC2)
      Imgproc.approxPolyDP(c, approx, Imgproc.arcLength(c, true) * 0.02, true)
      if (approx.total() == 4L && Imgproc.contourArea(contour) > 150) {
        // the contour has 4 points, it's valid
        largest = approx
        break
      }
    }
    return largest
  }

  /**
   * Sort the points so that they're in a nice clockwise order, starting from top-left.
   *
   * See: http://www.pyimagesearch.com/2014/08/25/4-point-opencv-getperspective-transform-example/
   */
  private fun sortPoints(src: Array<Point>): Array<Point> {
    val srcPoints = src.toList()
    val sum: Comparator<Point> = Comparator {
        lhs, rhs -> (lhs.y + lhs.x).compareTo(rhs.y + rhs.x)
    }
    val diff: Comparator<Point> = Comparator {
        lhs, rhs -> (lhs.y - lhs.x).compareTo(rhs.y - rhs.x)
    }
    return arrayOf(
      Collections.min(srcPoints, sum),
      Collections.min(srcPoints, diff),
      Collections.max(srcPoints, sum),
      Collections.max(srcPoints, diff))
  }

  private val target = object : Target {
    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
      bmp.value = bitmap
      findEdges()
    }

    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
    }
  }

  init {
    Picasso.get().load(page.photoUri).into(target)
  }

  companion object {
    private const val TAG = "PageViewModel"
  }
}