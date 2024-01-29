package com.github.dedis.popstellar.ui.lao.event

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.UpcomingEventsFragmentBinding
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallbackToEvents
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainEventsEventsViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.lao.event.eventlist.UpcomingEventsAdapter

class UpcomingEventsFragment : Fragment() {
  private lateinit var laoViewModel: LaoViewModel

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    val binding = UpcomingEventsFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    val eventsViewModel = obtainEventsEventsViewModel(requireActivity(), laoViewModel.laoId!!)

    binding.upcomingEventsRecyclerView.layoutManager = LinearLayoutManager(context)
    binding.upcomingEventsRecyclerView.adapter =
      UpcomingEventsAdapter(eventsViewModel.events, laoViewModel, requireActivity(), TAG)
    handleBackNav()
    return binding.root
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.future_header_title)
    laoViewModel.setIsTab(false)
  }

  private fun handleBackNav() {
    addBackNavigationCallbackToEvents(requireActivity(), viewLifecycleOwner, TAG)
  }

  companion object {
    private val TAG = UpcomingEventsFragment::class.java.simpleName

    @JvmStatic
    fun newInstance(): UpcomingEventsFragment {
      return UpcomingEventsFragment()
    }
  }
}
