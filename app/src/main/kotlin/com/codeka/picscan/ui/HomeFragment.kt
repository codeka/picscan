package com.codeka.picscan.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.codeka.picscan.R
import com.codeka.picscan.databinding.FragmentHomeBinding
import com.codeka.picscan.ui.viewmodel.ProjectViewModel

/**
 * The home fragment that shows a list of all your past projects and lets you start new ones.
 */
class HomeFragment : Fragment() {
  private lateinit var binding: FragmentHomeBinding

  override fun onCreateView(
      inflater: LayoutInflater, container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    binding = FragmentHomeBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.fab.setOnClickListener {
      if (!cameraPermissionsGranted()) {
        requestPermissions(CAMERA_REQUIRED_PERMISSIONS, CAMERA_PERMISSIONS_REQUEST_CODE)
      } else {
        createProject()
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
        createProject()
      } else {
        Toast.makeText(
          requireContext(), "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun createProject() {
    val vm: ProjectViewModel by navGraphViewModels(R.id.nav_graph)
    vm.create()

    findNavController().navigate(HomeFragmentDirections.toCameraFragment())
  }

  private fun cameraPermissionsGranted() = CAMERA_REQUIRED_PERMISSIONS.all {
    ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
  }

  private companion object {
    private const val CAMERA_PERMISSIONS_REQUEST_CODE = 1934;
    private val CAMERA_REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
  }
}