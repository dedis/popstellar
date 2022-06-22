package com.github.dedis.popstellar.ui.digitalcash;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.DigitalCashSendFragmentBinding;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.security.KeyManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass. Use the {@link DigitalCashSendFragment#newInstance} factory
 * method to create an instance of this fragment.
 */
public class DigitalCashSendFragment extends Fragment {
    private DigitalCashSendFragmentBinding mBinding;
    private DigitalCashViewModel mViewModel;

    public DigitalCashSendFragment() {
        // not implemented yet
    }

    /**
     * Use this factory method to create a new instance of this fragment using the provided
     * parameters.
     *
     * @return A new instance of fragment DigitalCashSendFragment.
     */
    public static DigitalCashSendFragment newInstance() {
        return new DigitalCashSendFragment();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mViewModel = DigitalCashActivity.obtainViewModel(getActivity());
        mBinding = DigitalCashSendFragmentBinding.inflate(inflater, container, false);

        // Inflate the layout for this fragment
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupSendCoinButton();
        mBinding.digitalCashSendAmount.setText("0");

        mViewModel
                .getPostTransactionEvent()
                .observe(
                        getViewLifecycleOwner(),
                        booleanEvent -> {
                            Boolean event = booleanEvent.getContentIfNotHandled();
                            if (event != null) {
                                String currentAmount = mBinding.digitalCashSendAmount.getText().toString();
                                String currentPublicKeySelected =
                                        String.valueOf(mBinding.digitalCashSendSpinner.getEditText().getText());
                                if (mViewModel.canPerformTransaction(currentAmount, currentPublicKeySelected, -1)) {
                                    try {
                                        Lao lao = mViewModel.getCurrentLao();
                                        PoPToken token = mViewModel.getKeyManager().getValidPoPToken(lao);
                                        if (canPostTransaction(
                                                lao, token.getPublicKey(), Integer.parseInt(currentAmount))) {
                                            postTransaction(
                                                    Collections.singletonMap(currentPublicKeySelected, currentAmount));
                                            mViewModel.updateReceiptAddressEvent(currentPublicKeySelected);
                                            mViewModel.updateReceiptAmountEvent(currentAmount);
                                            mViewModel.openReceipt();
                                        }

                                    } catch (KeyException keyException) {
                                        Toast.makeText(
                                                        requireContext(),
                                                        R.string.digital_cash_please_enter_a_lao,
                                                        Toast.LENGTH_SHORT)
                                                .show();
                                    }
                                }
                            }
                        });

        try {
            setUpTheAdapter();
        } catch (KeyException e) {
            Toast.makeText(
                            requireContext(),
                            R.string.digital_cash_error_poptoken,
                            Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * Function that check whether the user has money in his/her account. Check also if has enough money to pay.
     */
    public boolean canPostTransaction(Lao lao, PublicKey publicKey, int currentAmount) {
        Map<PublicKey, List<TransactionObject>> transactionByUser = lao.getTransactionByUser();
        if (transactionByUser.isEmpty() || !transactionByUser.containsKey(publicKey)) {
            Toast.makeText(requireContext(), R.string.digital_cash_warning_no_money, Toast.LENGTH_SHORT)
                    .show();
            return false;
        }
        long amount =
                TransactionObject.getMiniLaoPerReceiverSetTransaction(
                        transactionByUser.get(publicKey), publicKey);
        if (amount < currentAmount) {
            Toast.makeText(
                            requireContext(), R.string.digital_cash_warning_not_enough_money, Toast.LENGTH_SHORT)
                    .show();
            return false;
        } else {
            return true;
        }
    }

    /**
     * Function that set up the Adapter for the dropdown selector menu (with the public key list)
     */
    private void setUpTheAdapter() throws KeyException {
        /* Roll Call attendees to which we can send*/
        List<String> myArray;
        try {
            myArray = mViewModel.getAttendeesFromTheRollCallList();
        } catch (NoRollCallException e) {
            mViewModel.openHome();
            Toast.makeText(
                            requireContext(), R.string.digital_cash_please_enter_roll_call, Toast.LENGTH_SHORT)
                    .show();
            myArray = new ArrayList<>();
        }
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(requireContext(), R.layout.list_item, myArray);
        KeyManager km = mViewModel.getKeyManager();
        mBinding.digitalCashSendSpinner.getEditText().setText(km.getValidPoPToken(mViewModel.getCurrentLao()).getPublicKey().getEncoded());
        mBinding.digitalCashSendSpinnerTv.setAdapter(adapter);
    }

    /**
     * Function that setup the Button
     */
    private void setupSendCoinButton() {
        mBinding.digitalCashSendSend.setOnClickListener(v -> mViewModel.postTransactionEvent());
    }

    /**
     * Function that post the transaction (call the function of the view model)
     *
     * @param publicKeyAmount Map<String, String> containing the Public Keys and the related amount to
     *                        issue to
     * @throws KeyException throw this exception if the key of the issuer is not on the LAO
     */
    private void postTransaction(Map<String, String> publicKeyAmount) {
        // Add some check if have money
        if (mViewModel.getLaoId().getValue() == null) {
            Toast.makeText(
                            requireContext().getApplicationContext(), R.string.error_no_lao, Toast.LENGTH_LONG)
                    .show();
        } else {
            mViewModel.postTransaction(publicKeyAmount, 0, false);
            mViewModel.updateLaoCoinEvent();
        }
    }
}
