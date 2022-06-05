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
import com.github.dedis.popstellar.model.objects.ConsensusNode;
import com.github.dedis.popstellar.model.objects.ElectInstance;
import com.github.dedis.popstellar.model.objects.ElectInstance.State;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;

/**
 * A simple {@link Fragment} subclass. Use the {@link ElectionStartFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
@AndroidEntryPoint
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
    String instanceId = ElectInstance.generateConsensusId("election", electionId, "state");

    binding.electionTitle.setText(getString(R.string.election_start_title, election.getName()));
    electionStatus.setText(R.string.waiting_scheduled_time);
    electionStart.setText(getString(R.string.election_scheduled, scheduledDate));
    electionStart.setEnabled(false);

    setupTimerUpdate(election);

    setupButtonListeners(mLaoDetailViewModel, electionId);

    Lao lao = mLaoDetailViewModel.getCurrentLaoValue();
    List<ConsensusNode> nodes = lao.getNodes();
    ownNode = lao.getNode(mLaoDetailViewModel.getPublicKey());

    if (ownNode == null) {
      // Only possible if the user wasn't an acceptor, but shouldn't have access to this fragment
      Log.e(TAG, "Couldn't find the Node with public key : " + mLaoDetailViewModel.getPublicKey());
      throw new IllegalStateException("Only acceptors are allowed to access ElectionStartFragment");
    }

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

    binding.setLifecycleOwner(getViewLifecycleOwner());

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
      LaoDetailViewModel mLaoDetailViewModel,
      String electionId) {
    electionStart.setOnClickListener(
        clicked ->
            mLaoDetailViewModel.sendConsensusElect(
                Instant.now().getEpochSecond(), electionId, "election", "state", "started")
        );
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
      electionStatus.setText(R.string.started);
      electionStart.setText(getString(R.string.election_started_at, startedDate));
      electionStart.setEnabled(false);
    } else {
      State ownState = ownNode.getState(instanceId);
      boolean canClick = ownState == State.WAITING || ownState == State.FAILED;
      electionStart.setEnabled(canClick);
    }
  }
}
