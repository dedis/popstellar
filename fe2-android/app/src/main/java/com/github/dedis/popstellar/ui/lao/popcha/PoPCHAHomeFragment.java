package com.github.dedis.popstellar.ui.lao.popcha;

import static com.github.dedis.popstellar.ui.lao.LaoActivity.setCurrentFragment;

import android.os.Bundle;
import android.view.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.PopchaHomeFragmentBinding;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.ui.qrcode.QrScannerFragment;
import com.github.dedis.popstellar.ui.qrcode.ScanningAction;

public class PoPCHAHomeFragment extends Fragment {

  private LaoViewModel laoViewModel;

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

    PoPCHAViewModel popCHAViewModel =
        LaoActivity.obtainPoPCHAViewModel(requireActivity(), laoViewModel.getLaoId());
    PopchaHomeFragmentBinding binding =
        PopchaHomeFragmentBinding.inflate(inflater, container, false);

    binding.popchaHeader.setText(
        String.format(
            getResources().getString(R.string.popcha_header), popCHAViewModel.getLaoId()));

    binding.popchaScanner.setOnClickListener(v -> openScanner());

    popCHAViewModel
        .getTextDisplayed()
        .observe(
            getViewLifecycleOwner(),
            stringSingleEvent -> {
              String url = stringSingleEvent.getContentIfNotHandled();
              if (url != null) {
                binding.popchaText.setText(url);
              }
            });

    popCHAViewModel
        .getIsRequestCompleted()
        .observe(
            getViewLifecycleOwner(),
            booleanSingleEvent -> {
              Boolean finished = booleanSingleEvent.getContentIfNotHandled();
              if (finished != null && finished) {
                closeScanner();
                popCHAViewModel.deactivateRequestCompleted();
              }
            });

    handleBackNav();
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    laoViewModel.setPageTitle(R.string.popcha);
    laoViewModel.setIsTab(true);
  }

  private void openScanner() {
    laoViewModel.setIsTab(false);
    setCurrentFragment(
        getParentFragmentManager(),
        R.id.fragment_qr_scanner,
        () -> QrScannerFragment.newInstance(ScanningAction.ADD_POPCHA));
  }

  private void closeScanner() {
    laoViewModel.setIsTab(true);
    getParentFragmentManager().popBackStack();
  }

  public static void openFragment(FragmentManager manager) {
    LaoActivity.setCurrentFragment(manager, R.id.fragment_popcha_home, PoPCHAHomeFragment::new);
  }

  private void handleBackNav() {
    LaoActivity.addBackNavigationCallbackToEvents(requireActivity(), getViewLifecycleOwner(), TAG);
  }
}
