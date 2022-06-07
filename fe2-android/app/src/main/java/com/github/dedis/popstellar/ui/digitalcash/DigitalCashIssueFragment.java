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
import com.github.dedis.popstellar.databinding.DigitalCashIssueFragmentBinding;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

                int radio_group = mBinding.digitalCashIssueSelect.getCheckedRadioButtonId();
                Log.d(TAG, "radio group : " + radio_group);

                if ((current_amount.isEmpty()) || (Integer.valueOf(current_amount) < 0)) {
                  // create in View Model a function that toast : please enter amount
                  mViewModel.requireToPutAnAmount();
                } else if (current_public_key_selected.isEmpty() && (radio_group == -1)) {
                  // create in View Model a function that toast : please enter key
                  mViewModel.requireToPutLAOMember();
                } else {

                  try {
                    if (radio_group == -1) {
                      postTransaction(
                          Collections.singletonMap(current_public_key_selected, current_amount));
                    } else {
                      Set<PublicKey> attendees = new HashSet<>();
                      if (radio_group == 2131231314) {
                        Iterator<RollCall> rollCallIterator =
                            mViewModel.getCurrentLao().getRollCalls().values().iterator();
                        while (rollCallIterator.hasNext()) {
                          RollCall current = rollCallIterator.next();
                          attendees.addAll(current.getAttendees());
                        }
                      } else if (radio_group == 2131231315) {
                        attendees = mViewModel.getAttendeesFromTheRollCall();
                      } else {
                        attendees = mViewModel.getCurrentLao().getWitnesses();
                        if (attendees.isEmpty()) {
                          Toast.makeText(
                              requireContext(),
                              "Can't issue there are no witnesses",
                              Toast.LENGTH_LONG);
                          return;
                        }
                      }
                      Map<String, String> issueMap = new HashMap<>();
                      Iterator<PublicKey> publicKeyIterator = attendees.iterator();
                      while (publicKeyIterator.hasNext()) {
                        PublicKey publicKey = publicKeyIterator.next();
                        issueMap.putIfAbsent(publicKey.getEncoded(), current_amount);
                      }
                      postTransaction(issueMap);
                    }
                  } catch (KeyException e) {
                    Log.d(TAG, getString(R.string.error_no_rollcall_closed_in_LAO));
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
      mViewModel.postTransaction(PublicKeyAmount, Instant.now().getEpochSecond(), true);
      mViewModel.updateLaoCoinEvent();
    }
  }
}
