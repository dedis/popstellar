package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
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
                String currentAmount = mBinding.digitalCashSendAmount.getText().toString();
                String currentPublicKeySelected =
                    String.valueOf(mBinding.digitalCashSendSpinner.getEditText().getText());
                if ((currentAmount.isEmpty()) || (Integer.valueOf(currentAmount) < 0)) {
                  // create in View Model a function that toast : please enter amount
                  mViewModel.requireToPutAnAmount();
                  return;
                } else if (currentPublicKeySelected.isEmpty()) {
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
                  if (amount < (Integer.valueOf(currentAmount))) {
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
                postTransaction(Collections.singletonMap(currentPublicKeySelected, currentAmount));
                mViewModel.updateReceiptAddressEvent(currentPublicKeySelected);
                mViewModel.updateReceiptAmountEvent(currentAmount);
                mViewModel.openReceipt();
              }
            });

    setUpTheAdapter();
  }

  /** Funciton that set up the Adapter */
  private void setUpTheAdapter() {
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
   * @param publicKeyAmount Map<String, String> containing the Public Keys and the related amount to
   *     issue to
   * @throws KeyException throw this exception if the key of the issuer is not on the LAO
   */
  private void postTransaction(Map<String, String> publicKeyAmount) {
    // Add some check if have money
    if (mViewModel.getLaoId().getValue() == null) {
      Toast.makeText(
              requireContext().getApplicationContext(), R.string.error_no_lao, Toast.LENGTH_LONG)
          .show();
    } else {
      mViewModel.postTransaction(publicKeyAmount, Instant.now().getEpochSecond(), false);
      mViewModel.updateLaoCoinEvent();
    }
  }
}
