package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.DigitalCashReceiptFragmentBinding;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashReceiptFragment} factory method to
 * create an instance of this fragment.
 */
public class DigitalCashReceiptFragment extends Fragment {
  private DigitalCashReceiptFragmentBinding mBinding;
  private DigitalCashViewModel mViewModel;

  public DigitalCashReceiptFragment() {
    // not implemented yet
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment DigitalCashReceiveFragment.
   */
  public static DigitalCashReceiptFragment newInstance() {
    return new DigitalCashReceiptFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      System.out.println("ploup -4");
    this.mViewModel = DigitalCashActivity.obtainViewModel(getActivity());
      System.out.println("ploup -3");
    mBinding = DigitalCashReceiptFragmentBinding.inflate(inflater, container, false);
      System.out.println("ploup -2");
    return mBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
      System.out.println("ploup -1");
    mViewModel
        .getUpdateReceiptAmountEvent()
        .observe(
            getViewLifecycleOwner(),
            stringEvent -> {
                System.out.println("ploup");
              String amount = stringEvent.getContentIfNotHandled();
                System.out.println("ploup 2");
              if (amount != null) {
                  System.out.println("ploup 3");
                mBinding.digitalCashReceiptAmount.setText(amount);
                  System.out.println("ploup 4");
              }
            });
    mViewModel
        .getUpdateReceiptAddressEvent()
        .observe(
            getViewLifecycleOwner(),
            stringEvent -> {
                System.out.println("ploup 5");
              String address = stringEvent.getContentIfNotHandled();
                System.out.println("ploup 6");
              if (address != null) {
                  System.out.println("ploup 7");
                mBinding.digitalCashReceiptBeneficiary.setText(
                    String.format("Beneficary : %n %s", address));
              }
            });
  }
}
