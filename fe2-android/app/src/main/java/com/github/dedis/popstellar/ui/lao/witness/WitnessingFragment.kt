package com.github.dedis.popstellar.ui.lao.witness

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.github.dedis.popstellar.R
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.addBackNavigationCallbackToEvents
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainViewModel
import com.github.dedis.popstellar.ui.lao.LaoActivity.Companion.obtainWitnessingViewModel
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class WitnessingFragment : Fragment() {
  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    // Inflate the layout for this fragment
    val view = inflater.inflate(R.layout.witnessing_fragment, container, false)

    val viewPager = view.findViewById<ViewPager2>(R.id.witnessing_view_pager)
    val adapter = WitnessPagerAdapter(requireActivity())
    viewPager.adapter = adapter

    val tabLayout = view.findViewById<TabLayout>(R.id.witnessing_tab_layout)
    TabLayoutMediator(tabLayout, viewPager) { tab: TabLayout.Tab, position: Int ->
          tab.setText(if (position == 0) R.string.messages else R.string.witnesses)
        }
        .attach()
    if (!obtainWitnessingViewModel(requireActivity(), obtainViewModel(requireActivity()).laoId)
        .isWitness) {
      Toast.makeText(requireContext(), R.string.not_a_witness, Toast.LENGTH_SHORT).show()
    }

    handleBackNav()

    return view
  }

  override fun onResume() {
    super.onResume()
    val viewModel = obtainViewModel(requireActivity())
    viewModel.setPageTitle(R.string.witnessing)
    viewModel.setIsTab(true)
  }

  private fun handleBackNav() {
    addBackNavigationCallbackToEvents(requireActivity(), viewLifecycleOwner, TAG)
  }

  companion object {
    val TAG: String = WitnessingFragment::class.java.simpleName

    fun newInstance(): WitnessingFragment {
      return WitnessingFragment()
    }
  }
}
