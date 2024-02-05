package com.github.dedis.popstellar.ui.lao.event.election.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.CastVoteFragmentBinding
import com.github.dedis.popstellar.model.network.method.message.data.election.PlainVote
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallback
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainElectionViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.lao.event.election.ElectionViewModel
import com.github.dedis.popstellar.ui.lao.event.election.ZoomOutTransformer
import com.github.dedis.popstellar.ui.lao.event.election.adapters.CastVoteViewPagerAdapter
import com.github.dedis.popstellar.utility.ActivityUtils.buildBackButtonCallback
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

/**
 * A simple [Fragment] subclass. Use the [CastVoteFragment.newInstance] factory method to create an
 * instance of this fragment.
 */
@AndroidEntryPoint
class CastVoteFragment : Fragment() {
  @Inject lateinit var electionRepository: ElectionRepository

  private lateinit var laoViewModel: LaoViewModel
  private lateinit var electionViewModel: ElectionViewModel

  private lateinit var binding: CastVoteFragmentBinding
  private lateinit var electionId: String

  private val votes: MutableMap<String, Int> = HashMap()

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    // Inflate the layout for this fragment
    binding = CastVoteFragmentBinding.inflate(inflater, container, false)

    electionId = requireArguments().getString(ELECTION_ID)!!

    laoViewModel = obtainViewModel(requireActivity())
    electionViewModel = obtainElectionViewModel(requireActivity(), laoViewModel.laoId!!)

    // Setting the lao ad election name
    if (setLaoName() || setElectionName()) {
      return null
    }

    try {
      val election = electionRepository.getElection(laoViewModel.laoId!!, electionId)

      // Setting the viewPager and its adapter
      val pager = binding.castVotePager
      val adapter = CastVoteViewPagerAdapter(binding, election, votes)
      pager.adapter = adapter
      pager.setPageTransformer(ZoomOutTransformer())

      // Setting the indicator for horizontal swipe
      val circleIndicator = binding.swipeIndicator
      circleIndicator.setViewPager(pager)
    } catch (err: UnknownElectionException) {
      logAndShow(requireContext(), TAG, err, R.string.generic_error)
      return null
    }

    // setUp the cast Vote button
    binding.castVoteButton.setOnClickListener { voteButton: View -> castVote(voteButton) }
    setEncryptionVotes()
    handleBackNav()

    return binding.root
  }

  /**
   * Show the progress bar and block user's touch inputs if the encryption of the vote takes time
   */
  private fun setEncryptionVotes() {
    // observe the progress for encryption
    electionViewModel.isEncrypting.observe(viewLifecycleOwner) { isEncrypting: Boolean ->
      // Block touch inputs if loading and display progress bar
      if (java.lang.Boolean.TRUE == isEncrypting) {
        binding.loadingContainer.visibility = View.VISIBLE
        requireActivity()
            .window
            .setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
      } else {
        binding.loadingContainer.visibility = View.GONE
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
      }
    }
  }

  private fun setLaoName(): Boolean {
    return try {
      val laoView = laoViewModel.lao
      binding.castVoteLaoName.text = laoView.name
      false
    } catch (e: UnknownLaoException) {
      Timber.tag(TAG).d(e)
      logAndShow(requireContext(), TAG, R.string.error_no_lao)
      true
    }
  }

  private fun setElectionName(): Boolean {
    return try {
      val election = electionRepository.getElection(laoViewModel.laoId!!, electionId)
      binding.castVoteElectionName.text = election.name
      false
    } catch (e: UnknownElectionException) {
      Timber.tag(TAG).d(e)
      logAndShow(requireContext(), TAG, R.string.error_no_election)
      true
    }
  }

  private fun castVote(voteButton: View) {
    voteButton.isEnabled = false
    val plainVotes: MutableList<PlainVote> = ArrayList()
    try {
      val election = electionRepository.getElection(laoViewModel.laoId!!, electionId)
      val electionQuestions = election.electionQuestions

      // Attendee should not be able to send cast vote if he didn't vote for all questions
      if (votes.size < electionQuestions.size) {
        return
      }

      for (electionQuestion in electionQuestions) {
        val plainVote =
            PlainVote(
                electionQuestion.id,
                votes[electionQuestion.id],
                electionQuestion.writeIn,
                null,
                electionId)
        plainVotes.add(plainVote)
      }

      laoViewModel.addDisposable(
          electionViewModel
              .sendVote(electionId, plainVotes)
              .subscribe(
                  {
                    Toast.makeText(requireContext(), R.string.vote_sent, Toast.LENGTH_LONG).show()
                  },
                  { err: Throwable ->
                    logAndShow(requireContext(), TAG, err, R.string.error_send_vote)
                  }))
    } catch (err: UnknownElectionException) {
      logAndShow(requireContext(), TAG, err, R.string.generic_error)
    } finally {
      voteButton.isEnabled = true
    }
  }

  override fun onResume() {
    super.onResume()
    laoViewModel.setPageTitle(R.string.vote)
    laoViewModel.setIsTab(false)
  }

  private fun handleBackNav() {
    addBackNavigationCallback(
        requireActivity(),
        viewLifecycleOwner,
        buildBackButtonCallback(TAG, "election") {
          ElectionFragment.openFragment(parentFragmentManager, arguments?.getString(ELECTION_ID)!!)
        })
  }

  companion object {
    val TAG: String = CastVoteFragment::class.java.simpleName
    private const val ELECTION_ID = "election_id"

    @JvmStatic
    fun newInstance(electionId: String?): CastVoteFragment {
      val fragment = CastVoteFragment()
      val bundle = Bundle()
      bundle.putString(ELECTION_ID, electionId)
      fragment.arguments = bundle

      return fragment
    }
  }
}
