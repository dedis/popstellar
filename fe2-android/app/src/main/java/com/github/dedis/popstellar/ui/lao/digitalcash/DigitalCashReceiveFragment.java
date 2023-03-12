package com.github.dedis.popstellar.ui.lao.digitalcash;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.*;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashReceiveFragmentBinding;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.qrcode.PopTokenData;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.google.gson.Gson;

import net.glxn.qrgen.android.QRCode;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashReceiveFragment#newInstance}
 * factory method to create an instance of this fragment.
 */
@AndroidEntryPoint
public class DigitalCashReceiveFragment extends Fragment {
  public static final String TAG = DigitalCashReceiveFragment.class.getSimpleName();

  @Inject Gson gson;

  private DigitalCashReceiveFragmentBinding binding;
  private LaoViewModel laoViewModel;
  private DigitalCashViewModel digitalCashViewModel;

  public DigitalCashReceiveFragment() {
    // Required empty constructor
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment DigitalCashReceiveFragment.
   */
  public static DigitalCashReceiveFragment newInstance() {
    return new DigitalCashReceiveFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    laoViewModel = LaoActivity.obtainViewModel(requireActivity());
    digitalCashViewModel =
        LaoActivity.obtainDigitalCashViewModel(requireActivity(), laoViewModel.getLaoId());
    binding = DigitalCashReceiveFragmentBinding.inflate(inflater, container, false);
    setHomeInterface();

    handleBackNav();
    return binding.getRoot();
  }

  public void setHomeInterface() {
    // Subscribe to roll calls so that our own address is kept updated in case a new rc is closed
    laoViewModel.addDisposable(
        digitalCashViewModel
            .getRollCallsObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                ids -> {
                  PoPToken token = digitalCashViewModel.getValidToken();
                  PublicKey publicKey = token.getPublicKey();
                  binding.digitalCashReceiveAddress.setText(publicKey.getEncoded());
                  PopTokenData tokenData = new PopTokenData(token.getPublicKey());
                  Bitmap myBitmap = QRCode.from(gson.toJson(tokenData)).withColor(getQRCodeColor(), Color.TRANSPARENT).bitmap();
                  binding.digitalCashReceiveQr.setImageBitmap(myBitmap);
                },
                error ->
                    ErrorUtils.logAndShow(
                        requireContext(), TAG, error, R.string.error_retrieve_own_token)));
  }

  @Override
  public void onResume() {
    super.onResume();
    laoViewModel.setPageTitle(R.string.digital_cash_receive);
    laoViewModel.setIsTab(false);
  }

  private void handleBackNav() {
    requireActivity()
        .getOnBackPressedDispatcher()
        .addCallback(
            getViewLifecycleOwner(),
            new OnBackPressedCallback(true) {
              @Override
              public void handleOnBackPressed() {
                Log.d(TAG, "Back pressed, going to digital cash home");
                LaoActivity.setCurrentFragment(
                    getParentFragmentManager(),
                    R.id.fragment_digital_cash_home,
                    DigitalCashHomeFragment::new);
              }
            });
  }
    // Returns color white if dark mode is active and black if light mode is active.
    private int getQRCodeColor() {
        Configuration configuration = getResources().getConfiguration();
        int nightModeFlags = configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if(nightModeFlags == Configuration.UI_MODE_NIGHT_YES){
            return Color.WHITE;
        }
        return Color.BLACK;
    }


}
