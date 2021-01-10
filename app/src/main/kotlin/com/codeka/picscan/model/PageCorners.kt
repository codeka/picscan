package com.codeka.picscan.model

import android.graphics.PointF

/**
 * A data class that represents the corners of a page. This creates a convex polygon with exactly
 * four vertices.
 */
data class PageCorners(
  val topLeft: PointF = PointF(0.0f, 0.0f),
  val topRight: PointF = PointF(0.0f, 0.0f),
  val bottomRight: PointF = PointF(0.0f, 0.0f),
  val bottomLeft: PointF = PointF(0.0f, 0.0f))
