package com.codeka.picscan.ui.view

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.codeka.picscan.model.PageCorners

@BindingAdapter("app:bitmap")
fun setBitmap(view: ImageView, bmp: Bitmap?) {
  view.setImageBitmap(bmp)
}

@BindingAdapter("app:corners")
fun setCorners(view: PageCornerEditorView, corners: PageCorners?) {
  view.setCorners(corners)
}
