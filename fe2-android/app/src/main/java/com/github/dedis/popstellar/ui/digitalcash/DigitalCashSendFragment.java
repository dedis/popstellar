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
import com.github.dedis.popstellar.databinding.DigitalCashSendFragmentBinding;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;

import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;

import io.reactivex.Completable;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashSendFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class DigitalCashSendFragment extends Fragment {
  private static final String TAG = DigitalCashSendFragment.class.getSimpleName();
  private DigitalCashSendFragmentBinding binding;
  private LaoViewModel viewModel;
  private DigitalCashViewModel digitalCashViewModel;

  public DigitalCashSendFragment() {
    // Required empty constructor
  }

  /**
   * Use this factory method to create a new instance of this fragment
   *
   * @return A new instance of fragment DigitalCashSendFragment.
   */
  public static DigitalCashSendFragment newInstance() {
    return new DigitalCashSendFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    viewModel = LaoActivity.obtainViewModel(requireActivity());
    digitalCashViewModel =
        LaoActivity.obtainDigitalCashViewModel(requireActivity(), viewModel.getLaoId());
    binding = DigitalCashSendFragmentBinding.inflate(inflater, container, false);

    // Inflate the layout for this fragment
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setupSendCoinButton();

    digitalCashViewModel
        .getPostTransactionEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                String currentAmount = binding.digitalCashSendAmount.getText().toString();
                String currentPublicKeySelected =
                    String.valueOf(
                        Objects.requireNonNull(binding.digitalCashSendSpinner.getEditText())
                            .getText());
                if (digitalCashViewModel.canPerformTransaction(
                    currentAmount, currentPublicKeySelected, -1)) {
                  try {
                    PoPToken token = digitalCashViewModel.getValidToken();
                    if (canPostTransaction(token.getPublicKey(), Integer.parseInt(currentAmount))) {
                      viewModel.addDisposable(
                          postTransaction(
                                  Collections.singletonMap(currentPublicKeySelected, currentAmount))
                              .subscribe(
                                  () -> {
                                    digitalCashViewModel.updateReceiptAddressEvent(
                                        currentPublicKeySelected);
                                    digitalCashViewModel.updateReceiptAmountEvent(currentAmount);

                                    LaoActivity.setCurrentFragment(
                                        requireActivity().getSupportFragmentManager(),
                                        R.id.fragment_digital_cash_receipt,
                                        DigitalCashReceiptFragment::newInstance);
                                  },
                                  error -> Log.d(TAG, "error posting transaction", error)));
                    }

                  } catch (KeyException keyException) {
                    ErrorUtils.logAndShow(
                        requireContext(),
                        TAG,
                        keyException,
                        R.string.digital_cash_please_enter_a_lao);
                  }
                }
              }
            });

    try {
      setUpTheAdapter();
    } catch (KeyException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.digital_cash_error_poptoken);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.digital_cash_send);
    viewModel.setIsTab(false);
  }

  public boolean canPostTransaction(PublicKey publicKey, int amount) {
    long currentBalance = digitalCashViewModel.getUserBalance(publicKey);
    if (currentBalance < amount) {
      Log.d(TAG, "Current Balance: " + currentBalance + " amount: " + amount);
      Toast.makeText(
              requireContext(), R.string.digital_cash_warning_not_enough_money, Toast.LENGTH_SHORT)
          .show();
      return false;
    } else {
      return true;
    }
  }

  /** Function that set up the Adapter for the dropdown selector menu (with the public key list) */
  private void setUpTheAdapter() throws KeyException {
    /* Roll Call attendees to which we can send*/
    List<String> myArray;
    try {
      myArray = digitalCashViewModel.getAttendeesFromTheRollCallList();
    } catch (NoRollCallException e) {
      Toast.makeText(
              requireContext(), R.string.digital_cash_please_enter_roll_call, Toast.LENGTH_SHORT)
          .show();
      myArray = new ArrayList<>();
      LaoActivity.setCurrentFragment(
          getParentFragmentManager(),
          R.id.fragment_digital_cash_home,
          DigitalCashHomeFragment::newInstance);
    }
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(requireContext(), R.layout.list_item, myArray);
    Objects.requireNonNull(binding.digitalCashSendSpinner.getEditText())
        .setText(digitalCashViewModel.getValidToken().getPublicKey().getEncoded());
    binding.digitalCashSendSpinnerTv.setAdapter(adapter);
  }

  /** Function that setup the Button */
  private void setupSendCoinButton() {
    binding.digitalCashSendSend.setOnClickListener(
        v -> digitalCashViewModel.postTransactionEvent());
  }

  /**
   * Function that post the transaction (call the function of the view model)
   *
   * @param publicKeyAmount Map<String, String> containing the Public Keys and the related amount to
   *     issue to
   */
  private Completable postTransaction(Map<String, String> publicKeyAmount) {
    return digitalCashViewModel
        .postTransaction(publicKeyAmount, Instant.now().getEpochSecond(), false)
        .doOnComplete(
            () ->
                Toast.makeText(
                        requireContext(),
                        R.string.digital_cash_post_transaction,
                        Toast.LENGTH_SHORT)
                    .show())
        .doOnError(
            error -> {
              if (error instanceof KeyException || error instanceof GeneralSecurityException) {
                ErrorUtils.logAndShow(
                    requireContext(), TAG, error, R.string.error_retrieve_own_token);
              } else {
                ErrorUtils.logAndShow(
                    requireContext(), TAG, error, R.string.error_post_transaction);
              }
            });
  }
}
