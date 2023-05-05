package com.github.dedis.popstellar.ui.home;

import android.annotation.SuppressLint;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.model.objects.security.PublicKey;

import java.util.List;

public class WitnessesListAdapter
    extends RecyclerView.Adapter<WitnessesListAdapter.WitnessesListItemViewHolder> {

  private List<PublicKey> witnessesList;

  @SuppressLint("NotifyDataSetChanged")
  public void setList(List<PublicKey> witnessesList) {
    this.witnessesList = witnessesList;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public WitnessesListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
    View view = layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
    return new WitnessesListItemViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull WitnessesListItemViewHolder holder, int position) {
    PublicKey publicKey = witnessesList.get(position);
    holder.publicKey.setText(publicKey.getEncoded());
  }

  @Override
  public int getItemCount() {
    return witnessesList == null ? 0 : witnessesList.size();
  }

  static class WitnessesListItemViewHolder extends RecyclerView.ViewHolder {
    private final TextView publicKey;

    public WitnessesListItemViewHolder(@NonNull View itemView) {
      super(itemView);
      publicKey = itemView.findViewById(android.R.id.text1);
    }
  }
}
