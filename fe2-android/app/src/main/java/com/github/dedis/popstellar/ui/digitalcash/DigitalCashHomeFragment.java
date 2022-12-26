package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashHomeFragmentBinding;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.KeyException;

import java.util.Set;

import static com.github.dedis.popstellar.ui.digitalcash.DigitalCashActivity.TAG;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashHomeFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class DigitalCashHomeFragment extends Fragment {
  private DigitalCashHomeFragmentBinding binding;
  private DigitalCashViewModel viewModel;

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

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    viewModel = DigitalCashActivity.obtainViewModel(getActivity());
    binding = DigitalCashHomeFragmentBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    setHomeInterface();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.digital_cash_home);
  }

  public void setHomeInterface() {
    viewModel
        .getCurrentLao()
        .observe(
            requireActivity(),
            lao -> {
              if (lao == null) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.digital_cash_please_enter_a_lao),
                        Toast.LENGTH_SHORT)
                    .show();
              } else {
                try {
                  PoPToken token = viewModel.getValidToken();
                  PublicKey publicKey = token.getPublicKey();
                  binding.digitalCashHomeAddress.setText(publicKey.getEncoded());
                  if (lao.getTransactionByUser().containsKey(publicKey)) {
                    Set<TransactionObject> transactions = lao.getTransactionByUser().get(publicKey);
                    long totalAmount =
                        TransactionObject.getMiniLaoPerReceiverSetTransaction(
                            transactions, publicKey);
                    binding.digitalCashSendAddress.setText(
                        String.format("LAO coin : %s", totalAmount));
                  }

                } catch (KeyException e) {
                  ErrorUtils.logAndShow(
                      requireContext(), TAG, e, R.string.digital_cash_please_enter_roll_call);
                }
              }
            });
  }
}
