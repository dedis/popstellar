package com.github.dedis.popstellar.ui.lao.witness;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class WitnessPagerAdapter extends FragmentStateAdapter {

  public WitnessPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
    super(fragmentActivity);
  }

  @NonNull
  @Override
  public Fragment createFragment(int position) {
    if (position == 0) {
      return new WitnessesFragment();
    } else {
      return new WitnessMessageFragment();
    }
  }

  @Override
  public int getItemCount() {
    return 2; // We have 2 tabs
  }
}
