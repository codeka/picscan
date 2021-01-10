package com.codeka.picscan

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * PageDetectFragment takes the photo from a [Page] and uses OpenCV to figure out where the edges
 * of the page are. It also allows the user to adjust the edges manually if we don't get it exactly
 * right.
 */
class PageDetectFragment : Fragment() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_page_detect, container, false)
  }
}