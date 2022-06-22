package com.github.dedis.popstellar.ui.digitalcash;

import static com.github.dedis.popstellar.ui.digitalcash.DigitalCashActivity.TAG;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashHomeFragmentBinding;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.keys.KeyException;

import java.util.List;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashHomeFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class DigitalCashHomeFragment extends Fragment {
    private DigitalCashHomeFragmentBinding mBinding;
    private DigitalCashViewModel mViewModel;

    public DigitalCashHomeFragment() {
        // not implemented yet
    }

    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @return A new instance of fragment DigitalCashHomeFragment.
     */
    public static DigitalCashHomeFragment newInstance() {
        return new DigitalCashHomeFragment();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.mViewModel = DigitalCashActivity.obtainViewModel(getActivity());
        mBinding = DigitalCashHomeFragmentBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHomeInterface();
    }

    public void setHomeInterface() {
        Lao lao = mViewModel.getCurrentLao();
        if (lao == null) {
            Toast.makeText(
                            requireContext(),
                            getString(R.string.digital_cash_please_enter_a_lao),
                            Toast.LENGTH_SHORT)
                    .show();
        } else {
            try {
                PoPToken token = mViewModel.getKeyManager().getValidPoPToken(lao);
                PublicKey publicKey = token.getPublicKey();
                mBinding.digitalCashHomeAddress.setText(publicKey.getEncoded());
                if (lao.getTransactionByUser().containsKey(publicKey)) {
                    List<TransactionObject> transactions = lao.getTransactionByUser().get(publicKey);
                    long totalAmount =
                            TransactionObject.getMiniLaoPerReceiverSetTransaction(transactions, publicKey);
                    mBinding.digitalCashSendAddress.setText(String.format("LAO coin : %s", totalAmount));
                }

            } catch (KeyException e) {
                ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.digital_cash_please_enter_roll_call);
            }
        }
    }
}

