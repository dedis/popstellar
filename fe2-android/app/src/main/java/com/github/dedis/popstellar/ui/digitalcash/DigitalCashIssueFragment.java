package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.telephony.mbms.StreamingServiceInfo;
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
  private String TAG = DigitalCashIssueFragment.class.toString();

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
        .getPostTransactionEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {

                /*Take the amount entered by the user*/
                String current_amount = mBinding.digitalCashIssueAmount.getText().toString();
                String current_public_key_selected =
                    String.valueOf(mBinding.digitalCashIssueSpinner.getEditText().getText());

                if ((current_amount.isEmpty()) || (Integer.valueOf(current_amount) < 0)) {
                  // create in View Model a function that toast : please enter amount
                  mViewModel.requireToPutAnAmount();
                } else if (current_public_key_selected.isEmpty()) {
                  // create in View Model a function that toast : please enter key
                  mViewModel.requireToPutLAOMember();
                } else {
                  try {
                    postTransaction(
                        Collections.singletonMap(current_public_key_selected, current_amount));
                  } catch (KeyException e) {
                    e.printStackTrace();
                    Log.d(TAG, "error couldn't post the transaction due to key exception");
                  }
                }
              }
            });

    /* Roll Call attendees to which we can send*/
    List<String> myArray = null;
    try {
      myArray = mViewModel.getAttendeesFromTheRollCallList();
    } catch (NoRollCallException e) {
      mViewModel.openHome();
      Log.d(this.getClass().toString(), "error : no RollCall in the Lao");
      Toast.makeText(requireContext(), "Please attend to the some RollCall", Toast.LENGTH_SHORT).show();
    }
    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.list_item, myArray);
    mBinding.digitalCashIssueSpinnerTv.setAdapter(adapter);
  }

  /** Function that setup the Button */
  private void setupSendCoinButton() {
    mBinding.digitalCashIssueIssue.setOnClickListener(
            v -> mViewModel.postTransactionEvent()
    );
  }

  /**
   * Function that post the transaction (call the function of the view model)
   *
   * @param PublicKeyAmount Map<String, String> containing the Public Keys and the related amount to
   *     issue to
   * @throws KeyException throw this exception if the key of the issuer is not on the LAO
   */
  private void postTransaction(Map<String, String> PublicKeyAmount) throws KeyException {
    if (mViewModel.getLaoId().getValue() == null) {
      Toast.makeText(
              requireContext().getApplicationContext(), R.string.error_no_lao, Toast.LENGTH_LONG)
              .show();
    } else {
      mViewModel.postTransaction(PublicKeyAmount, Instant.now().getEpochSecond());
      mViewModel.updateLaoCoinEvent();
    }
  }
}
