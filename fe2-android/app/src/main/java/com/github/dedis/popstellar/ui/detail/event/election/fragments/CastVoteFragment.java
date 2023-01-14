package com.github.dedis.popstellar.ui.detail.event.election.fragments;

import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.CastVoteFragmentBinding;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.PlainVote;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.ElectionRepository;
import com.github.dedis.popstellar.ui.detail.*;
import com.github.dedis.popstellar.ui.detail.event.election.ZoomOutTransformer;
import com.github.dedis.popstellar.ui.detail.event.election.adapters.CastVoteViewPagerAdapter;
import com.github.dedis.popstellar.utility.error.UnknownElectionException;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import java.util.*;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import me.relex.circleindicator.CircleIndicator3;

import static com.github.dedis.popstellar.ui.detail.LaoDetailActivity.setCurrentFragment;
import static com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow;

/**
 * A simple {@link Fragment} subclass. Use the {@link CastVoteFragment#newInstance} factory method
 * to create an instance of this fragment.
 */
@AndroidEntryPoint
public class CastVoteFragment extends Fragment {
  public static final String TAG = CastVoteFragment.class.getSimpleName();

  private static final String ELECTION_ID = "election_id";

  @Inject ElectionRepository electionRepository;

  private LaoDetailViewModel viewModel;
  private CastVoteFragmentBinding binding;

  private String electionId;

  private final Map<String, Integer> votes = new HashMap<>();

  public CastVoteFragment() {
    // Required empty public constructor
  }

  public static CastVoteFragment newInstance(String electionId) {
    CastVoteFragment fragment = new CastVoteFragment();

    Bundle bundle = new Bundle();
    bundle.putString(ELECTION_ID, electionId);
    fragment.setArguments(bundle);

    return fragment;
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    electionId = requireArguments().getString(ELECTION_ID);

    // Inflate the layout for this fragment
    binding = CastVoteFragmentBinding.inflate(inflater, container, false);
    viewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    // Setting the lao ad election name
    if (setLaoName()) {
      return null;
    }

    if (setElectionName()) {
      return null;
    }

    try {
      Election election = electionRepository.getElection(viewModel.getLaoId(), electionId);

      // Setting the viewPager and its adapter
      ViewPager2 pager = binding.castVotePager;
      CastVoteViewPagerAdapter adapter = new CastVoteViewPagerAdapter(binding, election, votes);
      pager.setAdapter(adapter);
      pager.setPageTransformer(new ZoomOutTransformer());

      // Setting the indicator for horizontal swipe
      CircleIndicator3 circleIndicator = binding.swipeIndicator;
      circleIndicator.setViewPager(pager);
    } catch (UnknownElectionException err) {
      logAndShow(requireContext(), TAG, err, R.string.generic_error);
      return null;
    }

    // setUp the cast Vote button
    binding.castVoteButton.setOnClickListener(this::castVote);
    return binding.getRoot();
  }

  private boolean setLaoName() {
    try {
      LaoView laoView = viewModel.getLao();
      binding.castVoteLaoName.setText(laoView.getName());
      return false;
    } catch (UnknownLaoException e) {
      logAndShow(requireContext(), TAG, R.string.error_no_lao);
      return true;
    }
  }

  private boolean setElectionName() {
    try {
      Election election = electionRepository.getElection(viewModel.getLaoId(), electionId);
      binding.castVoteElectionName.setText(election.getName());
      return false;
    } catch (UnknownElectionException e) {
      logAndShow(requireContext(), TAG, R.string.error_no_election);
      return true;
    }
  }

  private void castVote(View voteButton) {
    voteButton.setEnabled(false);
    List<PlainVote> plainVotes = new ArrayList<>();

    try {
      Election election = electionRepository.getElection(viewModel.getLaoId(), electionId);
      List<ElectionQuestion> electionQuestions = election.getElectionQuestions();

      // Attendee should not be able to send cast vote if he didn't vote for all questions
      if (votes.size() < electionQuestions.size()) {
        return;
      }

      for (ElectionQuestion electionQuestion : electionQuestions) {
        PlainVote plainVote =
            new PlainVote(
                electionQuestion.getId(),
                votes.get(electionQuestion.getId()),
                electionQuestion.getWriteIn(),
                null,
                electionId);

        plainVotes.add(plainVote);
      }

      viewModel.addDisposable(
          viewModel
              .sendVote(electionId, plainVotes)
              .subscribe(
                  () -> {
                    setCurrentFragment(
                        getParentFragmentManager(),
                        R.id.fragment_lao_detail,
                        LaoDetailFragment::newInstance);
                    // Toast ? + send back to election screen or details screen ?
                    Toast.makeText(requireContext(), "vote successfully sent !", Toast.LENGTH_LONG)
                        .show();
                  },
                  err -> logAndShow(requireContext(), TAG, err, R.string.error_send_vote)));
    } catch (UnknownElectionException err) {
      logAndShow(requireContext(), TAG, err, R.string.generic_error);
    } finally {
      voteButton.setEnabled(true);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.vote);
    viewModel.setIsTab(false);
  }
}
