package com.github.dedis.popstellar.ui.detail.event.consensus;

import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.GridView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.ElectionStartFragmentBinding;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.ElectInstance.State;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.ElectionRepository;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.utility.error.*;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;

/**
 * A simple {@link Fragment} subclass. Use the {@link ElectionStartFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
@AndroidEntryPoint
public class ElectionStartFragment extends Fragment {

  private static final String TAG = ElectionStartFragment.class.getSimpleName();
  private static final String ELECTION_ID = "election_id";
  public static final String CONSENSUS_TYPE = "election";
  public static final String CONSENSUS_PROPERTY = "state";

  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z", Locale.getDefault());

  private final CompositeDisposable disposables = new CompositeDisposable();

  private ConsensusNode ownNode;

  @Inject ElectionRepository electionRepo;
  private com.github.dedis.popstellar.databinding.ElectionStartFragmentBinding binding;
  private NodesAcceptorAdapter adapter;

  public ElectionStartFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment ElectionStartFragment.
   */
  public static ElectionStartFragment newInstance(String electionId) {
    ElectionStartFragment fragment = new ElectionStartFragment();
    Bundle bundle = new Bundle();
    bundle.putString(ELECTION_ID, electionId);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    binding = ElectionStartFragmentBinding.inflate(inflater, container, false);

    LaoDetailViewModel viewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    String electionId = requireArguments().getString(ELECTION_ID);

    try {
      Observable<List<ConsensusNode>> nodes = viewModel.getNodes().observeOn(mainThread());
      Observable<Election> election =
          electionRepo
              .getElectionObservable(viewModel.getLaoId(), electionId)
              .observeOn(mainThread());

      Observable<ElectionNodesState> merged =
          Observable.combineLatest(nodes, election, ElectionNodesState::new);

      subscribeTo(nodes, this::updateNodes);
      subscribeTo(election, this::updateElection);
      subscribeTo(merged, this::updateNodesAndElection);
    } catch (UnknownElectionException | UnknownLaoException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.generic_error);
      return null;
    }

    setupButtonListeners(viewModel, electionId);

    try {
      LaoView laoView = viewModel.getLaoView();
      ownNode = laoView.getNode(viewModel.getPublicKey());

      if (ownNode == null) {
        // Only possible if the user wasn't an acceptor, but shouldn't have access to this fragment
        Log.e(TAG, "Couldn't find the Node with public key : " + viewModel.getPublicKey());
        throw new IllegalStateException(
            "Only acceptors are allowed to access ElectionStartFragment");
      }

      String instanceId = ElectInstance.generateConsensusId("election", electionId, "state");
      adapter = new NodesAcceptorAdapter(ownNode, instanceId, getViewLifecycleOwner(), viewModel);
      GridView gridView = binding.nodesGrid;
      gridView.setAdapter(adapter);
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, R.string.error_no_lao);
      return null;
    }

    binding.setLifecycleOwner(getViewLifecycleOwner());

    return binding.getRoot();
  }

  private <T> void subscribeTo(Observable<T> observable, Consumer<T> onNext) {
    disposables.add(
        observable.subscribe(
            onNext,
            err -> ErrorUtils.logAndShow(requireContext(), TAG, err, R.string.generic_error)));
  }

  private void updateNodes(List<ConsensusNode> nodes) {
    adapter.setList(nodes);
  }

  private void updateElection(Election election) {
    if (isElectionStartTimePassed(election)) {
      binding.electionStatus.setText(R.string.ready_to_start);
      binding.electionStart.setText(R.string.start_election);
      binding.electionStart.setEnabled(true);
    } else {
      String scheduledDate = dateFormat.format(new Date(election.getStartTimestampInMillis()));
      binding.electionStatus.setText(R.string.waiting_scheduled_time);
      binding.electionStart.setText(getString(R.string.election_scheduled, scheduledDate));
      binding.electionStart.setEnabled(false);
    }

    binding.electionTitle.setText(getString(R.string.election_start_title, election.getName()));
  }

  private void updateNodesAndElection(ElectionNodesState electionNodesState) {
    Election election = electionNodesState.election;
    List<ConsensusNode> nodes = electionNodesState.nodes;

    String instanceId = ElectInstance.generateConsensusId("election", election.getId(), "state");

    if (isElectionStartTimePassed(election)) {
      updateStartAndStatus(nodes, election, instanceId);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    disposables.dispose();
  }

  private boolean isElectionStartTimePassed(Election election) {
    return Instant.now().getEpochSecond() >= election.getStartTimestamp();
  }

  private void setupButtonListeners(LaoDetailViewModel mLaoDetailViewModel, String electionId) {
    binding.electionStart.setOnClickListener(
        clicked ->
            mLaoDetailViewModel.addDisposable(
                mLaoDetailViewModel
                    .sendConsensusElect(
                        Instant.now().getEpochSecond(),
                        electionId,
                        CONSENSUS_TYPE,
                        CONSENSUS_PROPERTY,
                        "started")
                    .subscribe(
                        msg -> {},
                        error ->
                            ErrorUtils.logAndShow(
                                requireContext(), TAG, error, R.string.error_start_election))));
  }

  private void updateStartAndStatus(
      List<ConsensusNode> nodes, Election election, String instanceId) {
    boolean isAnyElectInstanceAccepted =
        nodes.stream()
            .map(node -> node.getLastElectInstance(instanceId))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(ElectInstance::getState)
            .anyMatch(State.ACCEPTED::equals);

    if (isAnyElectInstanceAccepted) {
      // assuming the election start time was updated from scheduled to real start time
      String startedDate = dateFormat.format(new Date(election.getStartTimestampInMillis()));
      binding.electionStatus.setText(R.string.started);
      binding.electionStart.setText(getString(R.string.election_started_at, startedDate));
      binding.electionStart.setEnabled(false);
    } else {
      State ownState = ownNode.getState(instanceId);
      boolean canClick = ownState == State.WAITING || ownState == State.FAILED;
      binding.electionStart.setEnabled(canClick);
    }
  }

  /** Just pack the latest election and the nodes into one object */
  private static class ElectionNodesState {
    private final List<ConsensusNode> nodes;
    private final Election election;

    private ElectionNodesState(List<ConsensusNode> nodes, Election election) {
      this.nodes = nodes;
      this.election = election;
    }
  }
}
