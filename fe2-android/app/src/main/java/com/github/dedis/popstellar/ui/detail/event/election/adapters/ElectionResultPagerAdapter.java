package com.github.dedis.popstellar.ui.detail.event.election.adapters;

import android.view.*;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult;
import com.github.dedis.popstellar.repository.ElectionRepository;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.detail.event.election.fragments.ElectionResultFragment;
import com.github.dedis.popstellar.utility.error.UnknownElectionException;

import java.util.*;
import java.util.stream.Collectors;

import io.reactivex.android.schedulers.AndroidSchedulers;

import static com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow;

public class ElectionResultPagerAdapter
    extends RecyclerView.Adapter<ElectionResultPagerAdapter.Pager2ViewHolder> {

  private static final String TAG = ElectionResultFragment.class.getSimpleName();

  private List<QuestionResults> currentResults;
  private final LaoDetailViewModel viewModel;
  private final ElectionRepository electionRepository;
  private final String electionId;
  private ElectionResultListAdapter adapter;

  public ElectionResultPagerAdapter(
      LaoDetailViewModel viewModel, ElectionRepository electionRepository, String electionId) {
    this.viewModel = viewModel;
    this.electionRepository = electionRepository;
    this.electionId = electionId;

    try {
      viewModel.addDisposable(
          electionRepository
              .getElectionObservable(viewModel.getLaoId(), electionId)
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(
                  e -> {
                    List<QuestionResults> results =
                        e.getElectionQuestions().stream()
                            .map(q -> new QuestionResults(q, e.getResultsForQuestionId(q.getId())))
                            .collect(Collectors.toList());

                    if (!results.equals(currentResults)) {
                      // The results were updated, update the result list
                      currentResults = results;
                      notifyDataSetChanged();
                    }
                  }));
    } catch (UnknownElectionException err) {
      logAndShow(viewModel.getApplication(), TAG, err, R.string.generic_error);
    }
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
    // setting the question
    QuestionResults results = currentResults.get(position);
    String question = results.question.getQuestion();
    Set<QuestionResult> questionResults = results.results;

    holder.questionView.setText(question);

    // Create the displayable results
    List<ElectionResultListAdapter.ElectionResult> adaptedResults =
        questionResults.stream()
            .sorted(Comparator.comparing(QuestionResult::getCount).reversed())
            .map(
                result ->
                    new ElectionResultListAdapter.ElectionResult(
                        result.getBallot(), result.getCount()))
            .collect(Collectors.toList());

    adapter.clear();
    adapter.addAll(adaptedResults);

    holder.resultListView.setAdapter(adapter);
  }

  @Override
  public int getItemCount() {
    if (this.currentResults == null) {
      return 0;
    }

    return this.currentResults.size();
  }

  protected static class Pager2ViewHolder extends RecyclerView.ViewHolder {

    private final ListView resultListView;
    private final TextView questionView;

    public Pager2ViewHolder(View itemView) {
      super(itemView);
      resultListView = itemView.findViewById(R.id.election_result_listView);
      questionView = itemView.findViewById(R.id.election_result_question);
    }
  }

  private static class QuestionResults {

    private final ElectionQuestion question;
    private final Set<QuestionResult> results;

    private QuestionResults(ElectionQuestion question, Set<QuestionResult> results) {
      this.question = question;
      this.results = results;
    }
  }
}
