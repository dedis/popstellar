package com.github.dedis.popstellar.ui.lao.event.election.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.github.dedis.popstellar.R

class ElectionResultListAdapter(
    private val mContext: Context,
    private val mResource: Int,
    objects: List<ElectionResult>
) : ArrayAdapter<ElectionResultListAdapter.ElectionResult>(mContext, mResource, objects) {
  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val inflater = LayoutInflater.from(mContext)
    @SuppressLint("ViewHolder") val view = inflater.inflate(mResource, parent, false)

    val ballotOption = getItem(position)?.getBallotOption()
    val ballotView = view.findViewById<View>(R.id.election_result_ballot_option) as TextView
    ballotView.text = ballotOption

    val votesView = view.findViewById<View>(R.id.election_result_vote_number) as TextView
    val numberOfVotes = getItem(position)?.votes.toString()
    votesView.text = numberOfVotes

    return view
  }

  class ElectionResult(private var ballotOption: String, var votes: Int) {

    fun getBallotOption(): String {
      return ballotOption
    }

    fun setBallotOption(ballotOption: String?) {
      requireNotNull(ballotOption)

      this.ballotOption = ballotOption
    }
  }
}
