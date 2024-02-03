package com.github.dedis.popstellar.ui.lao.event.election.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion
import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.repository.ElectionRepository
import com.github.dedis.popstellar.ui.lao.LaoViewModel
import com.github.dedis.popstellar.ui.lao.event.election.fragments.ElectionResultFragment
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.stream.Collectors

class ElectionResultPagerAdapter
@SuppressLint("NotifyDataSetChanged")
constructor(viewModel: LaoViewModel, electionRepository: ElectionRepository, electionId: String) :
    RecyclerView.Adapter<ElectionResultPagerAdapter.Pager2ViewHolder>() {

  private var currentResults: List<QuestionResults>? = null
  private lateinit var adapter: ElectionResultListAdapter

  init {
    try {
      viewModel.addDisposable(
          electionRepository
              .getElectionObservable(viewModel.laoId!!, electionId)
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe { e: Election ->
                val results =
                    e.electionQuestions
                        .stream()
                        .map { q: ElectionQuestion ->
                          QuestionResults(q, e.getResultsForQuestionId(q.id))
                        }
                        .collect(Collectors.toList())
                if (results != currentResults) {
                  // The results were updated, update the result list
                  currentResults = results
                  notifyDataSetChanged()
                }
              })
    } catch (err: UnknownElectionException) {
      logAndShow(viewModel.getApplication(), TAG, err, R.string.generic_error)
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Pager2ViewHolder {
    adapter =
        ElectionResultListAdapter(
            parent.context, R.layout.election_result_list_view_layout, ArrayList())

    return Pager2ViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.election_result_pager_layout, parent, false))
  }

  override fun onBindViewHolder(holder: Pager2ViewHolder, position: Int) {
    // This is bad practice and should be removed in the future
    // The problem for now is that reused view messes up the data intake
    holder.setIsRecyclable(false)

    // setting the question
    val results = currentResults!![position]
    val question = results.question.question
    val questionResults = results.results
    holder.questionView.text = question

    // Create the displayable results
    val adaptedResults =
        questionResults!!
            .stream()
            .sorted(Comparator.comparing(QuestionResult::count).reversed())
            .map { result: QuestionResult ->
              ElectionResultListAdapter.ElectionResult(result.ballot, result.count)
            }
            .collect(Collectors.toList())

    adapter.clear()
    adapter.addAll(adaptedResults)
    holder.resultListView.adapter = adapter
  }

  override fun getItemCount(): Int {
    return currentResults?.size ?: 0
  }

  class Pager2ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val resultListView: ListView = itemView.findViewById(R.id.election_result_listView)
    val questionView: TextView = itemView.findViewById(R.id.election_result_question)
  }

  private class QuestionResults(val question: ElectionQuestion, val results: Set<QuestionResult>?)

  companion object {
    private val TAG = ElectionResultFragment::class.java.simpleName
  }
}
