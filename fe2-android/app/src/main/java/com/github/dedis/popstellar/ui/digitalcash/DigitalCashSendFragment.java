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
import com.github.dedis.popstellar.model.objects.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
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
  private String TAG = DigitalCashSendFragment.class.toString();

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
                if ((current_amount.isEmpty()) || (Integer.valueOf(current_amount) < 0)) {
                  // create in View Model a function that toast : please enter amount
                  mViewModel.requireToPutAnAmount();
                  return;
                } else if (current_public_key_selected.isEmpty()) {
                  // create in View Model a function that toast : please enter key
                  mViewModel.requireToPutLAOMember();
                  return;
                }

                PoPToken token = null;
                try {
                  token = mViewModel.getKeyManager().getValidPoPToken(mViewModel.getCurrentLao());
                  if (mViewModel.getCurrentLao().getTransactionByUser().isEmpty()
                      || !mViewModel
                          .getCurrentLao()
                          .getTransactionByUser()
                          .containsKey(token.getPublicKey())) {
                    Toast.makeText(
                            requireContext(),
                            "Can't send because you have no money",
                            Toast.LENGTH_SHORT)
                        .show();
                    return;
                  }

                  long amount =
                      TransactionObject.getMiniLaoPerReceiverSetTransaction(
                          mViewModel
                              .getCurrentLao()
                              .getTransactionByUser()
                              .get(token.getPublicKey()),
                          token.getPublicKey());
                  if (amount < (Integer.valueOf(current_amount))) {
                    Toast.makeText(
                            requireContext(),
                            "Can't send more money than you have !",
                            Toast.LENGTH_SHORT)
                        .show();
                    return;
                  }
                } catch (KeyException keyException) {
                  Toast.makeText(requireContext(), "Require to be in the LAO !", Toast.LENGTH_SHORT)
                      .show();
                  return;
                }

                try {
                  postTransaction(
                      Collections.singletonMap(current_public_key_selected, current_amount));
                  mViewModel.updateReceiptAddressEvent(current_public_key_selected);
                  mViewModel.updateReceiptAmountEvent(current_amount);
                  mViewModel.openReceipt();
                } catch (KeyException e) {
                  e.printStackTrace();
                  Log.d(TAG, "error couldn't post the transaction due to key exception");
                }
              }
            });

    /* Roll Call attendees to which we can send*/
    List<String> myArray = null;
    try {
      myArray = mViewModel.getAttendeesFromTheRollCallList();
    } catch (NoRollCallException e) {
      mViewModel.openHome();
      Toast.makeText(
              requireContext(), R.string.digital_cash_please_enter_roll_call, Toast.LENGTH_SHORT)
          .show();
    }
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(requireContext(), R.layout.list_item, myArray);
    mBinding.digitalCashSendSpinnerTv.setAdapter(adapter);
  }

  /** Function that setup the Button */
  private void setupSendCoinButton() {
    mBinding.digitalCashSendSend.setOnClickListener(v -> mViewModel.postTransactionEvent());
  }

  /**
   * Function that post the transaction (call the function of the view model)
   *
   * @param PublicKeyAmount Map<String, String> containing the Public Keys and the related amount to
   *     issue to
   * @throws KeyException throw this exception if the key of the issuer is not on the LAO
   */
  private void postTransaction(Map<String, String> PublicKeyAmount) throws KeyException {
    // Add some check if have money
    if (mViewModel.getLaoId().getValue() == null) {
      Toast.makeText(
              requireContext().getApplicationContext(), R.string.error_no_lao, Toast.LENGTH_LONG)
          .show();
    } else {
      mViewModel.postTransaction(PublicKeyAmount, Instant.now().getEpochSecond(), false);
      mViewModel.updateLaoCoinEvent();
    }
  }
}
