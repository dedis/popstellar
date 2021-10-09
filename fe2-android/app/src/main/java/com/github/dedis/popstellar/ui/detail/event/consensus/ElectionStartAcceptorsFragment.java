package com.github.dedis.popstellar.ui.detail.event.consensus;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.ElectionStartAcceptorsFragmentBinding;
import com.github.dedis.popstellar.model.objects.Consensus;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;

/**
 * A simple {@link Fragment} subclass. Use the {@link ElectionStartAcceptorsFragment#newInstance}
 * factory method to create an instance of this fragment.
 */
public class ElectionStartAcceptorsFragment extends Fragment {

  public static final String TAG = ElectionStartAcceptorsFragment.class.getSimpleName();

  private final SimpleDateFormat DATE_FORMAT =
      new SimpleDateFormat("yyyy/MM/dd HH:mm z", Locale.ENGLISH);

  private Button acceptButton;
  private Button rejectButton;

  private LaoDetailViewModel laoDetailViewModel;

  public ElectionStartAcceptorsFragment() {
    // Required empty public constructor
  }

  public static ElectionStartAcceptorsFragment newInstance() {
    return new ElectionStartAcceptorsFragment();
  }

  private void sendConsensusResponse(boolean accepted) {
    acceptButton.setEnabled(false);
    rejectButton.setEnabled(false);

    laoDetailViewModel.sendConsensusVote(accepted);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    ElectionStartAcceptorsFragmentBinding electionStartAcceptorsFragmentBinding =
        ElectionStartAcceptorsFragmentBinding.inflate(inflater, container, false);

    laoDetailViewModel = LaoDetailActivity.obtainViewModel(getActivity());

    Lao lao = laoDetailViewModel.getCurrentLaoValue();
    Consensus consensus = laoDetailViewModel.getCurrentConsensus();
    String proposer = consensus.getProposer();

    Optional<Election> electionOpt = lao.getElection(consensus.getKey().getId());
    long electionStart = 0;
    if (!electionOpt.isPresent()) {
      Log.d(TAG, "election not found : "+consensus.getKey().getId());
    } else {
      Election election = electionOpt.get();
      electionStart = election.getStartTimestamp() * 1000L;
    }

    Date currentDate = new Date();
    Date plannedDate = new Date(electionStart);

    electionStartAcceptorsFragmentBinding.currentTime.setText(DATE_FORMAT.format(currentDate));
    electionStartAcceptorsFragmentBinding.plannedStart.setText(DATE_FORMAT.format(plannedDate));
    electionStartAcceptorsFragmentBinding.proposer.setText(proposer);

    acceptButton = electionStartAcceptorsFragmentBinding.acceptButton;
    rejectButton = electionStartAcceptorsFragmentBinding.rejectButton;

    acceptButton.setOnClickListener(v -> sendConsensusResponse(true));
    rejectButton.setOnClickListener(v -> sendConsensusResponse(false));

    Button back = electionStartAcceptorsFragmentBinding.backLayout.findViewById(R.id.tab_back);
    back.setOnClickListener(v -> laoDetailViewModel.openLaoDetail());

    electionStartAcceptorsFragmentBinding.setLifecycleOwner(getActivity());

    return electionStartAcceptorsFragmentBinding.getRoot();
  }
}
