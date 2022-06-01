package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashIssueFragmentBinding;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashIssueFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
@AndroidEntryPoint
public class DigitalCashIssueFragment extends Fragment {
  private DigitalCashIssueFragmentBinding mBinding;
  private DigitalCashViewModel mViewModel;

  public DigitalCashIssueFragment() {
    // not implemented yet
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment DigitalCashIssueFragment.
   */
  public static DigitalCashIssueFragment newInstance() {
    return new DigitalCashIssueFragment();
  }


  @Override
  public View onCreateView(
          @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    this.mViewModel = DigitalCashMain.obtainViewModel(getActivity());
    mBinding = DigitalCashIssueFragmentBinding.inflate(inflater, container, false);
    return mBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupSendCoinButton();

    mViewModel
        .getpostTransactionEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                // String current_amount = mBinding.digitalCashIssueAmount.getText().toString();
                // Log.d(this.getClass().toString(), "the current amount is " + current_amount);
                String current_public_key_selected =
                    String.valueOf(mBinding.digitalCashIssueSpinner.getPlaceholderText());
                Log.d(
                    this.getClass().toString(),
                    "place holder text is " + current_public_key_selected);
                try {
                  postTransaction(Collections.singletonMap(current_public_key_selected, "10"));
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
    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.list_item, myArray);
    mBinding.digitalCashIssueSpinnerTv.setAdapter(adapter);
  }

  private void setupSendCoinButton(){
    mBinding.digitalCashIssueIssue.setOnClickListener(
            v -> mViewModel.postTransactionEvent()
    );
  }

  /// ** Function that permits to post transaction */
  private void postTransaction(Map<String, String> PublicKeyAmount) throws KeyException {
    // if (mBinding.digitalCashIssueAmount.getText() == null) {
    // Toast.makeText(this.requireContext(), "Please enter an amount", Toast.LENGTH_SHORT).show();
    // } else {
    // String amount_string = mBinding.digitalCashIssueAmount.getText().toString();
    Log.d(this.getClass().toString(), "Try to send a transaction");
    Log.d(this.getClass().toString(), PublicKeyAmount.entrySet().toString());

    // long amount = 0 ;
    mViewModel.postTransactionTest(PublicKeyAmount, Instant.now().getEpochSecond());
      //mViewModel.postTransaction(Collections.singletonMap(mViewModel.getCurrentLao().getOrganizer(),amount),
          //Instant.now().getEpochSecond());
   // }
  }
}
