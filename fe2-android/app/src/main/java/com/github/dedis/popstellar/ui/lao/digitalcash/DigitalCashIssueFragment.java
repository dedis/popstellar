package com.github.dedis.popstellar.ui.lao.digitalcash;

import android.os.Bundle;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashIssueFragmentBinding;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

  private static final Logger logger = LogManager.getLogger(DigitalCashIssueFragment.class);

  private DigitalCashIssueFragmentBinding binding;
  private LaoViewModel laoViewModel;
  private DigitalCashViewModel digitalCashViewModel;

  private int selectOneMember;
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
    laoViewModel = LaoActivity.obtainViewModel(requireActivity());
    digitalCashViewModel =
        LaoActivity.obtainDigitalCashViewModel(requireActivity(), laoViewModel.getLaoId());
    binding = DigitalCashIssueFragmentBinding.inflate(inflater, container, false);

    selectOneMember = binding.radioButton.getId();
    selectAllRollCallAttendees = binding.radioButtonAttendees.getId();
    selectAllLaoWitnesses = binding.radioButtonWitnesses.getId();

    handleBackNav();
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupSendCoinButton();
    setTheAdapterRollCallAttendee();
  }

  @Override
  public void onResume() {
    super.onResume();
    laoViewModel.setPageTitle(R.string.digital_cash_issue);
    laoViewModel.setIsTab(false);
  }

  /** Function which call the view model post transaction when a post transaction event occur */
  public void issueCoins() {
    /*Take the amount entered by the user*/
    String currentAmount = binding.digitalCashIssueAmount.getText().toString();
    String currentPublicKeySelected =
        String.valueOf(binding.digitalCashIssueSpinner.getEditText().getText());
    int radioGroup = binding.digitalCashIssueSelect.getCheckedRadioButtonId();
    if (digitalCashViewModel.canPerformTransaction(
        currentAmount, currentPublicKeySelected, radioGroup)) {
      try {
        Map<String, String> issueMap =
            computeMapForPostTransaction(currentAmount, currentPublicKeySelected, radioGroup);
        if (issueMap.isEmpty()) {
          displayToast(radioGroup);
        } else {
          postTransaction(issueMap);
        }
      } catch (NoRollCallException r) {
        ErrorUtils.logAndShow(requireContext(), logger, r, R.string.no_rollcall_exception);
      } catch (UnknownLaoException e) {
        ErrorUtils.logAndShow(requireContext(), logger, e, R.string.unknown_lao_exception);
      }
    }
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
      throws NoRollCallException, UnknownLaoException {
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
      throws NoRollCallException, UnknownLaoException {
    Set<PublicKey> attendees = new HashSet<>();
    if (radioGroup == selectOneMember && !currentSelected.equals("")) {
      attendees.add(new PublicKey(currentSelected));
    } else if (radioGroup == selectAllRollCallAttendees) {
      attendees = digitalCashViewModel.getAttendeesFromLastRollCall();
    } else if (radioGroup == selectAllLaoWitnesses) {
      attendees = Objects.requireNonNull(digitalCashViewModel.getLao()).getWitnesses();
    }
    return attendees;
  }

  /** Function that setup the Button */
  private void setupSendCoinButton() {
    binding.digitalCashIssueIssue.setOnClickListener(v -> issueCoins());
  }

  /** Function that set the Adapter */
  public void setTheAdapterRollCallAttendee() {
    /* Roll Call attendees to which we can send*/
    List<String> myArray;
    try {
      myArray = digitalCashViewModel.getAttendeesFromTheRollCallList();
    } catch (NoRollCallException e) {
      logger.debug(getString(R.string.error_no_rollcall_closed_in_LAO));
      Toast.makeText(
              requireContext(),
              getString(R.string.digital_cash_please_enter_roll_call),
              Toast.LENGTH_SHORT)
          .show();
      myArray = new ArrayList<>();
      LaoActivity.setCurrentFragment(
          getParentFragmentManager(),
          R.id.fragment_digital_cash_home,
          DigitalCashHomeFragment::newInstance);
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
    laoViewModel.addDisposable(
        digitalCashViewModel
            .postTransaction(publicKeyAmount, Instant.now().getEpochSecond(), true)
            .subscribe(
                () -> {
                  Toast.makeText(
                          requireContext(),
                          R.string.digital_cash_post_transaction,
                          Toast.LENGTH_LONG)
                      .show();
                  LaoActivity.setCurrentFragment(
                      getParentFragmentManager(),
                      R.id.fragment_digital_cash_home,
                      DigitalCashHomeFragment::newInstance);
                },
                error -> {
                  if (error instanceof KeyException || error instanceof GeneralSecurityException) {
                    ErrorUtils.logAndShow(
                        requireContext(), logger, error, R.string.error_retrieve_own_token);
                  } else {
                    ErrorUtils.logAndShow(
                        requireContext(), logger, error, R.string.error_post_transaction);
                  }
                }));
  }

  private void handleBackNav() {
    requireActivity()
        .getOnBackPressedDispatcher()
        .addCallback(
            getViewLifecycleOwner(),
            ActivityUtils.buildBackButtonCallback(
                logger,
                "digital cash home",
                () ->
                    LaoActivity.setCurrentFragment(
                        getParentFragmentManager(),
                        R.id.fragment_digital_cash_home,
                        DigitalCashHomeFragment::new)));
  }
}
