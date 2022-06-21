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
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.security.KeyManager;

import java.time.Instant;
import java.util.ArrayList;
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
    mViewModel = DigitalCashActivity.obtainViewModel(getActivity());
    mBinding = DigitalCashSendFragmentBinding.inflate(inflater, container, false);

    // Inflate the layout for this fragment
    return mBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupSendCoinButton();
    mBinding.digitalCashSendAmount.setText("0");

    mViewModel
        .getPostTransactionEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                System.out.println("ping 1");
                String currentAmount = mBinding.digitalCashSendAmount.getText().toString();
                String currentPublicKeySelected =
                    String.valueOf(mBinding.digitalCashSendSpinner.getEditText().getText());
                System.out.println("ping 2");
                if (mViewModel.canPerformTransaction(currentAmount, currentPublicKeySelected, -1)) {
                  try {
                    System.out.println("ping 3");
                    Lao lao = mViewModel.getCurrentLao();
                    System.out.println("ping 4");
                    PoPToken token = mViewModel.getKeyManager().getValidPoPToken(lao);
                    System.out.println("ping 5");
                    if (canPostTransaction(
                        lao, token.getPublicKey(), Integer.parseInt(currentAmount))) {
                      System.out.println("ping 6");
                      postTransaction(
                          Collections.singletonMap(currentPublicKeySelected, currentAmount));
                      System.out.println("ping 7");
                      mViewModel.updateReceiptAddressEvent(currentPublicKeySelected);
                      System.out.println("ping 8");
                      mViewModel.updateReceiptAmountEvent(currentAmount);
                      System.out.println("ping 9");
                      mViewModel.openReceipt();
                    }
                    System.out.println("ping 10");

                  } catch (KeyException keyException) {
                    Toast.makeText(
                            requireContext(),
                            R.string.digital_cash_please_enter_a_lao,
                            Toast.LENGTH_SHORT)
                        .show();
                  }
                }
              }
            });

    try {
      setUpTheAdapter();
    } catch (KeyException e) {
      Toast.makeText(
                      requireContext(),
                      R.string.digital_cash_error_poptoken,
                      Toast.LENGTH_SHORT)
              .show();
    }
  }

  public boolean canPostTransaction(Lao lao, PublicKey publicKey, int currentAmount) {
    Map<PublicKey, List<TransactionObject>> transactionByUser = lao.getTransactionByUser();
    System.out.println("plopo 1");
    for (PublicKey pk: transactionByUser.keySet()){
      System.out.println(pk.getEncoded());
      System.out.println(publicKey.getEncoded());
    }
    if (transactionByUser.isEmpty() || !transactionByUser.containsKey(publicKey)) {
      System.out.println("plopo 2");
      Toast.makeText(requireContext(), R.string.digital_cash_warning_no_money, Toast.LENGTH_SHORT)
          .show();
      return false;
    }
    System.out.println("plopo 3");
    long amount =
        TransactionObject.getMiniLaoPerReceiverSetTransaction(
            transactionByUser.get(publicKey), publicKey);
    System.out.println("plopo 4");
    if (amount < currentAmount) {
      System.out.println("plopo 5");
      Toast.makeText(
              requireContext(), R.string.digital_cash_warning_not_enough_money, Toast.LENGTH_SHORT)
          .show();
      return false;
    } else {
      System.out.println("plopo 6");
      return true;
    }
  }

  /** Funciton that set up the Adapter */
  private void setUpTheAdapter() throws KeyException {
    /* Roll Call attendees to which we can send*/
    List<String> myArray;
    try {
      myArray = mViewModel.getAttendeesFromTheRollCallList();
    } catch (NoRollCallException e) {
      mViewModel.openHome();
      Toast.makeText(
              requireContext(), R.string.digital_cash_please_enter_roll_call, Toast.LENGTH_SHORT)
          .show();
      myArray = new ArrayList<>();
    }
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(requireContext(), R.layout.list_item, myArray);
    KeyManager km = mViewModel.getKeyManager();
    mBinding.digitalCashSendSpinner.getEditText().setText(km.getPoPToken(mViewModel.getCurrentLao(), mViewModel.getCurrentLao().lastRollCallClosed()).getPublicKey().getEncoded());
    mBinding.digitalCashSendSpinnerTv.setAdapter(adapter);

    //mBinding.digitalCashSendSpinnerTv.setText(myArray.get(0), false);
  }

  /** Function that setup the Button */
  private void setupSendCoinButton() {
    System.out.println("SendCoin Setup");
    mBinding.digitalCashSendSend.setOnClickListener(v -> {
      System.out.println("Called send");
      mViewModel.postTransactionEvent();
    });
    System.out.println("SendCoin Ended");
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
