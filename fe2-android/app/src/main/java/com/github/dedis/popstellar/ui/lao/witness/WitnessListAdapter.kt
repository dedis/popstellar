package com.github.dedis.popstellar.ui.lao.witness

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.ui.lao.witness.WitnessListAdapter.WitnessViewHolder

/** Adapter to show witnesses of an Event */
class WitnessListAdapter(witness: List<PublicKey>?) : RecyclerView.Adapter<WitnessViewHolder>() {
  private var witnesses: List<PublicKey> = ArrayList()

  init {
    setList(witness)
  }

  fun replaceList(witnesses: List<PublicKey>?) {
    setList(witnesses)
  }

  @SuppressLint("NotifyDataSetChanged")
  private fun setList(witnesses: List<PublicKey>?) {
    this.witnesses = witnesses ?: return
    notifyDataSetChanged()
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WitnessViewHolder {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.witnesses_layout, parent, false)
    return WitnessViewHolder(view)
  }

  override fun onBindViewHolder(holder: WitnessViewHolder, position: Int) {
    val witness = witnesses[position].encoded
    holder.witnessKey.text = witness
  }

  override fun getItemCount(): Int {
    return witnesses.size
  }

  class WitnessViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val witnessKey: TextView = itemView.findViewById(R.id.text_view_witness_name)
  }
}
