package com.github.dedis.popstellar.ui.home;

import android.app.Activity;
import android.util.Log;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;

import java.util.List;

public class LAOListAdapter extends RecyclerView.Adapter<LAOListAdapter.LAOListItemViewHolder> {

  private static final String TAG = LAOListAdapter.class.getSimpleName();

  private final Activity activity;

  private List<Lao> laos;
  private final boolean openLaoDetail;

  public LAOListAdapter(List<Lao> laos, Activity activity, boolean openLaoDetail) {
    this.activity = activity;
    this.openLaoDetail = openLaoDetail;
    setList(laos);
  }

  public void setList(List<Lao> laos) {
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
          String laoId = lao.getId();
          if (openLaoDetail) {
            Log.d(TAG, "Opening lao detail activity on the home tab for lao " + laoId);
            activity.startActivity(LaoDetailActivity.newIntentForLao(activity, laoId));
          } else {
            Log.d(TAG, "Opening lao detail activity on the wallet tab for lao " + laoId);
            activity.startActivity(LaoDetailActivity.newIntentForWallet(activity, laoId));
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
