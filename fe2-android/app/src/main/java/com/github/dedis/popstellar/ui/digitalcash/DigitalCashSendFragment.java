package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashSendFragmentBinding;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashSendFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class DigitalCashSendFragment extends Fragment {
    private DigitalCashSendFragmentBinding mBinding;
    private DigitalCashViewModel mViewModel;

  public DigitalCashSendFragment() {
    // not implemented yet
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment DigitalCashSendFragment.
   */
  public static DigitalCashSendFragment newInstance() {
    return new DigitalCashSendFragment();
  }


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mViewModel = DigitalCashMain.obtainViewModel(getActivity());
        mBinding = DigitalCashSendFragmentBinding.inflate(inflater, container, false);

        // Inflate the layout for this fragment
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    setupSendCoinButton();

    mViewModel
        .getPostTransactionEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                String current_amount = mBinding.digitalCashSendAmount.getText().toString();
                // Log.d(this.getClass().toString(), "the current amount is " + current_amount);

                String current_public_key_selected =
                    String.valueOf(mBinding.digitalCashSendSpinner.getEditText().getText());
                Log.d(
                    this.getClass().toString(),
                    "place holder text is " + current_public_key_selected);
                try {
                  postTransaction(
                      Collections.singletonMap(current_public_key_selected, current_amount));

                  mViewModel.updateLaoCoinEvent();
                  mViewModel.updateReceiptAddressEvent(current_public_key_selected);
                  mViewModel.updateReceiptAmountEvent(current_amount);

                  mViewModel.openReceipt();
                } catch (KeyException e) {
                  e.printStackTrace();
                }
              }
            });

    List<String> myArray = null;
    try {
      myArray = mViewModel.getAttendeesFromTheRollCallList();
    } catch (NoRollCallException e) {
      e.printStackTrace();
      Log.d(this.getClass().toString(), "Error : No Roll Call in the Lao");
    }
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(requireContext(), R.layout.list_item, myArray);
    mBinding.digitalCashSendSpinnerTv.setAdapter(adapter);
    }

  private void setupSendCoinButton() {
    mBinding.digitalCashSendSend.setOnClickListener(v -> mViewModel.postTransactionEvent());
  }

  /// ** Function that permits to post transaction */
  private void postTransaction(Map<String, String> PublicKeyAmount) throws KeyException {
    // Add some check if have money
    if (mViewModel.getLaoId().getValue() == null) {
      Toast.makeText(
              requireContext().getApplicationContext(), R.string.error_no_lao, Toast.LENGTH_LONG)
          .show();
    } else {
      Log.d(this.getClass().toString(), "Try to send a transaction");
      Log.d(this.getClass().toString(), "The values are :" + PublicKeyAmount.values().toString());
      Log.d(this.getClass().toString(), "The keys are : " + PublicKeyAmount.keySet().toString());
      mViewModel.postTransaction(PublicKeyAmount, Instant.now().getEpochSecond());
    }
  }
}
