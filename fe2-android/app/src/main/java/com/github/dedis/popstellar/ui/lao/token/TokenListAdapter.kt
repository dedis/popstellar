package com.github.dedis.popstellar.ui.lao.token

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.setCurrentFragment
import com.github.dedis.popstellar.ui.lao.token.TokenListAdapter.TokensViewHolder
import com.google.android.material.card.MaterialCardView

class TokenListAdapter(private val activity: FragmentActivity) :
    RecyclerView.Adapter<TokensViewHolder>() {
  private var rollCalls: List<RollCall> = ArrayList()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokensViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.token_layout, parent, false)
    return TokensViewHolder(view)
  }

  override fun onBindViewHolder(holder: TokensViewHolder, position: Int) {
    val rollCall = rollCalls[position]

    holder.rollCallTitle.text = rollCall.name
    holder.status.visibility = View.GONE
    holder.materialCardView.setOnClickListener {
      setCurrentFragment(activity.supportFragmentManager, R.id.fragment_token) {
        TokenFragment.newInstance(rollCall.persistentId)
      }
    }
  }

  override fun getItemCount(): Int {
    return rollCalls.size
  }

  @SuppressLint("NotifyDataSetChanged")
  fun replaceList(rollCalls: List<RollCall>) {
    this.rollCalls = ArrayList(rollCalls)
    notifyDataSetChanged()
  }

  class TokensViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var materialCardView: MaterialCardView
    var rollCallTitle: TextView
    var status: TextView

    init {
      materialCardView = itemView.findViewById(R.id.token_card_layout)
      rollCallTitle = itemView.findViewById(R.id.token_layout_rc_title)
      status = itemView.findViewById(R.id.token_layout_status)
    }
  }
}
