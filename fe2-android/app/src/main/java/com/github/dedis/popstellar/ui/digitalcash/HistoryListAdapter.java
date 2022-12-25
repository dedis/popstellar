package com.github.dedis.popstellar.ui.digitalcash;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.KeyException;

import java.util.*;
import java.util.stream.Collectors;

public class HistoryListAdapter extends RecyclerView.Adapter<HistoryListAdapter.HistoryViewHolder> {
  private static final String TAG = HistoryListAdapter.class.getSimpleName();
  private List<TransactionHistoryElement> transactions;
  private final Map<String, Boolean> expandMap = new HashMap<>();
  private final DigitalCashViewModel viewModel;
  private final FragmentActivity activity;

  public HistoryListAdapter(DigitalCashViewModel viewModel, FragmentActivity activity) {
    this.viewModel = viewModel;
    this.activity = activity;
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
    TransactionHistoryElement element = transactions.get(position);
    if (element == null) {
      Log.d(TAG, "element is null");
      return;
    }
    String transactionId = element.getId();
    Boolean expandObject = expandMap.get(transactionId);
    boolean expand = expandObject != null && expandObject;

    holder.detailLayout.setVisibility(expand ? View.VISIBLE : View.GONE);
    holder.expandIcon.setRotation(expand ? 180f : 0f);
    holder.transactionTypeTitle.setText(
        element.isSender() ? R.string.digital_cash_sent : R.string.digital_cash_received);

    holder.transactionTypeValue.setText(element.getValue());

    holder.transactionIdValue.setText(transactionId);

    holder.transactionProvenanceTitle.setText(
        element.isSender() ? R.string.digital_cash_to : R.string.digital_cash_from);
    holder.transactionProvenanceValue.setText(element.getSenderOrReceiver());

    View.OnClickListener listener =
        v -> {
          Log.d(TAG, "transaction is " + transactionId + " position is " + position);
          boolean expandStatus = expandMap.get(transactionId);
          expandMap.put(transactionId, !expandStatus);
          notifyItemChanged(position);
        };
    holder.transactionCardView.setOnClickListener(listener);
    holder.expandIcon.setOnClickListener(listener); // For some reason needed
  }

  @Override
  public int getItemCount() {
    return transactions.size();
  }

  @SuppressLint("NotifyDataSetChanged") // Because our current implementation warrants it
  public void setList(List<TransactionObject> transactions) {
    this.transactions = buildTransactionList(transactions);
    transactions.forEach(
        transaction -> expandMap.putIfAbsent(transaction.getTransactionId(), false));
    notifyDataSetChanged();
  }

  private List<TransactionHistoryElement> buildTransactionList(
      List<TransactionObject> transactionObjects) {
    ArrayList<TransactionHistoryElement> transactionHistoryElements = new ArrayList<>();
    PublicKey ownKey;
    try {
      ownKey = viewModel.getValidToken().getPublicKey();
    } catch (KeyException e) {
      ErrorUtils.logAndShow(activity, TAG, e, R.string.error_retrieve_own_token);
      return new ArrayList<>();
    }
    for (TransactionObject transactionObject : transactionObjects) {
      // To know if we are in input or not. We assume that no two different person
      boolean isSender = transactionObject.isSender(ownKey);
      boolean isIssuance = transactionObject.isCoinBaseTransaction();
      transactionHistoryElements.addAll(
          transactionObject.getOutputs().stream()
              // If we are in input, we want all output except us. If we are not in input,
              // we want all output we are in: so we filter isInInput XOR isInOutput
              // if it is an issuance, we want all outputs where we are
              .filter(
                  outputObject -> {
                    boolean isOwn = outputObject.getPubKeyHash().equals(ownKey.computeHash());
                    return isIssuance && isOwn || !isIssuance && (isSender ^ isOwn);
                  })
              .map(
                  outputObject ->
                      new TransactionHistoryElement(
                          isSender
                              ? outputObject.getPubKeyHash()
                              : transactionObject.getInputs().get(0).getPubKey().getEncoded(),
                          String.valueOf(outputObject.getValue()),
                          transactionObject.getTransactionId(),
                          !isIssuance && isSender))
              .collect(Collectors.toList()));
    }
    return transactionHistoryElements;
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

  private static class TransactionHistoryElement {
    private final String senderOrReceiver;
    private final String value;
    private final String id;
    private final boolean isSender;

    public TransactionHistoryElement(
        String senderOrReceiver, String value, String id, boolean isSender) {
      this.senderOrReceiver = senderOrReceiver;
      this.value = value;
      this.id = id;
      this.isSender = isSender;
    }

    public String getSenderOrReceiver() {
      return senderOrReceiver;
    }

    public String getValue() {
      return value;
    }

    public String getId() {
      return id;
    }

    public boolean isSender() {
      return isSender;
    }

    @NonNull
    @Override
    public String toString() {
      return "TransactionHistoryElement{"
          + "senderOrReceiver='"
          + senderOrReceiver
          + '\''
          + ", value='"
          + value
          + '\''
          + ", id='"
          + id
          + '\''
          + ", isSender="
          + isSender
          + '}';
    }
  }
}
