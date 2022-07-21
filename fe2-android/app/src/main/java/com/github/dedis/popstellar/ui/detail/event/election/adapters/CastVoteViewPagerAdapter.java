package com.github.dedis.popstellar.ui.detail.event.election.adapters;

import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.CastVoteFragmentBinding;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import java.util.ArrayList;
import java.util.List;

public class CastVoteViewPagerAdapter
    extends RecyclerView.Adapter<CastVoteViewPagerAdapter.Pager2ViewHolder> {

  private ArrayAdapter<String> ballotAdapter;
  private final LaoDetailViewModel mLaoDetailViewModel;
  private final CastVoteFragmentBinding castVoteBinding;
  private Button voteButton;

  // Defines the number of votes (unique for now)
  private static final int NUMBER_OF_CHOICE = 1;

  public CastVoteViewPagerAdapter(
      LaoDetailViewModel mLaoDetailViewModel, CastVoteFragmentBinding castVoteBinding) {
    this.mLaoDetailViewModel = mLaoDetailViewModel;
    this.castVoteBinding = castVoteBinding;
  }

  @NonNull
  @Override
  public Pager2ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ballotAdapter =
        new ArrayAdapter<>(
            parent.getContext(),
            android.R.layout.simple_list_item_single_choice,
            new ArrayList<>());
    voteButton = castVoteBinding.castVoteButton;
    return new Pager2ViewHolder(
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.cast_vote_list_view_layout, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull Pager2ViewHolder holder, int position) {
    Election election = mLaoDetailViewModel.getCurrentElection();

    // setting the question
    ElectionQuestion question = election.getElectionQuestions().get(position);
    holder.questionView.setText(question.getQuestion());

    // this will determine the number of option the user can select and vote for
    // If another voting method is implemented in setUp, this can be adapted
    int numberOfChoices = NUMBER_OF_CHOICE; // by default it's 1

    List<Integer> votes = new ArrayList<>();

    // setting the list view with ballot options
    List<String> ballotOptions = question.getBallotOptions();
    ballotAdapter.clear();
    ballotAdapter.addAll(ballotOptions);
    ListView ballotsListView = holder.ballotsListView;
    ballotsListView.setAdapter(ballotAdapter);
    ballotsListView.setChoiceMode(
        numberOfChoices > 1 ? AbsListView.CHOICE_MODE_MULTIPLE : AbsListView.CHOICE_MODE_SINGLE);

    AdapterView.OnItemClickListener itemListener =
        (parent, view, listPosition, id) -> {
          // in this listener the position refers to the index of the question and
          // the list position is the index of the ballot that was clicked on
          ballotsListView.setClickable(false);
          if (numberOfChoices > 1) {
            if (votes.contains(listPosition)) {
              // without the cast listPosition is treated as the index rather than the list element
              votes.remove((Integer) listPosition);
              ballotsListView.setItemChecked(listPosition, false);
            } else if (votes.size() < numberOfChoices) {
              votes.add(listPosition);
              ballotsListView.setItemChecked(listPosition, true);
            } else {
              ballotsListView.setItemChecked(listPosition, false);
            }
          } else {
            if (votes.contains(listPosition)) {
              votes.clear();
              ballotsListView.setItemChecked(listPosition, false);
            } else {
              votes.clear();
              votes.add(listPosition);
            }
          }
          // This is for satisfying the unique vote method
          // It should be changed in the future when multiple vote will be allowed
          // For now the unique vote is the first element of the votes list
          mLaoDetailViewModel.setCurrentElectionQuestionVotes(
              votes.isEmpty() ? -1 : votes.get(0), position);
          ballotsListView.setClickable(true);
          voteButton.setEnabled(checkEachQuestion());
        };
    ballotsListView.setOnItemClickListener(itemListener);
  }

  @Override
  public int getItemCount() {
    return mLaoDetailViewModel.getCurrentElection().getElectionQuestions().size();
  }

  private boolean checkEachQuestion() {
    List<Integer> allVotes = mLaoDetailViewModel.getCurrentElectionVotes().getValue();
    if (allVotes != null) {
      if (allVotes.size() < getItemCount()) return false;
      for (Integer vote : allVotes) {
        if (vote == null || vote < 0) {
          return false;
        }
      }
    }

    return true;
  }

  protected static class Pager2ViewHolder extends RecyclerView.ViewHolder {

    private final ListView ballotsListView;
    private final TextView questionView;

    public Pager2ViewHolder(View itemView) {
      super(itemView);
      ballotsListView = itemView.findViewById(R.id.list_view_pager2);
      questionView = itemView.findViewById(R.id.cast_vote_question);
    }
  }
}
