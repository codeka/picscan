package com.codeka.picscan.ui

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
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
import com.codeka.picscan.model.Page
import com.codeka.picscan.model.Project
import com.codeka.picscan.model.ProjectWithPages
import com.codeka.picscan.ui.viewmodel.PageViewModel
import com.codeka.picscan.ui.viewmodel.ProjectViewModel
import com.codeka.picscan.util.observeOnce
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

  private var args: ProjectFragmentArgs? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)

    val bundleArgs = arguments
    if (bundleArgs != null) {
      args = ProjectFragmentArgs.fromBundle(bundleArgs)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View {
    binding = FragmentProjectBinding.inflate(inflater, container, false)

    val projectViewModel: ProjectViewModel by navGraphViewModels(R.id.nav_graph)
    val args = this.args
    if (args?.projectId != null && args.projectId > 0) {
      projectViewModel.load(args.projectId)
    }
    projectViewModel.project.observe(viewLifecycleOwner) {
      CoroutineScope(Dispatchers.Main).launch {
        projectViewModel.save()
      }
    }

    binding.lifecycleOwner = viewLifecycleOwner
    binding.project = projectViewModel

    binding.pages.layoutManager = LinearLayoutManager(requireContext())
    binding.pages.adapter = PageViewAdapter(projectViewModel.project, {
      val pageViewModel: PageViewModel by navGraphViewModels(R.id.nav_graph)
      pageViewModel.reset(it)

      findNavController().navigate(ProjectFragmentDirections.toColorFilterFragment())
    }, viewLifecycleOwner)

    binding.newPage.setOnClickListener {
      findNavController().navigate(ProjectFragmentDirections.toCameraFragment())
    }
    binding.export.setOnClickListener {
      projectViewModel.export(requireContext(), this)
    }

    binding.projectName.setOnEditorActionListener {
      _, _, _ ->
        CoroutineScope(Dispatchers.Main).launch {
          projectViewModel.save()
        }
        false
      }

    return binding.root
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.project_menu, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_delete -> {
        maybeDeleteProject()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  /**
   * Asks if you want to delete the current project, and deletes it if you do.
   */
  private fun maybeDeleteProject() {
    AlertDialog.Builder(activity)
      .setMessage(R.string.delete_confirm_msg)
      .setCancelable(true)
      .setPositiveButton(R.string.delete) { _, _ ->
        val projectViewModel: ProjectViewModel by navGraphViewModels(R.id.nav_graph)
        projectViewModel.delete()

        findNavController().navigateUp()
      }
      .setNegativeButton(R.string.cancel) { dialog, _ ->
        dialog.dismiss()
      }
      .show()
  }

  class PageViewAdapter(
    private val project: MutableLiveData<ProjectWithPages>, private val clickListener: (Page) -> Unit, lifecycleOwner: LifecycleOwner)
    : RecyclerView.Adapter<PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
      val binding = ProjectPageRowBinding.inflate(LayoutInflater.from(parent.context))
      return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
      val dir = File(App.filesDir, "images")
      val file = File(dir, "%06d.jpg".format(project.value?.pages?.get(position)?.id ?: 0))
      Picasso.get().load(file).into(holder.binding.testView)
      holder.binding.root.setOnClickListener {
        val page = project.value?.pages?.get(position) ?: return@setOnClickListener
        clickListener(page)
      }
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
