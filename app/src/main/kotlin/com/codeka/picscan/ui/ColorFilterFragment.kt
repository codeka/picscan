package com.codeka.picscan.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import com.codeka.picscan.R
import com.codeka.picscan.databinding.FragmentColorFilterBinding
import com.codeka.picscan.ui.viewmodel.ImageFilterType
import com.codeka.picscan.ui.viewmodel.PageViewModel
import com.codeka.picscan.ui.viewmodel.ProjectViewModel

/**
 * A simple [Fragment] subclass.
 * Use the [ColorFilterFragment.newInstance] factory method to
 * create an instance of this fragment.
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
      // TODO: handle finish button
    }

    return binding.root
  }
}