package com.codeka.picscan.ui

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codeka.picscan.App
import com.codeka.picscan.R
import com.codeka.picscan.databinding.FragmentHomeBinding
import com.codeka.picscan.databinding.ProjectRowBinding
import com.codeka.picscan.model.*
import com.codeka.picscan.ui.viewmodel.ProjectViewModel
import com.squareup.picasso.Picasso
import java.util.*


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

    val repo = ProjectRepository.create(App)
    binding.projects.adapter = PageViewAdapter(repo.getAll(), this, {
      findNavController().navigate(HomeFragmentDirections.toProjectFragment(projectId = it.id))
    })
    binding.projects.layoutManager = LinearLayoutManager(requireContext())
    binding.projects.addItemDecoration(
      DividerItemDecoration(requireContext(), DividerItemDecoration.HORIZONTAL))

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
          requireContext(), "Permissions not granted by the user.", Toast.LENGTH_SHORT
        ).show()
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

  private class PageViewAdapter(
    private val projects: LiveData<List<Project>>, private val lifecycleOwner: LifecycleOwner,
    private val clickListener: (Project) -> Unit
  ) : RecyclerView.Adapter<PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
      val binding = ProjectRowBinding.inflate(LayoutInflater.from(parent.context))
      return PageViewHolder(binding, parent.resources.displayMetrics)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
      val project = projects.value?.get(position) ?: return

      if (project.name != "") {
        holder.binding.projectName.text = project.name
      } else {
        holder.binding.projectName.text = "Unknown";
      }
      holder.binding.projectDate.text =
        Date(project.createDate * 1000L).toString()
      if (project.needsPreviewRegenerate()) {
        // We manually calculate the size because the width of the view may still be zero at
        // this point. TODO: figure out how to make it so we don't have to keep the values in
        // in-sync manually.
        val sizeInPx =
          TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 84f, holder.displayMetrics)

        val vm = ProjectViewModel()
        vm.load(project.id)
        vm.generatePreview(lifecycleOwner, sizeInPx.toInt())
      } else {
        Picasso.get().load(project.previewFile()).into(holder.binding.projectPreview)
      }
      holder.binding.root.setOnClickListener { clickListener(project) }
    }

    override fun getItemCount(): Int {
      return projects.value?.size ?: 0
    }

    init {
      projects.observe(lifecycleOwner) {
        notifyDataSetChanged()
      }
    }
  }

  class PageViewHolder(val binding: ProjectRowBinding, val displayMetrics: DisplayMetrics)
    : RecyclerView.ViewHolder(binding.root) {
  }

  private companion object {
    private const val CAMERA_PERMISSIONS_REQUEST_CODE = 1934;
    private val CAMERA_REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
  }
}