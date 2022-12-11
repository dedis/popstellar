package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashIssueFragmentBinding;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashIssueFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
@AndroidEntryPoint
public class DigitalCashIssueFragment extends Fragment {

  public static final String TAG = DigitalCashIssueFragment.class.getSimpleName();

  private DigitalCashIssueFragmentBinding binding;
  private DigitalCashViewModel viewModel;

  private int selectOneMember;
  private int selectAllLaoMembers;
  private int selectAllRollCallAttendees;
  private int selectAllLaoWitnesses;

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
    viewModel = DigitalCashActivity.obtainViewModel(getActivity());
    binding = DigitalCashIssueFragmentBinding.inflate(inflater, container, false);
    selectOneMember = binding.radioButton.getId();
    selectAllLaoMembers = binding.radioButton1.getId();
    selectAllRollCallAttendees = binding.radioButton2.getId();
    selectAllLaoWitnesses = binding.radioButton3.getId();
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupSendCoinButton();
    setUpGetPostTransactionEvent();
    setTheAdapterRollCallAttendee();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.digital_cash_issue);
  }

  /** Function which call the view model post transaction when a post transaction event occur */
  public void setUpGetPostTransactionEvent() {
    viewModel
        .getPostTransactionEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                /*Take the amount entered by the user*/
                String currentAmount = binding.digitalCashIssueAmount.getText().toString();
                String currentPublicKeySelected =
                    String.valueOf(binding.digitalCashIssueSpinner.getEditText().getText());
                int radioGroup = binding.digitalCashIssueSelect.getCheckedRadioButtonId();
                if (viewModel.canPerformTransaction(
                    currentAmount, currentPublicKeySelected, radioGroup)) {
                  try {
                    Map<String, String> issueMap =
                        computeMapForPostTransaction(
                            currentAmount, currentPublicKeySelected, radioGroup);
                    if (issueMap.isEmpty()) {
                      displayToast(radioGroup);
                    } else {
                      postTransaction(issueMap);
                    }
                  } catch (NoRollCallException r) {
                    Log.e(TAG, getString(R.string.no_rollcall_exception), r);
                  }
                }
              }
            });
  }

  private void displayToast(int radioGroup) {
    if (radioGroup == selectAllLaoWitnesses) {
      Toast.makeText(requireContext(), R.string.digital_cash_no_witness, Toast.LENGTH_LONG).show();
    } else {
      Toast.makeText(requireContext(), R.string.digital_cash_no_attendees, Toast.LENGTH_LONG)
          .show();
    }
  }

  public Map<String, String> computeMapForPostTransaction(
      String currentAmount, String currentPublicKeySelected, int radioGroup)
      throws NoRollCallException {
    if (radioGroup == DigitalCashViewModel.NOTHING_SELECTED) {
      // In unlikely event that no radiobutton are selected, it do as if the first one was selected
      return Collections.singletonMap(currentPublicKeySelected, currentAmount);
    } else {
      Set<PublicKey> attendees = attendeesPerRadioGroupButton(radioGroup, currentPublicKeySelected);
      Map<String, String> issueMap = new HashMap<>();
      if (!attendees.isEmpty()) {
        for (PublicKey publicKey : attendees) {
          issueMap.putIfAbsent(publicKey.getEncoded(), currentAmount);
        }
      }
      return issueMap;
    }
  }

  /**
   * Function that return the give list of attendees Radio Group Button selected (a empty list if
   * nothing)
   */
  private Set<PublicKey> attendeesPerRadioGroupButton(int radioGroup, String currentSelected)
      throws NoRollCallException {
    Set<PublicKey> attendees = new HashSet<>();
    if (radioGroup == selectOneMember && !currentSelected.equals("")) {
      attendees.add(new PublicKey(currentSelected));
    } else if (radioGroup == selectAllLaoMembers) {
      for (RollCall current :
          Objects.requireNonNull(viewModel.getCurrentLaoValue()).getRollCalls().values()) {
        attendees.addAll(current.getAttendees());
      }
    } else if (radioGroup == selectAllRollCallAttendees) {
      attendees = viewModel.getAttendeesFromLastRollCall();
    } else if (radioGroup == selectAllLaoWitnesses) {
      attendees = Objects.requireNonNull(viewModel.getCurrentLaoValue()).getWitnesses();
    }
    return attendees;
  }

  /** Function that setup the Button */
  private void setupSendCoinButton() {
    binding.digitalCashIssueIssue.setOnClickListener(v -> viewModel.postTransactionEvent());
  }

  /** Function that set the Adapter */
  public void setTheAdapterRollCallAttendee() {
    /* Roll Call attendees to which we can send*/
    List<String> myArray;
    try {
      myArray = viewModel.getAttendeesFromTheRollCallList();
    } catch (NoRollCallException e) {
      viewModel.setCurrentTab(DigitalCashTab.HOME);
      Log.d(TAG, getString(R.string.error_no_rollcall_closed_in_LAO));
      Toast.makeText(
              requireContext(),
              getString(R.string.digital_cash_please_enter_roll_call),
              Toast.LENGTH_SHORT)
          .show();
      myArray = new ArrayList<>();
    }
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(requireContext(), R.layout.list_item, myArray);
    binding.digitalCashIssueSpinnerTv.setAdapter(adapter);
  }

  /**
   * Function that post the transaction (call the function of the view model)
   *
   * @param publicKeyAmount Map<String, String> containing the Public Keys and the related amount to
   *     issue to
   */
  private void postTransaction(Map<String, String> publicKeyAmount) {
    if (viewModel.getLaoId().getValue() == null) {
      Toast.makeText(
              requireContext().getApplicationContext(), R.string.error_no_lao, Toast.LENGTH_LONG)
          .show();
    } else {
      viewModel.addDisposable(
          viewModel
              .postTransaction(publicKeyAmount, Instant.now().getEpochSecond(), true)
              .subscribe(
                  () ->
                      Toast.makeText(
                              requireContext(),
                              R.string.digital_cash_post_transaction,
                              Toast.LENGTH_LONG)
                          .show(),
                  error -> {
                    if (error instanceof KeyException
                        || error instanceof GeneralSecurityException) {
                      ErrorUtils.logAndShow(
                          requireContext(), TAG, error, R.string.error_retrieve_own_token);
                    } else {
                      ErrorUtils.logAndShow(
                          requireContext(), TAG, error, R.string.error_post_transaction);
                    }
                  }));
      viewModel.updateLaoCoinEvent();
    }
  }
}
