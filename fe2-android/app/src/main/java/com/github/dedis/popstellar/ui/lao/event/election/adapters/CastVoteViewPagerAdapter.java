package com.github.dedis.popstellar.ui.lao.event.election.adapters;

import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.CastVoteFragmentBinding;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion;
import com.github.dedis.popstellar.model.objects.Election;

import java.util.*;

public class CastVoteViewPagerAdapter
    extends RecyclerView.Adapter<CastVoteViewPagerAdapter.Pager2ViewHolder> {

  private ArrayAdapter<String> ballotAdapter;

  private final List<ElectionQuestion> questions;
  private final Map<String, Integer> votes;
  private final Button voteButton;

  public CastVoteViewPagerAdapter(
      CastVoteFragmentBinding binding, Election election, Map<String, Integer> votes) {
    questions = election.getElectionQuestions();
    voteButton = binding.castVoteButton;
    this.votes = votes;
  }

  @NonNull
  @Override
  public Pager2ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    ballotAdapter =
        new ArrayAdapter<>(
            parent.getContext(), R.layout.list_item_circle_single_choice, new ArrayList<>());

    return new Pager2ViewHolder(
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.cast_vote_list_view_layout, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull Pager2ViewHolder holder, int position) {
    // This is bad practice and should be removed in the future
    // The problem for now is that reused view messes up the data intake
    holder.setIsRecyclable(false);

    // setting the question
    ElectionQuestion question = questions.get(position);
    holder.questionView.setText(question.getQuestion());

    // this will determine the number of option the user can select and vote for
    // If another voting method is implemented in setUp, this can be adapted

    // setting the list view with ballot options
    ballotAdapter.clear();
    ballotAdapter.addAll(question.getBallotOptions());
    ListView ballotsListView = holder.ballotsListView;
    ballotsListView.setAdapter(ballotAdapter);
    ballotsListView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
    ballotsListView.setOnItemClickListener(
        (parent, view, listPosition, id) -> {
          // This is for satisfying the unique vote method
          // It should be changed in the future when multiple votes will be allowed
          votes.put(question.getId(), listPosition);
          voteButton.setEnabled(areEveryQuestionChecked());
        });

    // Ensure that the selection is not lost
    Integer index = votes.get(question.getId());
    if (index != null && index >= 0 && index < question.getBallotOptions().size()) {
      holder.ballotsListView.setItemChecked(index, true);
    }
  }

  @Override
  public int getItemCount() {
    return questions.size();
  }

  private boolean areEveryQuestionChecked() {
    return votes.size() == questions.size();
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
