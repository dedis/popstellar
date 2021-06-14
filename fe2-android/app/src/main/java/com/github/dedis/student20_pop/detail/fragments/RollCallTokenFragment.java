package com.github.dedis.student20_pop.detail.fragments;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.databinding.FragmentRollcallTokenBinding;
import com.github.dedis.student20_pop.detail.LaoDetailActivity;
import com.github.dedis.student20_pop.detail.LaoDetailViewModel;
import com.github.dedis.student20_pop.model.RollCall;
import com.github.dedis.student20_pop.model.Wallet;

import net.glxn.qrgen.android.QRCode;

import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Optional;

public class RollCallTokenFragment extends Fragment {
    public static final String TAG = RollCallTokenFragment.class.getSimpleName();
    public static final String EXTRA_ID = "rollcall_id";

    private LaoDetailViewModel mLaoDetailViewModel;
    private FragmentRollcallTokenBinding mFragmentRollcallTokenBinding;
    private RollCall rollCall;

    public static RollCallTokenFragment newInstance(String rollCallId) {
        RollCallTokenFragment rollCallTokenFragment = new RollCallTokenFragment();
        Bundle bundle = new Bundle(1);
        bundle.putString(EXTRA_ID, rollCallId);
        rollCallTokenFragment.setArguments(bundle);
        return rollCallTokenFragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mFragmentRollcallTokenBinding = FragmentRollcallTokenBinding.inflate(inflater, container, false);

        mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(getActivity());

        String rollCallId = this.getArguments().getString(EXTRA_ID);
        Optional<RollCall> optRollCall = mLaoDetailViewModel.getCurrentLao().getValue().getRollCall(rollCallId);
        if(!optRollCall.isPresent()){
            Log.d(TAG, "failed to retrieve roll call with id "+rollCallId);
            mLaoDetailViewModel.openLaoWallet();
        }else{
            rollCall = optRollCall.get();
        }

        String firstLaoId = mLaoDetailViewModel.getCurrentLaoValue().getChannel().substring(6); // use the laoId set at creation + need to remove /root/ prefix
        String sk = "";
        String pk = "";
        Log.d(TAG, "rollcall: "+rollCallId);
        try {
            Pair<byte[], byte[]> token = Wallet.getInstance().findKeyPair(firstLaoId, rollCall.getPersistentId());
            sk = Base64.getUrlEncoder().encodeToString(token.first);
            pk = Base64.getUrlEncoder().encodeToString(token.second);
        } catch (GeneralSecurityException e) {
            Log.d(TAG, "failed to retrieve token from wallet", e);
            mLaoDetailViewModel.openLaoWallet();
        }

        mFragmentRollcallTokenBinding.rollcallName.setText("Roll Call: "+rollCall.getName());
        mFragmentRollcallTokenBinding.privateKey.setText("Private key:\n"+sk);
        mFragmentRollcallTokenBinding.publicKey.setText("Public key:\n"+pk);

        Bitmap myBitmap = QRCode.from(pk).bitmap();
        mFragmentRollcallTokenBinding.pkQrCode.setImageBitmap(myBitmap);

        mFragmentRollcallTokenBinding.setLifecycleOwner(getActivity());

        return mFragmentRollcallTokenBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFragmentRollcallTokenBinding.backButton.setOnClickListener(clicked -> mLaoDetailViewModel.openLaoWallet());
    }
}

