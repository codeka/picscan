package com.codeka.picscan.ui

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
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
import com.codeka.picscan.model.Project
import com.codeka.picscan.model.ProjectRepository
import com.codeka.picscan.ui.viewmodel.ProjectViewModel
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
    private val projects: LiveData<List<Project>>, lifecycleOwner: LifecycleOwner,
    private val clickListener: (Project) -> Unit
  ) : RecyclerView.Adapter<PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
      val binding = ProjectRowBinding.inflate(LayoutInflater.from(parent.context))
      return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
      val project = projects.value?.get(position) ?: return

      Log.i("DEANH", "binding ${project.name} and ${project.createDate}")
      if (project.name == "") {
        holder.binding.projectName.text = project.name
      } else {
        holder.binding.projectName.text = "Unknown";
      }
      holder.binding.projectDate.text =
        Date(project.createDate * 1000L).toString()
      holder.binding.root.setOnClickListener { clickListener(project) }
    }

    override fun getItemCount(): Int {
      Log.i("DEANH", "getItemCount() = ${projects.value?.size ?: -1}")
      return projects.value?.size ?: 0
    }

    init {
      projects.observe(lifecycleOwner) {
        notifyDataSetChanged()
      }
    }
  }

  class PageViewHolder(val binding: ProjectRowBinding)
    : RecyclerView.ViewHolder(binding.root) {
  }

  private companion object {
    private const val CAMERA_PERMISSIONS_REQUEST_CODE = 1934;
    private val CAMERA_REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
  }
}