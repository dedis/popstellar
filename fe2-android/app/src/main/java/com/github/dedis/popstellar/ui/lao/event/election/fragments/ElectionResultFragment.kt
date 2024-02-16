package com.github.dedis.popstellar.ui.lao.event.election.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.ElectionResultFragmentBinding
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallback
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.lao.event.election.adapters.ElectionResultPagerAdapter
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

@AndroidEntryPoint
class ElectionResultFragment : Fragment() {
  private lateinit var laoViewModel: LaoViewModel
  @Inject lateinit var electionRepository: ElectionRepository

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    // Inflate the layout for this fragment
    val binding = ElectionResultFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    val electionId = requireArguments().getString(ELECTION_ID)!!
    try {
      val laoView = laoViewModel.lao
      val election = electionRepository.getElection(laoViewModel.laoId!!, electionId)

      // Setting the Lao Name
      binding.electionResultLaoName.text = laoView.name
      // Setting election name
      binding.electionResultElectionTitle.text = election.name

      val adapter = ElectionResultPagerAdapter(laoViewModel, electionRepository, election.id)
      val viewPager2 = binding.electionResultPager
      viewPager2.adapter = adapter

      // Setting the circle indicator
      val circleIndicator = binding.swipeIndicatorElectionResults
      circleIndicator.setViewPager(viewPager2)
    } catch (e: Exception) {
      when (e) {
        is UnknownLaoException -> logAndShow(requireContext(), TAG, R.string.error_no_lao)
        is UnknownElectionException -> logAndShow(requireContext(), TAG, R.string.error_no_election)
        else -> throw e
      }
      Timber.tag(TAG).d(e)
      return null
    }

    binding.lifecycleOwner = viewLifecycleOwner
    handleBackNav()

    return binding.root
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.election_result_title)
    laoViewModel.setIsTab(false)
  }

  private fun handleBackNav() {
    addBackNavigationCallback(
        requireActivity(),
        viewLifecycleOwner,
        buildBackButtonCallback(TAG, "election") {
          ElectionFragment.openFragment(
              parentFragmentManager, requireArguments().getString(ELECTION_ID)!!)
        })
  }

  companion object {
    private val TAG = ElectionResultFragment::class.java.simpleName
    private const val ELECTION_ID = "election_id"

    @JvmStatic
    fun newInstance(electionId: String?): ElectionResultFragment {
      val fragment = ElectionResultFragment()
      val bundle = Bundle()
      bundle.putString(ELECTION_ID, electionId)
      fragment.arguments = bundle

      return fragment
    }
  }
}
