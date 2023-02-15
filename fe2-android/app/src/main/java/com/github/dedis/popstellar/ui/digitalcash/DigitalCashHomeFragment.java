package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.util.Log;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashHomeFragmentBinding;
import com.github.dedis.popstellar.model.Role;
import com.github.dedis.popstellar.ui.lao.LaoActivity;
import com.github.dedis.popstellar.ui.lao.LaoViewModel;
import com.github.dedis.popstellar.utility.error.ErrorUtils;

import io.reactivex.android.schedulers.AndroidSchedulers;


/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashHomeFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class DigitalCashHomeFragment extends Fragment {
  private DigitalCashHomeFragmentBinding binding;
  private LaoViewModel viewModel;
  private DigitalCashViewModel digitalCashViewModel;

  public DigitalCashHomeFragment() {
    // Required empty constructor
  }
  /**
   * Use this factory method to create a new instance of this fragment
   *
   * @return A new instance of fragment DigitalCashHomeFragment.
   */
  public static DigitalCashHomeFragment newInstance() {
    return new DigitalCashHomeFragment();
  }

  public static final String TAG = DigitalCashHomeFragment.class.getSimpleName();

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    viewModel = LaoActivity.obtainViewModel(requireActivity());

    digitalCashViewModel =
        LaoActivity.obtainDigitalCashViewModel(requireActivity(), viewModel.getLaoId());
    binding = DigitalCashHomeFragmentBinding.inflate(inflater, container, false);

    subscribeToTransactions();
    subscribeToRole();
    setupReceiveButton();
    setupSendButton();
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.digital_cash_home);
    viewModel.setIsTab(true);
  }

  private void subscribeToTransactions() {
    viewModel.addDisposable(
        digitalCashViewModel
            .getTransactionsObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                transactions -> {
                  Log.d(TAG, "updating transactions " + transactions);
                  long totalAmount = digitalCashViewModel.getOwnBalance();
                  binding.coinAmountText.setText(String.valueOf(totalAmount));
                },
                error ->
                    ErrorUtils.logAndShow(
                        requireContext(), TAG, error, R.string.error_retrieve_own_token)));
  }

  private void setupReceiveButton() {
    View.OnClickListener receiveListener =
        v ->
            LaoActivity.setCurrentFragment(
                getParentFragmentManager(),
                R.id.fragment_digital_cash_receive,
                DigitalCashReceiveFragment::newInstance);

    binding.digitalCashReceiveButton.setOnClickListener(receiveListener);
    binding.digitalCashReceiveText.setOnClickListener(receiveListener);
  }

  private void setupSendButton() {
    View.OnClickListener sendListener =
        v ->
            LaoActivity.setCurrentFragment(
                getParentFragmentManager(),
                R.id.fragment_digital_cash_send,
                DigitalCashSendFragment::newInstance);

    binding.digitalCashSendButton.setOnClickListener(sendListener);
    binding.digitalCashSendText.setOnClickListener(sendListener);
  }

  private void subscribeToRole() {
    viewModel
        .getRole()
        .observe(
            getViewLifecycleOwner(),
            role -> {
              if (role == Role.ORGANIZER) {
                binding.issueButton.setVisibility(View.VISIBLE);
                binding.issueButton.setOnClickListener(
                    v ->
                        LaoActivity.setCurrentFragment(
                            getParentFragmentManager(),
                            R.id.fragment_digital_cash_issue,
                            DigitalCashIssueFragment::newInstance));
              } else {
                binding.issueButton.setVisibility(View.GONE);
              }
            });
  }
}
