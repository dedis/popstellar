package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.DigitalCashReceiveFragmentBinding;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.utility.error.keys.KeyException;

import java.time.Instant;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashReceiveFragment#newInstance}
 * factory method to create an instance of this fragment.
 */
public class DigitalCashReceiveFragment extends Fragment {
  private DigitalCashReceiveFragmentBinding mBinding;
  private DigitalCashViewModel mViewModel;

  public DigitalCashReceiveFragment() {
    // not implemented yet
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
    this.mViewModel = DigitalCashMain.obtainViewModel(getActivity());
    mBinding = DigitalCashReceiveFragmentBinding.inflate(inflater, container, false);
    return mBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    try {
      Lao lao = mViewModel.getCurrentLao();
      PoPToken token = mViewModel.getKeyManager().getValidPoPToken(lao);

      if (lao.getTransactionByUser().containsKey(token.getPublicKey())) {
        TransactionObject transaction = lao.getTransactionByUser().get(token.getPublicKey());
        String sender = transaction.getSendersTransaction().get(0).getEncoded();

        mBinding.digitalCashReceiveAddress.setText("Received from : \n" + sender);

        long timeAgo = Instant.now().getEpochSecond() - transaction.getLockTime();
        mBinding.digitalCashReceiveTime.setText(timeAgo + " seconds ago ");
      }
    } catch (KeyException e) {
      e.printStackTrace();
      Log.d(this.getClass().toString(), "Error to get the Key");
    }

    /*mViewModel
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
                String sender = transaction.getSendersTransaction().get(0).getEncoded();

                mBinding.digitalCashReceiveAddress.setText(" Received from : \n" + sender);
                mBinding.digitalCashReceiveAmount.setText(
                    "Total from : \n"
                        + transaction.getMiniLaoPerReceiver(token.getPublicKey())
                        + " LAOcoin");
              }

            } catch (KeyException e) {
              e.printStackTrace();
              Log.d(this.getClass().toString(), "Error to get the Key");
            }
          }
        });*/
  }
}
