package com.codeka.picscan.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.codeka.picscan.R
import com.codeka.picscan.databinding.FragmentCameraBinding
import com.codeka.picscan.ui.viewmodel.ProjectViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/** [CameraFragment] lets you take a new picture to add to a document. */
class CameraFragment : Fragment() {
  private var _binding: FragmentCameraBinding? = null
  private val binding get() = _binding!!

  private var imageCapture: ImageCapture? = null
  private lateinit var outputDirectory: File

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    outputDirectory = File(requireContext().filesDir, "captures")
    outputDirectory.mkdirs()
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = FragmentCameraBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.cameraCaptureButton.setOnClickListener { capturePicture() }

    val cameraProviderFuture = ProcessCameraProvider.getInstance(requireActivity())
    cameraProviderFuture.addListener({
      val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

      val preview = Preview.Builder()
        .build()
        .also {
          it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        }

      imageCapture = ImageCapture.Builder()
        .build()

      // Select back camera as a default
      val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

      try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
      } catch(exc: Exception) {
        Log.e(TAG, "Use case binding failed", exc)
      }
    }, ContextCompat.getMainExecutor(requireContext()))
  }

  private fun capturePicture() {
    // Get a stable reference of the modifiable image capture use case
    val imageCapture = imageCapture ?: return

    // Create time-stamped output file to hold the image
    val photoFile = File(
      outputDirectory,
      SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis()) + ".jpg")

    // Create output options object which contains file + metadata
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    // Set up image capture listener, which is triggered after photo has been taken
    imageCapture.takePicture(
      outputOptions, ContextCompat.getMainExecutor(requireContext()),
      object : ImageCapture.OnImageSavedCallback {
        override fun onError(exc: ImageCaptureException) {
          val msg = "Photo capture failed: ${exc.message}"
          Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
          Log.e(TAG, msg)
        }

        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
          val savedUri = Uri.fromFile(photoFile)

          val vm: ProjectViewModel by navGraphViewModels(R.id.nav_graph)
          vm.addPhoto(savedUri).observe(viewLifecycleOwner) {
            findNavController().navigate(CameraFragmentDirections.toPageDetectFragment(it))
          }
        }
      })
  }

  private companion object {
    private const val TAG = "CameraFragment"
    private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
  }
}