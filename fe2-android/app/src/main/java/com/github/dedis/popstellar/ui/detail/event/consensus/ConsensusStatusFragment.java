package com.github.dedis.popstellar.ui.detail.event.consensus;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import androidx.fragment.app.Fragment;
import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.ConsensusStatusFragmentBinding;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A simple {@link Fragment} subclass. Use the {@link ConsensusStatusFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class ConsensusStatusFragment extends Fragment {

  private static final String TAG = ConsensusStatusFragment.class.getSimpleName();
  private static final String EXTRA_ID = "CONSENSUS_ID";

  private Consensus consensus;

  public ConsensusStatusFragment() {
    // Required empty public constructor
  }

  public static ConsensusStatusFragment newInstance(String consensusId) {
    ConsensusStatusFragment consensusStatusFragment = new ConsensusStatusFragment();
    Bundle bundle = new Bundle();
    bundle.putString(EXTRA_ID, consensusId);
    consensusStatusFragment.setArguments(bundle);
    return consensusStatusFragment;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    ConsensusStatusFragmentBinding binding =
        ConsensusStatusFragmentBinding.inflate(inflater, container, false);

    LaoDetailViewModel mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(getActivity());

    String id = getArguments().getString(EXTRA_ID);
    Optional<Consensus> optConsensus = mLaoDetailViewModel.getCurrentLaoValue().getConsensus(id);
    if (!optConsensus.isPresent()) {
      Log.d(TAG, "failed to retrieved consensus with id " + id);
      mLaoDetailViewModel.openLaoDetail();
    } else {
      consensus = optConsensus.get();
    }

    ListView listView = binding.consensusResponsesList;
    List<String> acceptors = new ArrayList<>(consensus.getAcceptors());
    ConsensusAcceptorAdapter acceptorAdapter =
        new ConsensusAcceptorAdapter(acceptors, consensus.getAcceptorsResponses(), getActivity());
    listView.setAdapter(acceptorAdapter);

    binding.confirmButton.setEnabled(
        consensus.getState() == EventState.OPENED && consensus.canBeAccepted());
    binding.confirmButton.setOnClickListener(v -> mLaoDetailViewModel.sendConsensusLearn());
    binding
        .backLayout
        .findViewById(R.id.tab_back)
        .setOnClickListener(v -> mLaoDetailViewModel.openLaoDetail());

    binding.setLifecycleOwner(getActivity());

    return binding.getRoot();
  }
}
