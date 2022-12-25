package com.github.dedis.popstellar.ui.digitalcash;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashReceiveFragmentBinding;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.model.qrcode.PopTokenData;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.google.gson.Gson;

import net.glxn.qrgen.android.QRCode;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashReceiveFragment#newInstance}
 * factory method to create an instance of this fragment.
 */
@AndroidEntryPoint
public class DigitalCashReceiveFragment extends Fragment {
  public static final String TAG = DigitalCashReceiveFragment.class.getSimpleName();

  @Inject Gson gson;

  private DigitalCashReceiveFragmentBinding binding;
  private DigitalCashViewModel viewModel;

  public DigitalCashReceiveFragment() {
    // Required empty constructor
  }

  /**
   * Use this factory method to create a new instance of this fragment using the provided
   * parameters.
   *
   * @return A new instance of fragment DigitalCashReceiveFragment.
   */
  public static DigitalCashReceiveFragment newInstance() {
    return new DigitalCashReceiveFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    viewModel = DigitalCashActivity.obtainViewModel(getActivity());
    binding = DigitalCashReceiveFragmentBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    try {
      LaoView laoView = viewModel.getCurrentLaoValue();
      PoPToken token = viewModel.getValidToken();
      PublicKey user = token.getPublicKey();
      PopTokenData tokenData = new PopTokenData(token.getPublicKey());
      Bitmap myBitmap = QRCode.from(gson.toJson(tokenData)).bitmap();
      binding.digitalCashReceiveQr.setImageBitmap(myBitmap);
      List<TransactionObject> transactions = viewModel.getTransactionsForUser(user);
      if (transactions != null) {
        TransactionObject transaction = TransactionObject.lastLockedTransactionObject(transactions);
        String sender = transaction.getSendersTransaction().get(0).getEncoded();

        binding.digitalCashReceiveAddress.setText(String.format("Received from : %n %s", sender));

        binding.digitalCashReceiveAmount.setText(
            String.format(
                "%s LAOcoin", transaction.getMiniLaoPerReceiverFirst(token.getPublicKey())));
      }
    } catch (KeyException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.digital_cash_please_enter_roll_call);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.digital_cash_receive);
  }
}
