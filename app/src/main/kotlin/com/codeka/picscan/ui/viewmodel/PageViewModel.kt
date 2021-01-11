package com.codeka.picscan.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codeka.picscan.App
import com.codeka.picscan.model.ImageFilterType
import com.codeka.picscan.model.Page
import com.codeka.picscan.model.PageCorners
import com.codeka.picscan.model.ProjectRepository
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.android.Utils.bitmapToMat
import org.opencv.android.Utils.matToBitmap
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.Collections.sort
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.math.pow
import kotlin.math.sqrt


class PageViewModel : ViewModel() {
  private val repo = ProjectRepository.create(App)

  var page: Page? = null

  val bmp: MutableLiveData<Bitmap> = MutableLiveData()
  val corners: MutableLiveData<PageCorners> = MutableLiveData()

  // The bitmap after it's been transformed so the corner are the actual corners.
  val transformedBmp: MutableLiveData<Bitmap> = MutableLiveData()

  // The bitmap after it's been transformed and had color filters applied.
  var filterType: MutableLiveData<ImageFilterType> = MutableLiveData(ImageFilterType.None)
  val filteredBmp: MutableLiveData<Bitmap> = MutableLiveData()

  // TODO: disable for prod?
  private val enableDebug = true
  val debugBmp: MutableLiveData<Bitmap> = MutableLiveData()

  /** Reset this [PageViewModel] to refer to the given page. */
  fun reset(page: Page) {
    this.page = page
    bmp.value = null
    corners.value = null
    transformedBmp.value = null

    Picasso.get().load(page.photoUri).into(target)
  }

  /** Saves the current [PageViewModel] back to the database. */
  fun save() {
    // First, save the final bitmap to disk.
    val outputDirectory = File(App.filesDir, "images")
    outputDirectory.mkdirs()
    val outputFile = File(outputDirectory, "%06d.jpg".format(page!!.id))
    Log.i(TAG, "Saving image: ${outputFile.absolutePath}")
    FileOutputStream(outputFile).use {
      filteredBmp.value!!.compress(Bitmap.CompressFormat.JPEG, 90, it)
    }

    page!!.corners = corners.value!!
    viewModelScope.launch {
      repo.savePage(page!!)
    }
  }

  /**
   * Attempt to find the edges of the page for the current image.
   *
   * <p>This function calculates initial values for [corners] by doing some operations on the
   * original [bmp]. You can make manual adjustments to the corners, or just use the auto calculated
   * ones and then call [transformCorners] to create [transformedBmp].
   */
  fun findEdges() {
    viewModelScope.launch {
      withContext(Dispatchers.Default) {
        Log.i(TAG, "Finding edges of page")

        val origMat = Mat()
        bitmapToMat(bmp.value!!, origMat)

        val edges = Mat()
        Imgproc.cvtColor(origMat, edges, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(edges, edges, Size(11.0, 11.0), 0.0)
        Imgproc.Canny(edges, edges, 75.0, 200.0)
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

          Log.i(
            TAG, "Edges = ${c.topLeft.x},${c.topLeft.y} - ${c.topRight.x},${c.topRight.y}" +
                " - ${c.bottomRight.x},${c.bottomRight.y} - ${c.bottomLeft.x},${c.bottomLeft.y}"
          )

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

  fun transformCorners() {
    viewModelScope.launch {
      withContext(Dispatchers.Default) {
        Log.i(TAG, "Transforming bitmap")

        val src = Mat()
        bitmapToMat(bmp.value!!, src)

        // Figure out the final width/height of the transformed image. We'll make it the minimum
        // of the width of the quadrilateral we've defined by corners.
        val pageCorers = corners.value!!
        val widthTop = sqrt(
          (pageCorers.topRight.x.toDouble() - pageCorers.topLeft.x).pow(2.0) +
              (pageCorers.topRight.y.toDouble() - pageCorers.topLeft.y).pow(2.0)
        )
        val widthBottom = sqrt(
          (pageCorers.bottomRight.x.toDouble() - pageCorers.bottomLeft.x).pow(2.0) +
              (pageCorers.bottomRight.y.toDouble() - pageCorers.bottomLeft.y).pow(2.0)
        )
        val width = widthTop.coerceAtMost(widthBottom)

        val heightLeft = sqrt(
          (pageCorers.bottomLeft.x.toDouble() - pageCorers.topLeft.x).pow(2.0) +
              (pageCorers.bottomLeft.y.toDouble() - pageCorers.topLeft.y).pow(2.0)
        )
        val heightRight = sqrt(
          (pageCorers.bottomRight.x.toDouble() - pageCorers.topRight.x).pow(2.0) +
              (pageCorers.bottomRight.y.toDouble() - pageCorers.topRight.y).pow(2.0)
        )
        val height = heightLeft.coerceAtMost(heightRight)
        Log.i(TAG, "Calculated size: $width,$height")

        // Get the perspective transform by creating two matrices containing the source points
        // and destination points
        val srcMat = Mat(4, 1, CvType.CV_32FC2)
        srcMat.put(
          0, 0,
          pageCorers.topLeft.x.toDouble(), pageCorers.topLeft.y.toDouble(),
          pageCorers.topRight.x.toDouble(), pageCorers.topRight.y.toDouble(),
          pageCorers.bottomRight.x.toDouble(), pageCorers.bottomRight.y.toDouble(),
          pageCorers.bottomLeft.x.toDouble(), pageCorers.bottomLeft.y.toDouble()
        )

        val dstMat = Mat(4, 1, CvType.CV_32FC2)
        dstMat.put(0, 0, 0.0, 0.0, width, 0.0, width, height, 0.0, height)

        val transformMat = Imgproc.getPerspectiveTransform(srcMat, dstMat)

        // Now perform the perspective transform
        val transformed = Mat(height.toInt(), width.toInt(), CvType.CV_8UC4)
        Imgproc.warpPerspective(src, transformed, transformMat, transformed.size())

        val result = Bitmap.createBitmap(width.toInt(), height.toInt(), Bitmap.Config.ARGB_8888)
        matToBitmap(transformed, result)
        withContext(Dispatchers.Main) {
          transformedBmp.value = result
          filteredBmp.value = result
          filterType.value = ImageFilterType.None
        }
      }
    }
  }

  fun filterImage(filter: ImageFilterType) {
    viewModelScope.launch {
      if (filter == ImageFilterType.None) {
        filteredBmp.value = transformedBmp.value
        filterType.value = filter
      } else {
        withContext(Dispatchers.Default) {
          Log.i(TAG, "Transforming bitmap")

          val srcBmp = transformedBmp.value!!
          val width = srcBmp.width
          val height = srcBmp.height

          val src = Mat()
          bitmapToMat(srcBmp, src)

          // TODO: this is just a test filter...
          Imgproc.cvtColor(src,src,Imgproc.COLOR_RGBA2GRAY);
          Imgproc.adaptiveThreshold(
            src, src, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 15.0);

          val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
          matToBitmap(src, result)

          withContext(Dispatchers.Main) {
            filteredBmp.value = result
            filterType.value = filter
          }
        }
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
    val sum: Comparator<Point> = Comparator { lhs, rhs -> (lhs.y + lhs.x).compareTo(rhs.y + rhs.x)
    }
    val diff: Comparator<Point> = Comparator { lhs, rhs -> (lhs.y - lhs.x).compareTo(rhs.y - rhs.x)
    }
    return arrayOf(
      Collections.min(srcPoints, sum),
      Collections.min(srcPoints, diff),
      Collections.max(srcPoints, sum),
      Collections.max(srcPoints, diff)
    )
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

  companion object {
    private const val TAG = "PageViewModel"
  }
}