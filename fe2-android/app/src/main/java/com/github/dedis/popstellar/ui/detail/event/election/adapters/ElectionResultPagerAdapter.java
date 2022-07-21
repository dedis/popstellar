package com.github.dedis.popstellar.ui.detail.event.election.adapters;

import android.view.*;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import java.util.ArrayList;
import java.util.List;

public class ElectionResultPagerAdapter
    extends RecyclerView.Adapter<ElectionResultPagerAdapter.Pager2ViewHolder> {

  private final LaoDetailViewModel mLaoDetailViewModel;
  private ElectionResultListAdapter adapter;

  public ElectionResultPagerAdapter(LaoDetailViewModel mLaoDetailViewModel) {
    this.mLaoDetailViewModel = mLaoDetailViewModel;
  }

  @NonNull
  @Override
  public Pager2ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    adapter =
        new ElectionResultListAdapter(
            parent.getContext(), R.layout.election_result_list_view_layout, new ArrayList<>());
    return new Pager2ViewHolder(
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.election_result_pager_layout, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull Pager2ViewHolder holder, int position) {
    Election election = mLaoDetailViewModel.getCurrentElection();

    // setting the question
    ElectionQuestion electionQuestion = election.getElectionQuestions().get(position);
    String question = electionQuestion.getQuestion();

    holder.questionView.setText(question);

    List<QuestionResult> questionResults =
        election.getResultsForQuestionId(electionQuestion.getId());

    List<ElectionResultListAdapter.ElectionResult> electionResults = new ArrayList<>();
    for (int i = 0; i < questionResults.size(); i++) {
      electionResults.add(
          new ElectionResultListAdapter.ElectionResult(
              questionResults.get(i).getBallot(), questionResults.get(i).getCount()));
    }
    adapter.clear();
    adapter.addAll(electionResults);

    holder.resultListView.setAdapter(adapter);
  }

  @Override
  public int getItemCount() {
    return mLaoDetailViewModel.getCurrentElection().getElectionQuestions().size();
  }

  protected static class Pager2ViewHolder extends RecyclerView.ViewHolder {

    private final ListView resultListView;
    private final TextView questionView;

    public Pager2ViewHolder(View itemView) {
      super(itemView);
      resultListView = (ListView) itemView.findViewById(R.id.election_result_listView);
      questionView = (TextView) itemView.findViewById(R.id.election_result_question);
    }
  }
}
