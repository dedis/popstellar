package com.github.dedis.popstellar.ui.lao.witness;

import android.os.Bundle;
import android.view.*;

import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WitnessingFragment extends Fragment {

  private static final Logger logger = LogManager.getLogger(WitnessingFragment.class);

  public WitnessingFragment() {
    // Required empty public constructor
  }

  public static WitnessingFragment newInstance() {
    return new WitnessingFragment();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.witnessing_fragment, container, false);

    ViewPager2 viewPager = view.findViewById(R.id.witnessing_view_pager);
    WitnessPagerAdapter adapter = new WitnessPagerAdapter(requireActivity());
    viewPager.setAdapter(adapter);

    TabLayout tabLayout = view.findViewById(R.id.witnessing_tab_layout);
    new TabLayoutMediator(
            tabLayout,
            viewPager,
            ((tab, position) ->
                tab.setText(position == 0 ? R.string.witnesses : R.string.messages)))
        .attach();
    handleBackNav();
    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    LaoViewModel viewModel = LaoActivity.obtainViewModel(requireActivity());
    viewModel.setPageTitle(R.string.witnessing);
    viewModel.setIsTab(true);
  }

  private void handleBackNav() {
    LaoActivity.addBackNavigationCallbackToEvents(
        requireActivity(), getViewLifecycleOwner(), logger);
  }
}
