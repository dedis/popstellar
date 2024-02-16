package com.github.dedis.popstellar.ui.lao.event.consensus

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.ElectionStartFragmentBinding
import com.github.dedis.popstellar.model.objects.Channel.Companion.getLaoChannel
import com.github.dedis.popstellar.model.objects.ConsensusNode
import com.github.dedis.popstellar.model.objects.ElectInstance
import com.github.dedis.popstellar.model.objects.ElectInstance.Companion.generateConsensusId
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainConsensusViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.Optional
import javax.inject.Inject
import timber.log.Timber

/**
 * A simple [Fragment] subclass. Use the [ElectionStartFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@AndroidEntryPoint
class ElectionStartFragment : Fragment() {
  private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss z", Locale.getDefault())
  private val disposables = CompositeDisposable()

  private var ownNode: ConsensusNode? = null

  private lateinit var laoViewModel: LaoViewModel
  private lateinit var consensusViewModel: ConsensusViewModel

  @Inject lateinit var electionRepo: ElectionRepository

  private lateinit var binding: ElectionStartFragmentBinding
  private lateinit var adapter: NodesAcceptorAdapter

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    binding = ElectionStartFragmentBinding.inflate(inflater, container, false)

    laoViewModel = obtainViewModel(requireActivity())
    val laoId = laoViewModel.laoId!!
    consensusViewModel = obtainConsensusViewModel(requireActivity(), laoId)

    val electionId = requireArguments().getString(ELECTION_ID)!!
    try {
      val nodes =
          consensusViewModel
              .getNodesByChannel(getLaoChannel(laoId))
              .observeOn(AndroidSchedulers.mainThread())
      val election = electionRepo.getElectionObservable(laoViewModel.laoId!!, electionId)

      val merged =
          Observable.combineLatest(nodes, election) {
              consensusNodes: List<ConsensusNode>,
              electionState: Election ->
            ElectionNodesState(consensusNodes, electionState)
          }
      subscribeTo(nodes) { consensusNodes: List<ConsensusNode> -> updateNodes(consensusNodes) }
      subscribeTo(election) { electionState: Election -> updateElection(electionState) }
      subscribeTo(merged) { electionNodesState: ElectionNodesState ->
        updateNodesAndElection(electionNodesState)
      }
    } catch (e: UnknownElectionException) {
      logAndShow(requireContext(), TAG, e, R.string.generic_error)
      return null
    }

    setupButtonListeners(electionId)

    ownNode = consensusViewModel.getNodeByLao(laoId, laoViewModel.getPublicKey())
    if (ownNode == null) {
      // Only possible if the user wasn't an acceptor, but shouldn't have access to this fragment
      Timber.tag(TAG).e("Couldn't find the Node with public key : %s", laoViewModel.getPublicKey())
      error("Only acceptors are allowed to access ElectionStartFragment")
    }

    val instanceId = generateConsensusId(CONSENSUS_TYPE, electionId, CONSENSUS_PROPERTY)
    adapter =
        NodesAcceptorAdapter(
            ownNode!!, instanceId, viewLifecycleOwner, laoViewModel, consensusViewModel)
    val gridView = binding.nodesGrid
    gridView.adapter = adapter
    binding.lifecycleOwner = viewLifecycleOwner

    return binding.root
  }

  private fun <T> subscribeTo(observable: Observable<T>, onNext: Consumer<T>) {
    disposables.add(
        observable.subscribe(onNext) { err: Throwable ->
          logAndShow(requireContext(), TAG, err, R.string.generic_error)
        })
  }

  private fun updateNodes(nodes: List<ConsensusNode>) {
    adapter.setList(nodes)
  }

  private fun updateElection(election: Election) {
    if (isElectionStartTimePassed(election)) {
      binding.electionStatus.setText(R.string.ready_to_start)
      binding.electionStart.setText(R.string.start_election)
      binding.electionStart.isEnabled = true
    } else {
      val scheduledDate = dateFormat.format(Date(election.startTimestampInMillis))
      binding.electionStatus.setText(R.string.waiting_scheduled_time)
      binding.electionStart.text = getString(R.string.election_scheduled, scheduledDate)
      binding.electionStart.isEnabled = false
    }

    binding.electionTitle.text = getString(R.string.election_start_title, election.name)
  }

  private fun updateNodesAndElection(electionNodesState: ElectionNodesState) {
    val election = electionNodesState.election
    val nodes = electionNodesState.nodes
    val instanceId = generateConsensusId(CONSENSUS_TYPE, election.id, CONSENSUS_PROPERTY)

    if (isElectionStartTimePassed(election)) {
      updateStartAndStatus(nodes, election, instanceId)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    disposables.dispose()
  }

  private fun isElectionStartTimePassed(election: Election): Boolean {
    return Instant.now().epochSecond >= election.startTimestamp
  }

  private fun setupButtonListeners(electionId: String) {
    binding.electionStart.setOnClickListener {
      laoViewModel.addDisposable(
          consensusViewModel
              .sendConsensusElect(
                  Instant.now().epochSecond,
                  electionId,
                  CONSENSUS_TYPE,
                  CONSENSUS_PROPERTY,
                  "started")
              .subscribe(
                  {},
                  { error: Throwable ->
                    logAndShow(requireContext(), TAG, error, R.string.error_start_election)
                  }))
    }
  }

  private fun updateStartAndStatus(
      nodes: List<ConsensusNode>,
      election: Election,
      instanceId: String
  ) {
    val isAnyElectInstanceAccepted =
        nodes
            .stream()
            .map { node: ConsensusNode -> node.getLastElectInstance(instanceId) }
            .filter { obj: Optional<ElectInstance> -> obj.isPresent }
            .map { obj: Optional<ElectInstance> -> obj.get() }
            .map(ElectInstance::state)
            .anyMatch { other: ElectInstance.State -> ElectInstance.State.ACCEPTED == other }

    if (isAnyElectInstanceAccepted) {
      // assuming the election start time was updated from scheduled to real start time
      val startedDate = dateFormat.format(Date(election.startTimestampInMillis))
      binding.electionStatus.setText(R.string.started)
      binding.electionStart.text = getString(R.string.election_started_at, startedDate)
      binding.electionStart.isEnabled = false
    } else {
      val ownState = ownNode!!.getState(instanceId)
      val canClick =
          ownState === ElectInstance.State.WAITING || ownState === ElectInstance.State.FAILED

      binding.electionStart.isEnabled = canClick
    }
  }

  /** Just pack the latest election and the nodes into one object */
  private class ElectionNodesState(val nodes: List<ConsensusNode>, val election: Election)

  companion object {
    private val TAG = ElectionStartFragment::class.java.simpleName
    private const val ELECTION_ID = "election_id"
    const val CONSENSUS_TYPE = "election"
    const val CONSENSUS_PROPERTY = "state"

    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @return A new instance of fragment ElectionStartFragment.
     */
    @JvmStatic
    fun newInstance(electionId: String): ElectionStartFragment {
      val fragment = ElectionStartFragment()
      val bundle = Bundle()
      bundle.putString(ELECTION_ID, electionId)
      fragment.arguments = bundle

      return fragment
    }
  }
}
