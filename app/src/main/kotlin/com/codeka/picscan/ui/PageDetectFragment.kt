package com.codeka.picscan.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import com.codeka.picscan.R
import com.codeka.picscan.databinding.FragmentPageDetectBinding
import com.codeka.picscan.model.Page
import com.codeka.picscan.ui.viewmodel.ProjectViewModel

/**
 * PageDetectFragment takes the photo from a [Page] and uses OpenCV to figure out where the edges
 * of the page are. It also allows the user to adjust the edges manually if we don't get it exactly
 * right.
 */
class PageDetectFragment : Fragment() {
  private lateinit var binding: FragmentPageDetectBinding

  private lateinit var args: PageDetectFragmentArgs

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    args = PageDetectFragmentArgs.fromBundle(requireArguments())
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentPageDetectBinding.inflate(inflater, container, false)

    val projectViewModel: ProjectViewModel by navGraphViewModels(R.id.nav_graph)
    val pageViewModel = projectViewModel.getPageViewModel(args.pageId)

    binding.lifecycleOwner = viewLifecycleOwner
    binding.project = projectViewModel
    binding.page = pageViewModel
    return binding.root
  }
}
