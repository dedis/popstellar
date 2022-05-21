package com.github.dedis.popstellar.ui.detail.witness;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import java.util.List;

/** Adapter to show witnesses of an Event */
public class WitnessListViewAdapter
    extends RecyclerView.Adapter<WitnessListViewAdapter.WitnessViewHolder> {

  private final LaoDetailViewModel viewModel;

  private List<PublicKey> witnesses;

  private final LifecycleOwner lifecycleOwner;

  public WitnessListViewAdapter(
      List<PublicKey> witness, LaoDetailViewModel viewModel, LifecycleOwner activity) {
    this.viewModel = viewModel;
    setList(witness);
    lifecycleOwner = activity;
  }

  public void replaceList(List<PublicKey> witnesses) {
    setList(witnesses);
  }

  private void setList(List<PublicKey> witnesses) {
    this.witnesses = witnesses;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public WitnessViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.witnesses_layout, parent, false);
    return new WitnessViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull WitnessViewHolder holder, int position) {
    String witness = witnesses.get(position).getEncoded();
    holder.witnessKey.setText(witness);
  }

  @Override
  public int getItemCount() {
    return witnesses.size();
  }

  public static class WitnessViewHolder extends RecyclerView.ViewHolder {
    private final TextView witnessKey;

    public WitnessViewHolder(@NonNull View itemView) {
      super(itemView);
      witnessKey = itemView.findViewById(R.id.text_view_witness_name);
    }
  }
}
