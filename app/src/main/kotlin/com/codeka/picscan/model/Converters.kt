package com.codeka.picscan.model

import android.graphics.PointF
import androidx.room.TypeConverter
import java.util.*

class Converters {
  @TypeConverter
  fun fromPointF(value: PointF?): String? {
    return value?.let { String.format(Locale.US, "%.6f,%.6f", it.x, it.y) }
  }

  @TypeConverter
  fun toPointF(value: String?): PointF? {
    return value?.let {
      val parts = it.split(',')
      return PointF(parts[0].toFloat(), parts[1].toFloat())
    }
  }
}
