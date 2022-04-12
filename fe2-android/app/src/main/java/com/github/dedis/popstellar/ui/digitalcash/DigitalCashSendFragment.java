package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.DigitalCashSendFragmentBinding;

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

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    // TODO here add the send Coin event
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

  private void sendTransaction() {
    // Change to Open Receipt
    mDigitalCashViewModel.openReceipt();
  }
}
