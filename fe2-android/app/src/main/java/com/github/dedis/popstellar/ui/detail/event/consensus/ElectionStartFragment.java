package com.github.dedis.popstellar.ui.detail.event.consensus;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
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
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A simple {@link Fragment} subclass. Use the {@link ElectionStartFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class ElectionStartFragment extends Fragment {

  private static final String TAG = ElectionStartFragment.class.getSimpleName();

  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z", Locale.ENGLISH);

  private final CompositeDisposable disposables = new CompositeDisposable();
  private List<ConsensusNode> nodes;
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
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    ElectionStartFragmentBinding binding =
        ElectionStartFragmentBinding.inflate(inflater, container, false);

    electionStart = binding.electionStart;
    electionStatus = binding.electionStatus;

    LaoDetailViewModel mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(getActivity());

    Election election = mLaoDetailViewModel.getCurrentElection();
    Lao lao = mLaoDetailViewModel.getCurrentLaoValue();

    String scheduledDate = dateFormat.format(new Date(election.getStartTimestamp() * 1000));
    String electionId = election.getId();

    binding.electionTitle.setText(getString(R.string.election_start_title, election.getName()));
    electionStatus.setText(R.string.waiting_scheduled_time);
    electionStart.setText(getString(R.string.election_scheduled, scheduledDate));
    electionStart.setEnabled(false);

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

    electionStart.setOnClickListener(
        clicked ->
            mLaoDetailViewModel.createNewConsensus(
                Instant.now().getEpochSecond(), electionId, "election", "state", "started"));

    String ownPublicKey = mLaoDetailViewModel.getPublicKey();
    Set<String> publicKeys = new HashSet<>(lao.getWitnesses());
    publicKeys.add(lao.getOrganizer());

    nodes = new ArrayList<>();
    for (String key : publicKeys) {
      nodes.add(new ConsensusNode(key));
    }
    ownNode =
        nodes.stream()
            .filter(node -> node.getPublicKey().equals(ownPublicKey))
            .findAny()
            .get(); // should always be present

    updateNodes(mLaoDetailViewModel.getCurrentLaoValue(), electionId);

    NodesAcceptorAdapter adapter =
        new NodesAcceptorAdapter(nodes, ownPublicKey, this, mLaoDetailViewModel);
    GridView gridView = binding.nodesGrid;
    gridView.setAdapter(adapter);

    if (isElectionStartTimePassed(election)) {
      updateApproved(lao, election, adapter);
    }

    mLaoDetailViewModel
        .getUpdateConsensusEvent()
        .observe(
            this,
            updatedConsensus -> {
              if (updatedConsensus.getKey().getId().equals(election.getId())) {
                // There was an update for at least one consensus for this election => update all
                updateNodes(lao, election.getId());
                adapter.notifyDataSetChanged();
                updateApproved(lao, election, adapter);
              }
            });

    binding
        .backLayout
        .findViewById(R.id.tab_back)
        .setOnClickListener(clicked -> mLaoDetailViewModel.openLaoDetail());

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

  // Update the state and consensus (if multiple, keep only last one) of all nodes
  private void updateNodes(Lao lao, String electionId) {
    Map<String, List<Consensus>> proposerToConsensuses =
        lao.getConsensuses().values().stream()
            .filter(consensus -> consensus.getKey().getId().equals(electionId))
            .sorted((c1, c2) -> (int) (c1.getCreation() - c2.getCreation()))
            .collect(Collectors.groupingBy(Consensus::getProposer));

    // If a node tried multiple times to start a consensus and failed, only consider last one
    proposerToConsensuses.forEach(
        (proposer, consensuses) -> {
          if (!consensuses
              .isEmpty()) { // should not be possible (if it is in the map, there is at least one)
            for (ConsensusNode node : nodes) {
              if (node.getPublicKey().equals(proposer)) {
                Consensus lastConsensus = consensuses.get(consensuses.size() - 1);
                node.setState(State.STARTING);
                node.setConsensus(lastConsensus);
              }
            }
          }
        });
  }

  // get the list of all node where the consensus can be accepted
  private List<ConsensusNode> getApprovedNodes() {
    List<ConsensusNode> approved = new ArrayList<>();
    for (ConsensusNode node : nodes) {
      if (node.getConsensus() != null && node.getConsensus().canBeAccepted()) {
        approved.add(node);
      }
    }
    return approved;
  }

  // check if some nodes have a consensus that was approved by enough nodes and update fragment
  private void updateApproved(Lao lao, Election election, BaseAdapter adapter) {
    List<ConsensusNode> approvedNodes = getApprovedNodes();
    if (approvedNodes.size() > 1) {
      // multiple attempts collided
      approvedNodes.forEach(node -> node.setState(State.FAILED));
      adapter.notifyDataSetChanged();

    } else if (!approvedNodes.isEmpty()) {
      // assuming the election start time was updated from scheduled to real start time
      String startedDate = dateFormat.format(new Date(election.getStartTimestamp() * 1000));
      electionStatus.setText(R.string.started);
      electionStart.setText(getString(R.string.election_started_at, startedDate));
      electionStart.setEnabled(false);
      return;
    }
    // avoid creating multiple consensus
    electionStart.setEnabled(ownNode.getState() != State.STARTING);
  }
}
