package com.github.dedis.popstellar.ui.lao.token;

import android.annotation.SuppressLint;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class TokenListAdapter extends RecyclerView.Adapter<TokenListAdapter.TokensViewHolder> {

  private List<RollCall> rollCalls = new ArrayList<>();
  private final FragmentActivity activity;

  public TokenListAdapter(FragmentActivity activity) {
    this.activity = activity;
  }

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
    holder.materialCardView.setOnClickListener(
        view ->
            LaoActivity.setCurrentFragment(
                activity.getSupportFragmentManager(),
                R.id.fragment_token,
                () -> TokenFragment.newInstance(rollCall.getPersistentId())));
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
    MaterialCardView materialCardView;
    TextView rollCallTitle;
    TextView status;

    public TokensViewHolder(@NonNull View itemView) {
      super(itemView);
      materialCardView = itemView.findViewById(R.id.token_card_layout);
      rollCallTitle = itemView.findViewById(R.id.token_layout_rc_title);
      status = itemView.findViewById(R.id.token_layout_status);
    }
  }
}
