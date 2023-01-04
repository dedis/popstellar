package com.github.dedis.popstellar.ui.detail.token;

import android.annotation.SuppressLint;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.RollCall;

import java.util.ArrayList;
import java.util.List;

public class TokenListAdapter extends RecyclerView.Adapter<TokenListAdapter.TokensViewHolder> {
  List<RollCall> rollCalls = new ArrayList<>();

  @NonNull
  @Override
  public TokensViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.token_layout, parent, false);
    return new TokensViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull TokensViewHolder holder, int position) {
    RollCall rollCall = rollCalls.get(position);
    holder.rollCallTitle.setText(rollCall.getName());
    holder.status.setVisibility(View.GONE);
  }

  @Override
  public int getItemCount() {
    return rollCalls.size();
  }

  @SuppressLint("NotifyDataSetChanged")
  public void replaceList(List<RollCall> rollCalls) {
    this.rollCalls = new ArrayList<>(rollCalls);
    notifyDataSetChanged();
  }

  public static class TokensViewHolder extends RecyclerView.ViewHolder {
    TextView rollCallTitle;
    TextView status;

    public TokensViewHolder(@NonNull View itemView) {
      super(itemView);
      rollCallTitle = itemView.findViewById(R.id.token_layout_rc_title);
      status = itemView.findViewById(R.id.token_layout_status);
    }
  }
}
