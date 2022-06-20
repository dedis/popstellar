package com.github.dedis.popstellar.ui.digitalcash;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.TransactionObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.HistoryViewHolder> {
  private List<TransactionObject> transactions;
  private final String[] types = new String[] {"Sent", "Received"};
  private final String[] amounts = new String[] {"2.7", "2303"};
  private final String[] provenanceType = new String[] {"To", "From"};
  private final String[] provenanceValue =
      new String[] {"0x5654556456456456456456456465=", "9872554566546516="};
  private final String[] id = new String[] {"0x5465", "0x987456"};
  private Map<String, Boolean> expandMap;
  private final DigitalCashViewModel viewModel;

  public HistoryListAdapter(List<TransactionObject> transactions, DigitalCashViewModel viewModel) {
    if (transactions == null) {
      throw new IllegalArgumentException();
    }
    this.transactions = transactions;
    expandMap = new HashMap<>();
    Arrays.stream(id).sequential().forEach(s -> expandMap.put(s, false));
    this.viewModel = viewModel;
  }

  @NonNull
  @Override
  public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext())
            .inflate(R.layout.history_transaction_layout, parent, false);
    return new HistoryViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
    String transactionId = id[position];

    boolean expand = expandMap.get(transactionId);

    holder.detailLayout.setVisibility(expand ? View.VISIBLE : View.GONE);
    holder.expandIcon.setRotation(expand ? 180f : 0f);
    holder.transactionTypeTitle.setText(types[position]);
    holder.transactionTypeValue.setText(amounts[position]);

    holder.transactionIdValue.setText(transactionId);

    holder.transactionProvenanceTitle.setText(provenanceType[position]);
    holder.transactionProvenanceValue.setText(provenanceValue[position]);

    View.OnClickListener listener =
        v -> {
          boolean expandStatus = expandMap.get(transactionId);
          expandMap.put(transactionId, !expandStatus);
          notifyItemChanged(position);
        };
    holder.transactionCardView.setOnClickListener(listener);
    holder.expandIcon.setOnClickListener(listener); // For some reason needed
  }

  @Override
  public int getItemCount() {
    //    return transactions.parallelStream()
    //        .reduce(
    //            0,
    //            (accumulator, element) ->
    //                accumulator + element.getInputs().size() + element.getOutputs().size(),
    //            Integer::sum); //We compute the sum of each input and output of each transaction
    return 2;
  }

  public void replaceList(List<TransactionObject> transactions) {
    setList(transactions);
  }

  @SuppressLint("NotifyDataSetChanged") // Because our current implementation warrants it
  private void setList(List<TransactionObject> transactions) {
    this.transactions = new ArrayList<>(transactions);
    notifyDataSetChanged();
  }

  public static class HistoryViewHolder extends RecyclerView.ViewHolder {
    private final ImageView expandIcon;
    private final TextView transactionTypeTitle;
    private final TextView transactionTypeValue;
    private final TextView transactionProvenanceTitle;
    private final TextView transactionProvenanceValue;
    private final TextView transactionIdValue;
    private final ConstraintLayout detailLayout;
    private final CardView transactionCardView;

    public HistoryViewHolder(@NonNull View itemView) {
      super(itemView);
      expandIcon = itemView.findViewById(R.id.history_transaction_expand_icon);
      transactionTypeTitle = itemView.findViewById(R.id.history_transaction_type_title);
      transactionTypeValue = itemView.findViewById(R.id.history_transaction_type_value);
      transactionProvenanceValue = itemView.findViewById(R.id.history_transaction_provenance_value);
      transactionProvenanceTitle = itemView.findViewById(R.id.history_transaction_provenance_title);
      transactionIdValue = itemView.findViewById(R.id.history_transaction_transaction_id_value);
      detailLayout = itemView.findViewById(R.id.history_transaction_detail_layout);
      transactionCardView = itemView.findViewById(R.id.transaction_card_view);
    }
  }
}
