package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.DigitalCashHomeFragmentBinding;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.utility.error.keys.KeyException;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashHomeFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class DigitalCashHomeFragment extends Fragment {
  private DigitalCashHomeFragmentBinding mBinding;
  private DigitalCashViewModel mViewModel;

  public DigitalCashHomeFragment() {
    // not implemented yet
  }
  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment DigitalCashHomeFragment.
   */
  public static DigitalCashHomeFragment newInstance() {
    return new DigitalCashHomeFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    this.mViewModel = DigitalCashMain.obtainViewModel(getActivity());
    mBinding = DigitalCashHomeFragmentBinding.inflate(inflater, container, false);
    return mBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mViewModel
        .getUpdateLaoCoinEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                try {
                  Lao lao = mViewModel.getCurrentLao();
                  PoPToken token = mViewModel.getKeyManager().getValidPoPToken(lao);

                  if (lao.getTransactionByUser().containsKey(token.getPublicKey())) {
                    TransactionObject transaction =
                        lao.getTransactionByUser().get(token.getPublicKey());
                    mBinding.digitalCashSendAddress.setText(
                        "LAOcoin : " + transaction.getMiniLaoPerReceiver(token.getPublicKey()));
                  }

                } catch (KeyException e) {
                  e.printStackTrace();
                  Log.d(this.getClass().toString(), "Error to get the Key");
                }
              }
            });

    try {
      PoPToken token = mViewModel.getKeyManager().getValidPoPToken(mViewModel.getCurrentLao());
      mBinding.digitalCashHomeAddress.setText(token.getPublicKey().getEncoded());
    } catch (KeyException e) {
      e.printStackTrace();
      Log.d(this.getClass().toString(), "No key in lao");
    }
  }
}
