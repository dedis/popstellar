package com.github.dedis.popstellar.ui.home

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.ui.home.LAOListAdapter.LAOListItemViewHolder
import com.github.dedis.popstellar.ui.lao.LaoActivity
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import timber.log.Timber

class LAOListAdapter(private val homeViewModel: HomeViewModel, private val activity: Activity) :
    RecyclerView.Adapter<LAOListItemViewHolder>() {
  private var laoIdList: List<String> = emptyList()

  @SuppressLint("NotifyDataSetChanged")
  fun setList(laoIdList: List<String>) {
    this.laoIdList = laoIdList
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LAOListItemViewHolder {
    val layoutInflater = LayoutInflater.from(parent.context)
    val view = layoutInflater.inflate(R.layout.lao_card, parent, false)
    return LAOListItemViewHolder(view)
  }

  override fun onBindViewHolder(holder: LAOListItemViewHolder, position: Int) {
    val laoId = laoIdList[position]
    val cardView = holder.cardView
    cardView.setOnClickListener {
      Timber.tag(TAG).d("Opening lao detail activity on the home tab for lao %s", laoId)
      activity.startActivity(LaoActivity.newIntentForLao(activity, laoId))
    }

    val laoTitle = holder.laoTitle
    val laoRole = holder.laoRole
    try {
      val laoView = homeViewModel.getLaoView(laoId)
      laoTitle.text = laoView.name
        //laoRole.text = laoView.role.toString()
    } catch (e: UnknownLaoException) {
      Timber.tag(TAG).e(e)
      error("Lao with id $laoId is supposed to be present")
    }
  }

  override fun getItemId(position: Int): Long {
    return position.toLong()
  }

  override fun getItemCount(): Int {
    return laoIdList.size
  }

  class LAOListItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val cardView: CardView
    val laoTitle: TextView
    val laoRole: TextView

    init {
      cardView = itemView.findViewById(R.id.lao_card_view)
      laoTitle = itemView.findViewById(R.id.lao_card_title_text_view)
      laoRole = itemView.findViewById(R.id.lao_card_role_text_view)
    }
  }

  companion object {
    private val TAG = LAOListAdapter::class.java.simpleName
  }
}
