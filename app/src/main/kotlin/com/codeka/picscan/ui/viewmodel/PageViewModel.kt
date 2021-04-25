package com.codeka.picscan.ui.viewmodel

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.camera.core.ImageProxy
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
import java.lang.Integer.min
import java.util.*
import java.util.Collections.sort
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.math.max
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
  val debugEdgeDetectBmp: MutableLiveData<Bitmap> = MutableLiveData()
  val debugContoursBmp: MutableLiveData<Bitmap> = MutableLiveData()

  /** Reset this [PageViewModel] to refer to the given page. */
  fun reset(page: Page) {
    this.page = page
    bmp.value = null
    corners.value = null
    transformedBmp.value = null

    Picasso.get().load(page.photoUri).into(target)
  }

  /** Saves the current [PageViewModel] back to the database. */
  suspend fun save() {
    val page = this.page
    if (page == null || page.id == 0L) {
      throw IllegalStateException("Cannot call PageViewModel.save() before ProjectViewModel.save()")
    }

    // First, save the final bitmap to disk.
    val outputDirectory = File(App.filesDir, "images")
    outputDirectory.mkdirs()
    val outputFile = File(outputDirectory, "%06d.jpg".format(page.id))
    Log.i(TAG, "Saving image: ${outputFile.absolutePath}")
    withContext(Dispatchers.IO) {
      FileOutputStream(outputFile).use {
        filteredBmp.value!!.compress(Bitmap.CompressFormat.JPEG, 90, it)
      }
    }

    page.corners = corners.value!!
    repo.savePage(page)
  }

  /**
   * Deletes all the files associated with the page. Only do this if you're also about to delete the
   * page (or project) itself.
   */
  fun deleteFiles() {
    val page = this.page ?: return

    try {
      File(page.photoUri).delete()
    } catch (e: java.lang.Exception) {
      Log.w(TAG, String.format("Unexpected error deleting photoUri: %s %s", page.photoUri, e))
    }

    val directory = File(App.filesDir, "images")
    val filteredImage = File(directory, "%06d.jpg".format(page.id))
    if (filteredImage.exists()) {
      try {
        filteredImage.delete()
      } catch(e: java.lang.Exception) {
        Log.w(TAG, "Unexpected error deleting filtered image: %s", e)
      }
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
        origMat.copyTo(edges)
        // Resize edges to be a bit smaller
        Imgproc.resize(edges, edges, Size(), 0.1, 0.1, Imgproc.INTER_CUBIC)
        Imgproc.cvtColor(edges, edges, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(edges, edges, Size(3.0, 3.0), 0.0)
        if (enableDebug) {
          val outBmp =
            Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888)
          matToBitmap(edges, outBmp)
          withContext(Dispatchers.Main) {
            debugEdgeDetectBmp.value = outBmp
          }
        }
        Imgproc.Canny(edges, edges, 10.0, 100.0)

        val largestContour = findLargestContour(edges)
        if (largestContour != null) {
          Log.i(TAG, "Found a contour, extracting points")
          val points = sortPoints(largestContour.toArray())
          val c = PageCorners()
          c.topLeft.x = points[0].x.toFloat() * 10.0f
          c.topLeft.y = points[0].y.toFloat() * 10.0f
          c.topRight.x = points[1].x.toFloat() * 10.0f
          c.topRight.y = points[1].y.toFloat() * 10.0f
          c.bottomRight.x = points[2].x.toFloat() * 10.0f
          c.bottomRight.y = points[2].y.toFloat() * 10.0f
          c.bottomLeft.x = points[3].x.toFloat() * 10.0f
          c.bottomLeft.y = points[3].y.toFloat() * 10.0f

          Log.i(
            TAG, "Edges = ${c.topLeft.x},${c.topLeft.y} - ${c.topRight.x},${c.topRight.y}" +
                " - ${c.bottomRight.x},${c.bottomRight.y} - ${c.bottomLeft.x},${c.bottomLeft.y}"
          )

          withContext(Dispatchers.Main) {
            corners.value = c
          }

          largestContour.release()
        } else {
          val c = PageCorners()
          c.topLeft.x = 0.0f
          c.topLeft.y = 0.0f
          c.topRight.x = bmp.value!!.width.toFloat()
          c.topRight.y = 0.0f
          c.bottomRight.x = bmp.value!!.width.toFloat()
          c.bottomRight.y = bmp.value!!.height.toFloat()
          c.bottomLeft.x = 0.0f
          c.bottomLeft.y = bmp.value!!.height.toFloat()
          withContext(Dispatchers.Main) {
            corners.value = c
          }
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

        val srcBmp = bmp.value ?: return@withContext
        val src = Mat()
        bitmapToMat(srcBmp, src)

        // Figure out the final width/height of the transformed image. We'll make it the minimum
        // of the width of the quadrilateral we've defined by corners.
        val pageCorners = corners.value
        if (pageCorners == null) {
          // We haven't done a transform yet, just return the original bitmap.
          withContext(Dispatchers.Main) {
            transformedBmp.value = srcBmp
            filteredBmp.value = srcBmp
            filterType.value = ImageFilterType.None
          }
          return@withContext
        }

        val widthTop = sqrt(
          (pageCorners.topRight.x.toDouble() - pageCorners.topLeft.x).pow(2.0) +
              (pageCorners.topRight.y.toDouble() - pageCorners.topLeft.y).pow(2.0)
        )
        val widthBottom = sqrt(
          (pageCorners.bottomRight.x.toDouble() - pageCorners.bottomLeft.x).pow(2.0) +
              (pageCorners.bottomRight.y.toDouble() - pageCorners.bottomLeft.y).pow(2.0)
        )
        val width = widthTop.coerceAtMost(widthBottom)

        val heightLeft = sqrt(
          (pageCorners.bottomLeft.x.toDouble() - pageCorners.topLeft.x).pow(2.0) +
              (pageCorners.bottomLeft.y.toDouble() - pageCorners.topLeft.y).pow(2.0)
        )
        val heightRight = sqrt(
          (pageCorners.bottomRight.x.toDouble() - pageCorners.topRight.x).pow(2.0) +
              (pageCorners.bottomRight.y.toDouble() - pageCorners.topRight.y).pow(2.0)
        )
        val height = heightLeft.coerceAtMost(heightRight)
        Log.i(TAG, "Calculated size: $width,$height")

        // Get the perspective transform by creating two matrices containing the source points
        // and destination points
        val srcMat = Mat(4, 1, CvType.CV_32FC2)
        srcMat.put(
          0, 0,
          pageCorners.topLeft.x.toDouble(), pageCorners.topLeft.y.toDouble(),
          pageCorners.topRight.x.toDouble(), pageCorners.topRight.y.toDouble(),
          pageCorners.bottomRight.x.toDouble(), pageCorners.bottomRight.y.toDouble(),
          pageCorners.bottomLeft.x.toDouble(), pageCorners.bottomLeft.y.toDouble()
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

          // First, a median blur with a largish matrix size will remove small details (like text)
          // https://medium.com/@florestony5454/median-filtering-with-python-and-opencv-2bce390be0d1
          val blur = Mat()
          Imgproc.medianBlur(src, blur, 21);

          // Next, we do a much larger gaussian blur to find the 'average background color'
          // https://stackoverflow.com/a/62634900
          Imgproc.GaussianBlur(blur, blur, Size(61.0, 61.0), 0.0)

          // Figure out the mean color (which should be close-ish the background color).
          val mean = Core.mean(blur).`val`[0]

          // Now, divide the source by the blurred image, to remove the large details.
          val res = src.clone()
          Core.divide(src, blur, res, mean, -1)

          // Finally, adjust the contrast a bit to make the blacks more defined.
          adjustContrastBrightness(res, 64.0, 0.0)

          val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
          matToBitmap(res, result)

          withContext(Dispatchers.Main) {
            filteredBmp.value = result
            filterType.value = filter
          }
        }
      }
    }
  }

  private fun adjustContrastBrightness(img: Mat, contrast: Double, brightness: Double) {
    if (brightness != 0.0) {
      var shadow = 0.0
      var highlight = 255.0 + brightness
      if (brightness > 0.0) {
        shadow = brightness
        highlight = 255.0
      }
      val alpha = (highlight - shadow) / 255.0
      val beta = shadow
      img.convertTo(img, img.type(), alpha, beta)
    }

    if (contrast != 0.0) {
      val f = 131.0 * (contrast + 127.0) / (127.0 * (131.0 - contrast))
      val alpha = f
      val beta = 127.0*(1.0 - f)
      img.convertTo(img, img.type(), alpha, beta)
    }
  }

  // TODO: keep the other contours as well, so we can snap to them when you edit and stuff...
  private suspend fun findLargestContour(src: Mat): MatOfPoint2f? {
    val contours = ArrayList<MatOfPoint>()
    Imgproc.findContours(src, contours, Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)

    // Sort contours by size, largest first.
    sort(contours) { lhs, rhs ->
      val area1 = Imgproc.contourArea(lhs)
      val area2 = Imgproc.contourArea(rhs)
      (area2.toInt() - area1.toInt())
    }

    if (enableDebug) {
      val outMat = Mat(src.rows(), src.cols(), CvType.CV_8UC4)
      outMat.setTo(Scalar(0.0, 0.0, 0.0, 255.0))
      for (i in 0..min(7, contours.size)) {
        Imgproc.drawContours(outMat, contours, i, contourColors[i], 10)
      }

      val outBmp =
        Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888)
      matToBitmap(outMat, outBmp)
      withContext(Dispatchers.Main) {
        debugContoursBmp.value = outBmp
        outMat.release()
      }
    }

    //if (contours.size > 5) {
    //  contours.subList(4, contours.size - 1).clear()
   // }
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

    private val contourColors = arrayOf(
      Scalar(255.0, 255.0, 255.0, 255.0),
      Scalar(0.0, 255.0, 255.0, 255.0),
      Scalar(255.0, 0.0, 255.0, 255.0),
      Scalar(255.0, 255.0, 0.0, 255.0),
      Scalar(0.0, 0.0, 255.0, 255.0),
      Scalar(0.0, 255.0, 0.0, 255.0),
      Scalar(255.0, 0.0, 0.0, 255.0),
      Scalar(0.0, 255.0, 255.0, 255.0),
    )
  }
}