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
  private PoPCHAViewModel popCHAViewModel;

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

    popCHAViewModel = LaoActivity.obtainPoPCHAViewModel(requireActivity(), laoViewModel.getLaoId());
    PopchaHomeFragmentBinding binding =
        PopchaHomeFragmentBinding.inflate(inflater, container, false);

    binding.popchaHeader.setText(
        String.format(
            getResources().getString(R.string.popcha_header), popCHAViewModel.getLaoId()));

    binding.popchaScanner.setOnClickListener(
        v -> {
          String data =
              "http://localhost:9100/authorize?response_mode=query&response_type=id_token&client_id=WAsabGuEe5m1KpqOZQKgmO7UShX84Jmd_eaenOZ32wU&redirect_uri=http%3A%2F%2Flocalhost%3A8000%2Fcb&scope=openid+profile&login_hint=ApvVz51aIZPVXa_wTfEniIlEqC5OY-ZH9BhLwxD6mi4=&nonce=frXgNl-IxJPzsNia07f_3yV0ECYlWOb2RXG_SGvATKcJ7-s0LthmboTrnMqlQS1RnzmV9hW0iumu_5NwAqXwGA&state=m_9r5sPUD8NoRIdVVYFMyYCOb-8xh1d2q8l-pKDXO0sn9TWnR_2nmC8MfVj1COHZsh1rElqimOTLAp3CbhbYJQ";
          popCHAViewModel.handleData(data);
          // openScanner();
        });

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
              if (finished.equals(Boolean.TRUE)) {
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
