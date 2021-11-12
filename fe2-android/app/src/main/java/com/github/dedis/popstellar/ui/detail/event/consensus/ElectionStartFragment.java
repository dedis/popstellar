package com.github.dedis.popstellar.ui.detail.event.consensus;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.ElectionStartFragmentBinding;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.ConsensusNode;
import com.github.dedis.popstellar.model.objects.ConsensusNode.State;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;

/**
 * A simple {@link Fragment} subclass. Use the {@link ElectionStartFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class ElectionStartFragment extends Fragment {

  private static final String TAG = ElectionStartFragment.class.getSimpleName();

  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z", Locale.getDefault());

  private final CompositeDisposable disposables = new CompositeDisposable();
  private ConsensusNode ownNode;
  private Button electionStart;
  private TextView electionStatus;

  public ElectionStartFragment() {
    // Required empty public constructor
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment ElectionStartFragment.
   */
  public static ElectionStartFragment newInstance() {
    return new ElectionStartFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    ElectionStartFragmentBinding binding =
        ElectionStartFragmentBinding.inflate(inflater, container, false);

    electionStart = binding.electionStart;
    electionStatus = binding.electionStatus;

    LaoDetailViewModel mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    Election election = mLaoDetailViewModel.getCurrentElection();
    if (election == null) {
      Log.e(TAG, "The current election of the LaoDetailViewModel is null");
      return null;
    }

    String scheduledDate = dateFormat.format(new Date(election.getStartTimestampInMillis()));
    String electionId = election.getId();
    String instanceId = Consensus.generateConsensusId("election", electionId, "state");

    binding.electionTitle.setText(getString(R.string.election_start_title, election.getName()));
    electionStatus.setText(R.string.waiting_scheduled_time);
    electionStart.setText(getString(R.string.election_scheduled, scheduledDate));
    electionStart.setEnabled(false);

    setupTimerUpdate(election);

    setupButtonListeners(binding, mLaoDetailViewModel, electionId, instanceId);

    List<ConsensusNode> nodes = mLaoDetailViewModel.getCurrentLaoValue().getNodes();

    ownNode = getOwnNode(mLaoDetailViewModel.getPublicKey(), nodes);

    NodesAcceptorAdapter adapter =
        new NodesAcceptorAdapter(
            nodes, ownNode, instanceId, getViewLifecycleOwner(), mLaoDetailViewModel);
    GridView gridView = binding.nodesGrid;
    gridView.setAdapter(adapter);

    if (isElectionStartTimePassed(election)) {
      updateStartAndStatus(nodes, election, instanceId);
    }

    mLaoDetailViewModel
        .getNodes()
        .observe(
            getViewLifecycleOwner(),
            consensusNodes -> {
              Log.d(TAG, "got an update for nodes : " + consensusNodes);
              adapter.setList(consensusNodes);
              if (isElectionStartTimePassed(election)) {
                updateStartAndStatus(consensusNodes, election, instanceId);
              }
            });

    binding.setLifecycleOwner(getActivity());

    return binding.getRoot();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    disposables.dispose();
  }

  private boolean isElectionStartTimePassed(Election election) {
    return Instant.now().getEpochSecond() >= election.getStartTimestamp();
  }

  private ConsensusNode getOwnNode(String ownPublicKey, List<ConsensusNode> nodes) {
    Optional<ConsensusNode> ownNodeOpt =
        nodes.stream().filter(node -> node.getPublicKey().equals(ownPublicKey)).findAny();
    if (ownNodeOpt.isPresent()) {
      return ownNodeOpt.get();
    } else {
      // Only possible if the user wasn't an acceptor, but shouldn't have access to this fragment
      Log.e(TAG, "Couldn't find our own Node with public key : " + ownPublicKey);
      throw new IllegalStateException("Only acceptors are allowed to access ElectionStartFragment");
    }
  }

  private void setupTimerUpdate(Election election) {
    // Check every seconds until the scheduled time, then show the "election start" button
    Disposable disposable =
        Observable.interval(0, 1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(
                new DisposableObserver<Long>() {
                  @Override
                  public void onNext(@NonNull Long aLong) {
                    if (isElectionStartTimePassed(election)) {
                      electionStatus.setText(R.string.ready_to_start);
                      electionStart.setText(R.string.start_election);
                      electionStart.setEnabled(true);
                      dispose();
                    }
                  }

                  @Override
                  public void onError(@NonNull Throwable e) {
                    Log.w(TAG, e);
                  }

                  @Override
                  public void onComplete() {
                    // Do nothing
                  }
                });
    disposables.add(disposable);
  }

  private void setupButtonListeners(
      ElectionStartFragmentBinding binding,
      LaoDetailViewModel mLaoDetailViewModel,
      String electionId,
      String instanceId) {
    electionStart.setOnClickListener(
        clicked -> {
          Optional<Consensus> acceptedConsensus =
              ownNode
                  .getLastConsensus(instanceId)
                  .filter(consensus -> consensus.canBeAccepted() && !consensus.isFailed());
          if (acceptedConsensus.isPresent()) {
            mLaoDetailViewModel.sendConsensusLearn(acceptedConsensus.get());
          } else {
            mLaoDetailViewModel.createNewConsensus(
                Instant.now().getEpochSecond(), electionId, "election", "state", "started");
          }
        });

    binding
        .backLayout
        .findViewById(R.id.tab_back)
        .setOnClickListener(clicked -> mLaoDetailViewModel.openLaoDetail());
  }

  private void updateStartAndStatus(
      List<ConsensusNode> nodes, Election election, String instanceId) {
    Optional<Consensus> acceptedConsensus =
        nodes.stream()
            .map(node -> node.getLastConsensus(instanceId))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(Consensus::isAccepted)
            .findAny();
    if (acceptedConsensus.isPresent()) {
      // assuming the election start time was updated from scheduled to real start time
      String startedDate = dateFormat.format(new Date(election.getStartTimestampInMillis()));
      electionStatus.setText(R.string.started);
      electionStart.setText(getString(R.string.election_started_at, startedDate));
      electionStart.setEnabled(false);
    } else {
      State ownState = ownNode.getState(instanceId);
      boolean canAccept =
          ownState == State.STARTING
              && ownNode.getLastConsensus(instanceId).map(Consensus::canBeAccepted).orElse(false);
      boolean canClick = canAccept || ownState == State.WAITING || ownState == State.FAILED;
      electionStart.setEnabled(canClick);
    }
  }
}
