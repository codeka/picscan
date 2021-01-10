package com.codeka.picscan

import android.app.Application
import org.opencv.android.OpenCVLoader

class MyApp : Application() {

  init {
    App = this
  }

  override fun onCreate() {
    super.onCreate()

    OpenCVLoader.initDebug()
  }
}

lateinit var App : MyApp