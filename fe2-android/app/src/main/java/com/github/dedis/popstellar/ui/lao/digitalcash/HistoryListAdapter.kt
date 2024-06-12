package com.github.dedis.popstellar.ui.lao.digitalcash

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.model.objects.OutputObject
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.ui.lao.digitalcash.HistoryListAdapter.HistoryViewHolder
import com.github.dedis.popstellar.utility.error.ErrorUtils.logAndShow
import com.github.dedis.popstellar.utility.error.keys.KeyException
import java.util.function.Consumer
import java.util.stream.Collectors
import timber.log.Timber

class HistoryListAdapter(
    private val viewModel: DigitalCashViewModel,
    private val activity: FragmentActivity
) : RecyclerView.Adapter<HistoryViewHolder>() {

  private var transactions: List<TransactionHistoryElement> = ArrayList()
  private val expandMap: MutableMap<String, Boolean> = HashMap()

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
    val view =
        LayoutInflater.from(parent.context)
            .inflate(R.layout.history_transaction_layout, parent, false)
    return HistoryViewHolder(view)
  }

  override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
    val element = transactions[position]

    val transactionId: String = element.id
    val expandObject = expandMap[transactionId]
    val expand = expandObject != null && expandObject

    holder.detailLayout.visibility = if (expand) View.VISIBLE else View.GONE
    holder.expandIcon.rotation = if (expand) 180f else 0f
    holder.transactionTypeTitle.setText(
        if (element.isSender) R.string.digital_cash_sent else R.string.digital_cash_received)
    holder.transactionTypeValue.text = element.value
    holder.transactionIdValue.text = transactionId
    holder.transactionProvenanceTitle.setText(
        if (element.isSender) R.string.digital_cash_to else R.string.digital_cash_from)
    holder.transactionProvenanceValue.text =
        if (element.senderOrReceiver.encoded == viewModel.organizer.encoded)
            element.senderOrReceiver.encoded + " (organizer)"
        else element.senderOrReceiver.getUsername()

    val listener =
        View.OnClickListener {
          Timber.tag(TAG).d("transaction is %s position is %s", transactionId, position)
          val expandStatus = expandMap[transactionId]
          expandMap[transactionId] = !(expandStatus ?: true)
          notifyItemChanged(position)
        }

    holder.transactionCardView.setOnClickListener(listener)
    holder.expandIcon.setOnClickListener(listener) // For some reason needed
  }

  override fun getItemCount(): Int {
    return transactions.size
  }

  @SuppressLint("NotifyDataSetChanged") // Because our current implementation warrants it
  fun setList(transactions: List<TransactionObject>) {
    this.transactions = buildTransactionList(transactions)
    transactions.forEach(
        Consumer { transaction: TransactionObject ->
          expandMap.putIfAbsent(transaction.transactionId, false)
        })
    notifyDataSetChanged()
  }

  private fun buildTransactionList(
      transactionObjects: List<TransactionObject>
  ): List<TransactionHistoryElement> {
    val transactionHistoryElements = ArrayList<TransactionHistoryElement>()

    val ownKey: PublicKey =
        try {
          viewModel.validToken.publicKey
        } catch (e: KeyException) {
          logAndShow(activity, TAG, e, R.string.error_retrieve_own_token)
          return ArrayList()
        }

    for (transactionObject in transactionObjects) {
      // To know if we are in input or not. We assume that no two different person
      val isSender = transactionObject.isSender(ownKey)
      val isIssuance = transactionObject.isCoinBaseTransaction
      transactionHistoryElements.addAll(
          transactionObject.outputs
              .stream() // If we are in input, we want all output except us. If we are not in input,
              // we want all output we are in: so we filter isInInput XOR isInOutput
              // if it is an issuance, we want all outputs where we are
              .filter { outputObject: OutputObject ->
                val isOwn = outputObject.pubKeyHash == ownKey.computeHash()
                isIssuance && isOwn || !isIssuance && isSender xor isOwn
              }
              .map { outputObject: OutputObject ->
                TransactionHistoryElement(
                    // TODO : was previously outputObject.pubKeyHash, but was wrong (hash is smaller
                    // than 32 bytes and is clearly not the receivers public key)
                    // I set it to ownKey so that it works for now, but should actually show the
                    // public key of the receiver | Maxime @Kaz-ookid 06.2025
                    if (isSender) ownKey else PublicKey(transactionObject.inputs[0].pubKey.encoded),
                    outputObject.value.toString(),
                    transactionObject.transactionId,
                    !isIssuance && isSender)
              }
              .collect(Collectors.toList()))
    }

    return transactionHistoryElements
  }

  class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val expandIcon: ImageView = itemView.findViewById(R.id.history_transaction_expand_icon)
    val transactionTypeTitle: TextView = itemView.findViewById(R.id.history_transaction_type_title)
    val transactionTypeValue: TextView = itemView.findViewById(R.id.history_transaction_type_value)
    val transactionProvenanceTitle: TextView =
        itemView.findViewById(R.id.history_transaction_provenance_title)
    val transactionProvenanceValue: TextView =
        itemView.findViewById(R.id.history_transaction_provenance_value)
    val transactionIdValue: TextView =
        itemView.findViewById(R.id.history_transaction_transaction_id_value)
    val detailLayout: ConstraintLayout =
        itemView.findViewById(R.id.history_transaction_detail_layout)
    val transactionCardView: CardView = itemView.findViewById(R.id.transaction_card_view)
  }

  private class TransactionHistoryElement(
      val senderOrReceiver: PublicKey,
      val value: String,
      val id: String,
      val isSender: Boolean
  ) {

    override fun toString(): String {
      return "TransactionHistoryElement{senderOrReceiverHash='${senderOrReceiver.encoded}', senderOrReceiverUsername='${senderOrReceiver.getUsername()}',value='$value', " +
          "id='$id', isSender=$isSender}"
    }
  }

  companion object {
    private val TAG: String = HistoryListAdapter::class.java.simpleName
  }
}
