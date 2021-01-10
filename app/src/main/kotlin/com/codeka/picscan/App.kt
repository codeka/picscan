package com.codeka.picscan

import android.app.Application

class MyApp : Application() {

  init {
    App = this
  }

  override fun onCreate() {
    super.onCreate()

    // TODO??
  }
}

lateinit var App : MyApp