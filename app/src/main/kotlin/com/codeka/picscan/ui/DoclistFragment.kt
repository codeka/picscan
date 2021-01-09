package com.codeka.picscan.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.codeka.picscan.R
import com.codeka.picscan.databinding.FragmentDoclistBinding
import com.google.android.material.snackbar.Snackbar

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class DoclistFragment : Fragment() {
  private var _binding: FragmentDoclistBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(
      inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    _binding = FragmentDoclistBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.buttonFirst.setOnClickListener {
      findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
    }

    binding.fab.setOnClickListener {
      if (!cameraPermissionsGranted()) {
        requestPermissions(CAMERA_REQUIRED_PERMISSIONS, CAMERA_PERMISSIONS_REQUEST_CODE)
      } else {
        startCamera()
      }
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    if (requestCode == CAMERA_PERMISSIONS_REQUEST_CODE) {
      if (cameraPermissionsGranted()) {
        startCamera()
      } else {
        Toast.makeText(
          requireContext(), "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun startCamera() {
    findNavController().navigate(R.id.action_DoclistFragment_to_cameraFragment)
  }

  private fun cameraPermissionsGranted() = CAMERA_REQUIRED_PERMISSIONS.all {
    ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }


  private companion object {
    private const val CAMERA_PERMISSIONS_REQUEST_CODE = 1934;
    private val CAMERA_REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
  }
}