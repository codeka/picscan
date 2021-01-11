package com.codeka.picscan.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.codeka.picscan.App
import com.codeka.picscan.R
import com.codeka.picscan.databinding.FragmentProjectBinding
import com.codeka.picscan.databinding.ProjectPageRowBinding
import com.codeka.picscan.export.PdfExporter
import com.codeka.picscan.model.ProjectWithPages
import com.codeka.picscan.ui.viewmodel.ProjectViewModel
import com.squareup.picasso.Picasso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * This fragment displays your project: a list of all the pages, some options you can change such
 * as the name and so on. And also lets you export the project to a PDF file.
 */
class ProjectFragment : Fragment() {
  private lateinit var binding: FragmentProjectBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View {
    binding = FragmentProjectBinding.inflate(inflater, container, false)

    val projectViewModel: ProjectViewModel by navGraphViewModels(R.id.nav_graph)
    projectViewModel.project.observe(viewLifecycleOwner) {
      CoroutineScope(Dispatchers.Main).launch {
        projectViewModel.save()
      }
    }
    projectViewModel.project.value

    binding.lifecycleOwner = viewLifecycleOwner
    binding.project = projectViewModel

    binding.pages.layoutManager = LinearLayoutManager(requireContext())
    binding.pages.adapter = PageViewAdapter(projectViewModel.project, viewLifecycleOwner)

    binding.newPage.setOnClickListener {
      findNavController().navigate(ProjectFragmentDirections.toCameraFragment())
    }
    binding.export.setOnClickListener {
      projectViewModel.export(requireContext(), this)
    }

    return binding.root
  }

  class PageViewAdapter(
    private val project: MutableLiveData<ProjectWithPages>, lifecycleOwner: LifecycleOwner)
    : RecyclerView.Adapter<PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
      val binding = ProjectPageRowBinding.inflate(LayoutInflater.from(parent.context))
      return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
      val dir = File(App.filesDir, "images")
      val file = File(dir, "%06d.jpg".format(project.value?.pages?.get(position)?.id ?: 0))
      Picasso.get().load(file).into(holder.binding.testView)
    }

    override fun getItemCount(): Int {
      return project.value?.pages?.size ?: 0
    }

    init {
      project.observe(lifecycleOwner) {
        notifyDataSetChanged()
      }
    }
  }

  class PageViewHolder(val binding: ProjectPageRowBinding)
    : RecyclerView.ViewHolder(binding.root) {
  }
}
