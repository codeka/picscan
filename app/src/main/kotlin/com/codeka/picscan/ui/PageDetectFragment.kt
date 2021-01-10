package com.codeka.picscan.ui

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.codeka.picscan.R
import com.codeka.picscan.databinding.FragmentPageDetectBinding
import com.codeka.picscan.model.Page
import com.codeka.picscan.ui.viewmodel.PageViewModel
import com.codeka.picscan.ui.viewmodel.ProjectViewModel

/**
 * PageDetectFragment takes the photo from a [Page] and uses OpenCV to figure out where the edges
 * of the page are. It also allows the user to adjust the edges manually if we don't get it exactly
 * right.
 */
class PageDetectFragment : Fragment() {
  private lateinit var binding: FragmentPageDetectBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    binding = FragmentPageDetectBinding.inflate(inflater, container, false)

    val projectViewModel: ProjectViewModel by navGraphViewModels(R.id.nav_graph)
    val pageViewModel: PageViewModel by navGraphViewModels(R.id.nav_graph)

    binding.lifecycleOwner = viewLifecycleOwner
    binding.project = projectViewModel
    binding.page = pageViewModel

    binding.next.setOnClickListener {
      pageViewModel?.transformCorners()
      findNavController().navigate(PageDetectFragmentDirections.toColorFilterFragment())
    }

    return binding.root
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    inflater.inflate(R.menu.page_detect_menu, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.action_show_debug -> {
        val pageViewModel: PageViewModel by navGraphViewModels(R.id.nav_graph)
        pageViewModel.debugBmp.observe(viewLifecycleOwner) {
          binding.image.setImageBitmap(it)
        }
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }
}
