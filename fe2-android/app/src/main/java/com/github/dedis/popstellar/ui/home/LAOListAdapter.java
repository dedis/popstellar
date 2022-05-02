package com.github.dedis.popstellar.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;

import com.github.dedis.popstellar.databinding.HomeLaoLayoutBinding;
import com.github.dedis.popstellar.model.objects.Lao;

import java.util.List;

public class LAOListAdapter extends BaseAdapter {

  private final HomeViewModel homeViewModel;

  private List<Lao> laos;

  private final LifecycleOwner lifecycleOwner;

  private final boolean openLaoDetail;

<<<<<<< HEAD
  public LAOListAdapter(
      List<Lao> laos, HomeViewModel homeViewModel, LifecycleOwner activity, boolean openLaoDetail) {
=======
  public LAOListAdapter(List<Lao> laos, HomeViewModel homeViewModel, boolean openLaoDetail) {

>>>>>>> master
    this.homeViewModel = homeViewModel;
    setList(laos);
    lifecycleOwner = activity;
    this.openLaoDetail = openLaoDetail;
  }

  public void replaceList(List<Lao> laos) {
    setList(laos);
  }

  private void setList(List<Lao> laos) {
    this.laos = laos;
    notifyDataSetChanged();
  }

  @Override
<<<<<<< HEAD
  public int getCount() {
    return laos != null ? laos.size() : 0;
  }

  @Override
  public Object getItem(int position) {
    return laos.get(position);
=======
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
            homeViewModel.openLAO(lao.getId());
          } else {
            homeViewModel.openLaoWallet(lao.getId());
          }
        });

    TextView laoTitle = holder.laoTitle;
    laoTitle.setText(lao.getName());
>>>>>>> master
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View view, ViewGroup viewGroup) {
    HomeLaoLayoutBinding binding;
    if (view == null) {
      // inflate
      LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());

      binding = HomeLaoLayoutBinding.inflate(inflater, viewGroup, false);
    } else {
      binding = DataBindingUtil.getBinding(view);
    }

<<<<<<< HEAD
    if (binding == null) throw new IllegalStateException("Binding could not be find in the view");
=======
  static class LAOListItemViewHolder extends RecyclerView.ViewHolder {
>>>>>>> master

    LAOItemUserActionsListener userActionsListener =
        lao -> {
          if (openLaoDetail) {
            homeViewModel.openLAO(lao.getId());
          } else {
            homeViewModel.openLaoWallet(lao.getId());
          }
        };

    binding.setLao(laos.get(position));
    binding.setLifecycleOwner(lifecycleOwner);

<<<<<<< HEAD
    binding.setListener(userActionsListener);

    binding.executePendingBindings();

    return binding.getRoot();
=======
      cardView = itemView.findViewById(R.id.lao_card_view);
      laoTitle = itemView.findViewById(R.id.lao_card_text_view);
    }
>>>>>>> master
  }
}
