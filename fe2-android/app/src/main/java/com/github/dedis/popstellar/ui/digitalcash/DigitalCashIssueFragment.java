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
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
  public static final String TAG = DigitalCashIssueFragment.class.getSimpleName();
  private static int selectAllLaoMembers;
  private static int selectAllRollCallAttendees;
  private static int selectAllLaoWitnesses;
  private static final int NOTHING_SELECTED = -1;
  private static final int MIN_LAO_COIN = 0;

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
    selectAllLaoMembers =
        mBinding.digitalCashIssueSelect.indexOfChild(mBinding.digitalCashIssueSelect.getChildAt(0));
    selectAllRollCallAttendees =
        mBinding.digitalCashIssueSelect.indexOfChild(mBinding.digitalCashIssueSelect.getChildAt(1));
    selectAllLaoWitnesses =
        mBinding.digitalCashIssueSelect.indexOfChild(mBinding.digitalCashIssueSelect.getChildAt(2));
    return mBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupSendCoinButton();
    setUpGetPostTransactionEvent();
    setTheAdapterRollCallAttendee();
  }

  /** Function which call the view model post transaction when a post transaction event occur */
  public void setUpGetPostTransactionEvent() {
    mViewModel
        .getPostTransactionEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {

                /*Take the amount entered by the user*/
                String currentAmount = mBinding.digitalCashIssueAmount.getText().toString();
                String currentPublicKeySelected =
                    String.valueOf(mBinding.digitalCashIssueSpinner.getEditText().getText());

                int radioGroup = mBinding.digitalCashIssueSelect.getCheckedRadioButtonId();
                if ((currentAmount.isEmpty()) || (Integer.parseInt(currentAmount) < MIN_LAO_COIN)) {
                  // create in View Model a function that toast : please enter amount
                  mViewModel.requireToPutAnAmount();
                } else if (currentPublicKeySelected.isEmpty() && (radioGroup == NOTHING_SELECTED)) {
                  // create in View Model a function that toast : please enter key
                  mViewModel.requireToPutLAOMember();
                } else {

                  try {
                    if (radioGroup == NOTHING_SELECTED) {
                      postTransaction(
                          Collections.singletonMap(currentPublicKeySelected, currentAmount));
                    } else {
                      Set<PublicKey> attendees = new HashSet<>();
                      if (radioGroup == selectAllLaoMembers) {
                        for (RollCall current :
                            Objects.requireNonNull(mViewModel.getCurrentLao())
                                .getRollCalls()
                                .values()) {
                          attendees.addAll(current.getAttendees());
                        }
                      } else if (radioGroup == selectAllRollCallAttendees) {
                        attendees = mViewModel.getAttendeesFromTheRollCall();
                      } else if (radioGroup == selectAllLaoWitnesses) {
                        attendees =
                            Objects.requireNonNull(mViewModel.getCurrentLao()).getWitnesses();
                      }

                      if (attendees.isEmpty()) {
                        Toast.makeText(
                                requireContext(),
                                R.string.digital_cash_no_attendees,
                                Toast.LENGTH_LONG)
                            .show();
                        return;
                      } else {
                        Map<String, String> issueMap = new HashMap<>();
                        for (PublicKey publicKey : attendees) {
                          issueMap.putIfAbsent(publicKey.getEncoded(), currentAmount);
                        }
                        postTransaction(issueMap);
                      }
                    }
                  } catch (KeyException e) {
                    Log.d(TAG, getString(R.string.error_no_rollcall_closed_in_LAO));
                  }
                }
              }
            });
  }

  /** Function that setup the Button */
  private void setupSendCoinButton() {
    mBinding.digitalCashIssueIssue.setOnClickListener(v -> mViewModel.postTransactionEvent());
  }

  /** Function that set the Adapter */
  public void setTheAdapterRollCallAttendee() {
    /* Roll Call attendees to which we can send*/
    List<String> myArray = null;
    try {
      myArray = mViewModel.getAttendeesFromTheRollCallList();
    } catch (NoRollCallException e) {
      mViewModel.openHome();
      Log.d(TAG, "error : no RollCall in the Lao");
      Toast.makeText(requireContext(), "Please attend to the some RollCall", Toast.LENGTH_SHORT)
          .show();
    }
    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.list_item, myArray);
    mBinding.digitalCashIssueSpinnerTv.setAdapter(adapter);
  }

  /**
   * Function that post the transaction (call the function of the view model)
   *
   * @param publicKeyAmount Map<String, String> containing the Public Keys and the related amount to
   *     issue to
   * @throws KeyException throw this exception if the key of the issuer is not on the LAO
   */
  private void postTransaction(Map<String, String> publicKeyAmount) {
    if (mViewModel.getLaoId().getValue() == null) {
      Toast.makeText(
              requireContext().getApplicationContext(), R.string.error_no_lao, Toast.LENGTH_LONG)
          .show();
    } else {
      mViewModel.postTransaction(publicKeyAmount, Instant.now().getEpochSecond(), true);
      mViewModel.updateLaoCoinEvent();
    }
  }
}
