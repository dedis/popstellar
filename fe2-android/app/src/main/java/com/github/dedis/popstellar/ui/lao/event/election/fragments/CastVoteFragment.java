package com.github.dedis.popstellar.ui.lao.event.election.fragments;

import static com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow;

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
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.ui.lao.event.election.ElectionViewModel;
import com.github.dedis.popstellar.ui.lao.event.election.ZoomOutTransformer;
import com.github.dedis.popstellar.ui.lao.event.election.adapters.CastVoteViewPagerAdapter;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.UnknownElectionException;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import dagger.hilt.android.AndroidEntryPoint;
import java.util.*;
import javax.inject.Inject;
import me.relex.circleindicator.CircleIndicator3;

/**
 * A simple {@link Fragment} subclass. Use the {@link CastVoteFragment#newInstance} factory method
 * to create an instance of this fragment.
 */
@AndroidEntryPoint
public class CastVoteFragment extends Fragment {
  public static final String TAG = CastVoteFragment.class.getSimpleName();

  private static final String ELECTION_ID = "election_id";

  @Inject ElectionRepository electionRepository;

  private LaoViewModel laoViewModel;
  private ElectionViewModel electionViewModel;

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
    laoViewModel = LaoActivity.obtainViewModel(requireActivity());
    electionViewModel =
        LaoActivity.obtainElectionViewModel(requireActivity(), laoViewModel.getLaoId());

    // Setting the lao ad election name
    if (setLaoName()) {
      return null;
    }

    if (setElectionName()) {
      return null;
    }

    try {
      Election election = electionRepository.getElection(laoViewModel.getLaoId(), electionId);

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

    setEncryptionVotes();

    handleBackNav();
    return binding.getRoot();
  }

  /**
   * Show the progress bar and block user's touch inputs if the encryption of the vote takes time
   */
  private void setEncryptionVotes() {
    // observe the progress for encryption
    electionViewModel
        .getIsEncrypting()
        .observe(
            getViewLifecycleOwner(),
            isEncrypting -> {
              // Block touch inputs if loading and display progress bar
              if (Boolean.TRUE.equals(isEncrypting)) {
                binding.loadingContainer.setVisibility(View.VISIBLE);
                requireActivity()
                    .getWindow()
                    .setFlags(
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
              } else {
                binding.loadingContainer.setVisibility(View.GONE);
                requireActivity()
                    .getWindow()
                    .clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
              }
            });
  }

  private boolean setLaoName() {
    try {
      LaoView laoView = laoViewModel.getLao();
      binding.castVoteLaoName.setText(laoView.getName());
      return false;
    } catch (UnknownLaoException e) {
      logAndShow(requireContext(), TAG, R.string.error_no_lao);
      return true;
    }
  }

  private boolean setElectionName() {
    try {
      Election election = electionRepository.getElection(laoViewModel.getLaoId(), electionId);
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
      Election election = electionRepository.getElection(laoViewModel.getLaoId(), electionId);
      List<ElectionQuestion> electionQuestions = election.getElectionQuestions();

      // Attendee should not be able to send cast vote if he didn't vote for all questions
      if (votes.size() < electionQuestions.size()) {
        return;
      }

      for (ElectionQuestion electionQuestion : electionQuestions) {
        PlainVote plainVote =
            new PlainVote(
                electionQuestion.id,
                votes.get(electionQuestion.id),
                electionQuestion.writeIn,
                null,
                electionId);

        plainVotes.add(plainVote);
      }

      laoViewModel.addDisposable(
          electionViewModel
              .sendVote(electionId, plainVotes)
              .subscribe(
                  () ->
                      Toast.makeText(requireContext(), R.string.vote_sent, Toast.LENGTH_LONG)
                          .show(),
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
    laoViewModel.setPageTitle(R.string.vote);
    laoViewModel.setIsTab(false);
  }

  private void handleBackNav() {
    LaoActivity.addBackNavigationCallback(
        requireActivity(),
        getViewLifecycleOwner(),
        ActivityUtils.buildBackButtonCallback(
            TAG,
            "election",
            () ->
                ElectionFragment.openFragment(
                    getParentFragmentManager(), getArguments().getString(ELECTION_ID))));
  }
}
