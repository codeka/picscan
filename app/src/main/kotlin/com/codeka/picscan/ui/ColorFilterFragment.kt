package com.codeka.picscan.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.codeka.picscan.R
import com.codeka.picscan.databinding.FragmentColorFilterBinding
import com.codeka.picscan.model.ImageFilterType
import com.codeka.picscan.ui.viewmodel.PageViewModel
import com.codeka.picscan.ui.viewmodel.ProjectViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * This fragment lets you modify the color filtering of the image. We also let you pan & zoom the
 * image to get a better look.
 */
class ColorFilterFragment : Fragment() {
  private lateinit var binding: FragmentColorFilterBinding

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View {
    binding = FragmentColorFilterBinding.inflate(inflater, container, false)

    val projectViewModel: ProjectViewModel by navGraphViewModels(R.id.nav_graph)
    val pageViewModel: PageViewModel by navGraphViewModels(R.id.nav_graph)

    binding.lifecycleOwner = viewLifecycleOwner
    binding.project = projectViewModel
    binding.page = pageViewModel

    binding.filterNone.setOnClickListener {
      pageViewModel.filterImage(ImageFilterType.None)
    }
    binding.filterTest.setOnClickListener {
      pageViewModel.filterImage(ImageFilterType.Test)
    }

    binding.finish.setOnClickListener {
      CoroutineScope(Dispatchers.Main).launch {
        if (pageViewModel.page?.id == 0L) {
          projectViewModel.addPage(pageViewModel.page!!)
          projectViewModel.project.value?.project?.draft = false
          projectViewModel.save()
        }
        pageViewModel.save()

        findNavController().navigate(ColorFilterFragmentDirections.toProjectFragment())
      }
    }

    return binding.root
  }
}