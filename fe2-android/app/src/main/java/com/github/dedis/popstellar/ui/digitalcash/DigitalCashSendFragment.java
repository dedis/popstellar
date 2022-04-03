package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashSendFragmentBinding;
import com.github.dedis.popstellar.databinding.SocialMediaSendFragmentBinding;
import com.github.dedis.popstellar.model.objects.Address;
import com.github.dedis.popstellar.ui.socialmedia.SocialMediaActivity;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment where you can send a coin */
@AndroidEntryPoint
public class DigitalCashSendFragment extends Fragment {
  private DigitalCashSendFragmentBinding mDigitalCashSendFragBinding;
  private DigitalCashViewModel mDigitalCashViewModel;

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment DigitalCashSendFragment.
   */
  public static DigitalCashSendFragment newInstance() {
    DigitalCashSendFragment fragment = new DigitalCashSendFragment();
    return fragment;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    mDigitalCashSendFragBinding =
        DigitalCashSendFragmentBinding.inflate(inflater, container, false);

    mDigitalCashViewModel = DigitalCashMain.obtainViewModel(requireActivity());

    mDigitalCashSendFragBinding.setViewModel(mDigitalCashViewModel);
    mDigitalCashSendFragBinding.setLifecycleOwner(getViewLifecycleOwner());

    return mDigitalCashSendFragBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupSendCoinButton();
    mDigitalCashViewModel
            .getSendNewDummyTransactionEvent()
            .observe(
                    getViewLifecycleOwner(),
                    booleanEvent -> {
                      Boolean event = booleanEvent.getContentIfNotHandled();
                      if (event != null) {
                        sendNewDummyCoin();
                      }
                    });
  }

  private void setupSendCoinButton() {
    mDigitalCashSendFragBinding.buttonSend.setOnClickListener(
        v -> mDigitalCashViewModel.sendNewDummyTransactionEvent());
  }

  private void sendNewDummyCoin() {
    //TODO: should be some check LAO
    // send some coin
    // make a toast appear
    // if (mDigitalCashViewModel.getLaoId().getValue() == null) {
    // Toast.makeText(
    //        requireContext().getApplicationContext(), R.string.error_no_lao, Toast.LENGTH_LONG)
    //       .show();
    // } else {
    mDigitalCashViewModel.sendNewDummyCoin(
        Integer.parseInt(mDigitalCashSendFragBinding.edAmount.getText().toString()),
        new Address(mDigitalCashSendFragBinding.edSenderAddress.getText().toString()),
        new Address(mDigitalCashSendFragBinding.edReceiverAddress.getText().toString()),this.requireContext());
    // Change to Open Receipt
    mDigitalCashViewModel.openReceipt();
    // }
  }
}
