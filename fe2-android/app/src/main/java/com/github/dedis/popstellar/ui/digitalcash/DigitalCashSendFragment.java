package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashSendFragmentBinding;

import java.time.Instant;
import java.util.Collections;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashSendFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class DigitalCashSendFragment extends Fragment {
  private DigitalCashSendFragmentBinding digitalCashSendFragmentBinding;
  private DigitalCashViewModel digitalCashViewModel;

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
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    this.digitalCashSendFragmentBinding =
        DigitalCashSendFragmentBinding.inflate(inflater, container, false);
    this.digitalCashViewModel = DigitalCashMain.obtainViewModel(requireActivity());
    digitalCashSendFragmentBinding.setViewModel(digitalCashViewModel);
    digitalCashSendFragmentBinding.setLifecycleOwner(getViewLifecycleOwner());

    return digitalCashSendFragmentBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupSendCoinButton();
    // subscribe to "send coin" event
    digitalCashViewModel
        .getPostTransactionEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                postTransaction();
              }
            });
  }

  private void setupSendCoinButton() {
    digitalCashSendFragmentBinding.sendButtonCoin.setOnClickListener(
        v -> digitalCashViewModel.setPostTransactionEvent());
  }

  /** Function that permits to post transaction */
  private void postTransaction() {
    if (digitalCashViewModel.getLaoId().getValue() == null) {
      Toast.makeText(
              requireContext().getApplicationContext(), R.string.error_no_lao, Toast.LENGTH_LONG)
          .show();
    } else {
      String receiver = digitalCashSendFragmentBinding.pkToSend.getText().toString();

      Integer amount =
          Integer.valueOf(digitalCashSendFragmentBinding.amountCoin.getText().toString());
      digitalCashViewModel.postTransaction(
          receiver, Collections.singletonList(amount), Instant.now().getEpochSecond());
      digitalCashViewModel.openReceipt();
    }
  }

  // open Scanning
  // setup Camera Permission Fragment
  // setupSendCoinFragment

}
