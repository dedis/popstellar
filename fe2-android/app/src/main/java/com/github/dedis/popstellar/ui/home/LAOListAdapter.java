package com.github.dedis.popstellar.ui.home;

import android.app.Activity;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Lao;

import java.util.List;

public class LAOListAdapter extends RecyclerView.Adapter<LAOListAdapter.LAOListItemViewHolder> {

  private final Activity activity;

  private List<Lao> laos;

  private final boolean openLaoDetail;

  public LAOListAdapter(List<Lao> laos, Activity activity, boolean openLaoDetail) {
    this.activity = activity;
    setList(laos);
    this.openLaoDetail = openLaoDetail;
  }

  public void replaceList(List<Lao> laos) {
    setList(laos);
  }

  private void setList(List<Lao> laos) {
    this.laos = laos;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public LAOListItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
    View view = layoutInflater.inflate(R.layout.lao_card, parent, false);
    return new LAOListItemViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull LAOListItemViewHolder holder, int position) {

    final Lao lao = laos.get(position);

    CardView cardView = holder.cardView;
    cardView.setOnClickListener(
        v -> {
          if (openLaoDetail) {
            HomeViewModel.openLao(activity, lao.getId());
          } else {
            HomeViewModel.openLaoWallet(activity, lao.getId());
          }
        });

    TextView laoTitle = holder.laoTitle;
    laoTitle.setText(lao.getName());
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public int getItemCount() {
    return laos != null ? laos.size() : 0;
  }

  static class LAOListItemViewHolder extends RecyclerView.ViewHolder {

    private final CardView cardView;
    private final TextView laoTitle;

    public LAOListItemViewHolder(@NonNull View itemView) {
      super(itemView);

      cardView = itemView.findViewById(R.id.lao_card_view);
      laoTitle = itemView.findViewById(R.id.lao_card_text_view);
    }
  }
}
