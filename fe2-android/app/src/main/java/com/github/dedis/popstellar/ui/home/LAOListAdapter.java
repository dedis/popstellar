package com.github.dedis.popstellar.ui.home;

import android.app.Activity;
import android.util.Log;
import android.view.*;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import java.util.List;

public class LAOListAdapter extends RecyclerView.Adapter<LAOListAdapter.LAOListItemViewHolder> {

  private static final String TAG = LAOListAdapter.class.getSimpleName();

  private final Activity activity;

  private List<String> laoIdList;
  private final boolean openLaoDetail;

  private final HomeViewModel homeViewModel;

  public LAOListAdapter(HomeViewModel homeViewModel, Activity activity, boolean openLaoDetail) {
    this.activity = activity;
    this.openLaoDetail = openLaoDetail;
    this.homeViewModel = homeViewModel;
  }

  public void setList(List<String> laoIdList) {
    this.laoIdList = laoIdList;
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

    final String laoId = laoIdList.get(position);

    CardView cardView = holder.cardView;
    cardView.setOnClickListener(
        v -> {
          if (openLaoDetail) {
            Log.d(TAG, "Opening lao detail activity on the home tab for lao " + laoId);
            activity.startActivity(LaoDetailActivity.newIntentForLao(activity, laoId));
          } else {
            Log.d(TAG, "Opening lao detail activity on the wallet tab for lao " + laoId);
            activity.startActivity(LaoDetailActivity.newIntentForWallet(activity, laoId));
          }
        });

    TextView laoTitle = holder.laoTitle;

    try {
      LaoView laoView = homeViewModel.getLaoView(laoId);
      laoTitle.setText(laoView.getName());
    } catch (UnknownLaoException e) {
      e.printStackTrace();
      throw new IllegalStateException("Lao with id " + laoId + " is supposed to be present");
    }
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public int getItemCount() {
    return laoIdList != null ? laoIdList.size() : 0;
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
