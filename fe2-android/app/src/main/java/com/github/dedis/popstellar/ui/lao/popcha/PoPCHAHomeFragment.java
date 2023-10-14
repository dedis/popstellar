package com.github.dedis.popstellar.ui.lao.popcha;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.PopchaHomeFragmentBinding;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;

public class PoPCHAHomeFragment extends Fragment {
  private PopchaHomeFragmentBinding binding;
  private LaoViewModel laoViewModel;
  private PoPCHAViewModel poPCHAViewModel;

  public PoPCHAHomeFragment() {
    // Required public empty constructor
  }

  public static PoPCHAHomeFragment newInstance() {
    return new PoPCHAHomeFragment();
  }

  public static final String TAG = PoPCHAHomeFragment.class.getSimpleName();

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    laoViewModel = LaoActivity.obtainViewModel(requireActivity());

    poPCHAViewModel = LaoActivity.obtainPoPCHAViewModel(requireActivity(), laoViewModel.getLaoId());
    binding = PopchaHomeFragmentBinding.inflate(inflater, container, false);

    binding.popchaHeader.setText(
        String.format(
            getResources().getString(R.string.popcha_header), poPCHAViewModel.getLaoId()));

    binding.popchaScanner.setOnClickListener(v -> {});

    handleBackNav();
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    laoViewModel.setPageTitle(R.string.popcha);
    laoViewModel.setIsTab(true);
  }

  public static void openFragment(FragmentManager manager) {
    LaoActivity.setCurrentFragment(manager, R.id.fragment_popcha_home, PoPCHAHomeFragment::new);
  }

  private void handleBackNav() {
    LaoActivity.addBackNavigationCallbackToEvents(requireActivity(), getViewLifecycleOwner(), TAG);
  }
}
