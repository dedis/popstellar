package com.github.dedis.popstellar.ui.lao.event.election.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.databinding.CastVoteFragmentBinding
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion
import com.github.dedis.popstellar.model.objects.Election

class CastVoteViewPagerAdapter(
    binding: CastVoteFragmentBinding,
    election: Election,
    private val votes: MutableMap<String, Int>
) : RecyclerView.Adapter<CastVoteViewPagerAdapter.Pager2ViewHolder>() {

  private lateinit var ballotAdapter: ArrayAdapter<String>
  private val questions: List<ElectionQuestion> = election.electionQuestions
  private val voteButton: Button = binding.castVoteButton

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Pager2ViewHolder {
    ballotAdapter =
        ArrayAdapter(parent.context, R.layout.list_item_circle_single_choice, ArrayList())

    return Pager2ViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.cast_vote_list_view_layout, parent, false))
  }

  override fun onBindViewHolder(holder: Pager2ViewHolder, position: Int) {
    // This is bad practice and should be removed in the future
    // The problem for now is that reused view messes up the data intake
    holder.setIsRecyclable(false)

    // setting the question
    val question = questions[position]
    holder.questionView.text = question.question

    // this will determine the number of option the user can select and vote for
    // If another voting method is implemented in setUp, this can be adapted

    // setting the list view with ballot options
    ballotAdapter.clear()
    ballotAdapter.addAll(question.ballotOptions)

    val ballotsListView = holder.ballotsListView
    ballotsListView.adapter = ballotAdapter
    ballotsListView.choiceMode = AbsListView.CHOICE_MODE_SINGLE
    ballotsListView.onItemClickListener =
        OnItemClickListener { _: AdapterView<*>?, _: View?, listPosition: Int, _: Long ->
          // This is for satisfying the unique vote method
          // It should be changed in the future when multiple votes will be allowed
          votes[question.id] = listPosition
          voteButton.isEnabled = areEveryQuestionChecked()
        }

    // Ensure that the selection is not lost
    val index = votes[question.id]
    if (index != null && index >= 0 && index < question.ballotOptions.size) {
      holder.ballotsListView.setItemChecked(index, true)
    }
  }

  override fun getItemCount(): Int {
    return questions.size
  }

  private fun areEveryQuestionChecked(): Boolean {
    return votes.size == questions.size
  }

  class Pager2ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val ballotsListView: ListView = itemView.findViewById(R.id.list_view_pager2)
    val questionView: TextView = itemView.findViewById(R.id.cast_vote_question)
  }
}
