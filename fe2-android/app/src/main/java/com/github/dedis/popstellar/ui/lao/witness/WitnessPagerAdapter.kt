package com.github.dedis.popstellar.ui.lao.witness

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class WitnessPagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
  override fun createFragment(position: Int): Fragment {
    return if (position == 0) {
      WitnessMessageFragment()
    } else {
      WitnessesFragment()
    }
  }

  override fun getItemCount(): Int {
    return 2 // We have 2 tabs
  }
}
