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

  public LAOListAdapter(
      List<Lao> laos, HomeViewModel homeViewModel, LifecycleOwner activity, boolean openLaoDetail) {
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
  public int getCount() {
    return laos != null ? laos.size() : 0;
  }

  @Override
  public Object getItem(int position) {
    return laos.get(position);
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

    LAOItemUserActionsListener userActionsListener =
        lao -> {
          if (openLaoDetail) {
            homeViewModel.openLAO(lao.getChannel());
          } else {
            homeViewModel.openLaoWallet(lao.getChannel());
          }
        };

    binding.setLao(laos.get(position));
    binding.setLifecycleOwner(lifecycleOwner);

    binding.setListener(userActionsListener);

    binding.executePendingBindings();

    return binding.getRoot();
  }
}
