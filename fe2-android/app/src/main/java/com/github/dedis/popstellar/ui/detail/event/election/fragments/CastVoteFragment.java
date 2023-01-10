package com.github.dedis.popstellar.ui.detail.event.election.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.CastVoteFragmentBinding;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.PlainVote;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.ui.detail.*;
import com.github.dedis.popstellar.ui.detail.event.election.ZoomOutTransformer;
import com.github.dedis.popstellar.ui.detail.event.election.adapters.CastVoteViewPagerAdapter;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import java.util.*;

import dagger.hilt.android.AndroidEntryPoint;
import me.relex.circleindicator.CircleIndicator3;

import static com.github.dedis.popstellar.ui.detail.LaoDetailActivity.setCurrentFragment;

/**
 * A simple {@link Fragment} subclass. Use the {@link CastVoteFragment#newInstance} factory method
 * to create an instance of this fragment.
 */
@AndroidEntryPoint
public class CastVoteFragment extends Fragment {
  public static final String TAG = CastVoteFragment.class.getSimpleName();

  private Button voteButton;
  private LaoDetailViewModel viewModel;

  private final View.OnClickListener buttonListener =
      v -> {
        voteButton.setEnabled(false);
        List<PlainVote> plainVotes = new ArrayList<>();
        List<ElectionQuestion> electionQuestions =
            viewModel.getCurrentElection().getElectionQuestions();
        for (int i = 0; i < electionQuestions.size(); i++) {
          ElectionQuestion electionQuestion = electionQuestions.get(i);

          // Attendee should not be able to send cast vote if he didn't vote for all questions
          List<Integer> votes = viewModel.getCurrentElectionVotes().getValue();
          if (Objects.requireNonNull(votes).size() < electionQuestions.size()) {
            return;
          }

          Integer vote = viewModel.getCurrentElectionVotes().getValue().get(i);
          // Only one vote should be selected.
          PlainVote plainVote =
              new PlainVote(
                  electionQuestion.getId(),
                  vote,
                  electionQuestion.getWriteIn(),
                  null,
                  viewModel.getCurrentElection().getId());
          plainVotes.add(plainVote);
        }

        viewModel.addDisposable(
            viewModel
                .sendVote(plainVotes)
                .subscribe(
                    () -> {
                      setCurrentFragment(
                          getParentFragmentManager(),
                          R.id.fragment_lao_detail,
                          LaoDetailFragment::newInstance);
                      // Toast ? + send back to election screen or details screen ?
                      Toast.makeText(
                              requireContext(), "vote successfully sent !", Toast.LENGTH_LONG)
                          .show();
                    },
                    error ->
                        ErrorUtils.logAndShow(
                            requireContext(), TAG, error, R.string.error_send_vote)));
      };

  public CastVoteFragment() {
    // Required empty public constructor
  }

  public static CastVoteFragment newInstance() {
    return new CastVoteFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    // Inflate the layout for this fragment
    CastVoteFragmentBinding mCastVoteFragBinding =
        CastVoteFragmentBinding.inflate(inflater, container, false);
    viewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    TextView laoNameView = mCastVoteFragBinding.castVoteLaoName;
    TextView electionNameView = mCastVoteFragBinding.castVoteElectionName;

    // setUp the cast Vote button
    voteButton = mCastVoteFragBinding.castVoteButton;

    // Getting laoView
    LaoView laoView;
    try {
      laoView = viewModel.getLaoView();
    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, R.string.error_no_lao);
      return null;
    }

    // Getting election
    Election election = viewModel.getCurrentElection();
    if (election == null) {
      Log.e(TAG, "The current election of the LaoDetailViewModel is null");
      return null;
    }

    // Setting the Lao Name
    laoNameView.setText(laoView.getName());

    // Setting election name
    electionNameView.setText(election.getName());

    // Setting up the votes for the adapter
    viewModel.setCurrentElectionVotes(setEmptyVoteList());

    // Setting the viewPager and its adapter
    ViewPager2 viewPager2 = mCastVoteFragBinding.castVotePager;
    CastVoteViewPagerAdapter adapter =
        new CastVoteViewPagerAdapter(viewModel, mCastVoteFragBinding);
    viewPager2.setAdapter(adapter);
    viewPager2.setPageTransformer(new ZoomOutTransformer());
    // Setting the indicator for horizontal swipe
    CircleIndicator3 circleIndicator = mCastVoteFragBinding.swipeIndicator;
    circleIndicator.setViewPager(viewPager2);

    voteButton.setOnClickListener(buttonListener);
    return mCastVoteFragBinding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.vote);
  }

  private List<Integer> setEmptyVoteList() {
    // Keep this method if we need in the future to have multiple votes
    return new ArrayList<>();
  }
}
