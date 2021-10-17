package com.github.dedis.popstellar.ui.detail.event.consensus;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
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
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A simple {@link Fragment} subclass. Use the {@link ElectionStartFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class ElectionStartFragment extends Fragment {

  private static final String TAG = ElectionStartFragment.class.getSimpleName();

  private final SimpleDateFormat DATE_FORMAT =
      new SimpleDateFormat("yyyy/MM/dd HH:mm z", Locale.ENGLISH);

  private final CompositeDisposable disposables = new CompositeDisposable();

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

    LaoDetailViewModel mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(getActivity());

    Election election = mLaoDetailViewModel.getCurrentElection();

    String scheduledDate = DATE_FORMAT.format(new Date(election.getStartTimestamp() * 1000));

    binding.electionTitle.setText(getString(R.string.election_start_title, election.getName()));
    binding.electionStatus.setText(R.string.waiting_scheduled_time);
    binding.electionStart.setText(getString(R.string.election_scheduled, scheduledDate));
    binding.electionStart.setEnabled(false);

    // Check every seconds until the scheduled time, then show the "election start" button
    Disposable disposable =
        Observable.interval(0, 1, TimeUnit.SECONDS)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(
                new DisposableObserver<Long>() {
                  @Override
                  public void onNext(@NonNull Long aLong) {
                    if (Instant.now().getEpochSecond() >= election.getStartTimestamp()) {
                      binding.electionStart.setText(R.string.start_election);
                      binding.electionStatus.setText(R.string.ready_to_start);
                      binding.electionStart.setEnabled(true);
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

    binding.electionStart.setOnClickListener(
        clicked ->
            mLaoDetailViewModel.createNewConsensus(
                Instant.now().getEpochSecond(), election.getId(), "election", "state", "started"));

    List<ConsensusNode> nodes = getNodes(mLaoDetailViewModel, election.getId());
    NodesAcceptorAdapter adapter = new NodesAcceptorAdapter(nodes, getActivity());
    GridView gridView = binding.nodesGrid;
    gridView.setAdapter(adapter);

    mLaoDetailViewModel
        .getUpdateConsensusEvent()
        .observe(
            this,
            consensusSingleEvent -> {
              Consensus consensus = consensusSingleEvent.getContentIfNotHandled();
              if (consensus != null && consensus.getKey().getId().equals(election.getId())) {
                adapter.updateList(getNodes(mLaoDetailViewModel, election.getId()));
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

  private List<ConsensusNode> getNodes(LaoDetailViewModel mLaoDetailViewModel, String electionId) {
    List<ConsensusNode> nodes = new ArrayList<>();
    List<String> publicKeys = new ArrayList<>(mLaoDetailViewModel.getWitnesses().getValue());
    publicKeys.add(mLaoDetailViewModel.getCurrentLaoValue().getOrganizer());
    for (String key : publicKeys) {
      nodes.add(new ConsensusNode(key));
    }

    Map<String, List<Consensus>> proposerToConsensuses =
        mLaoDetailViewModel.getCurrentLaoValue().getConsensuses().values().stream()
            .filter(consensus -> consensus.getKey().getId().equals(electionId))
            .sorted((c1, c2) -> (int) (c1.getCreation() - c2.getCreation()))
            .collect(Collectors.groupingBy(Consensus::getProposer));

    // If a node tried multiple times to start a consensus and failed, only consider last one
    proposerToConsensuses.forEach(
        (proposer, consensuses) -> {
          if (!consensuses.isEmpty()) {
            for (ConsensusNode node : nodes) {
              if (node.getPublicKey().equals(proposer)) {
                Consensus lastConsensus = consensuses.get(consensuses.size() - 1);
                node.setState(State.STARTING);
                node.setConsensus(lastConsensus);
              }
            }
          }
        });

    return nodes;
  }
}
