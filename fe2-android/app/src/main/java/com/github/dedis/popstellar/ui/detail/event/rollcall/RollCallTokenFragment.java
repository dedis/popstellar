package com.github.dedis.popstellar.ui.detail.event.rollcall;

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

import com.github.dedis.popstellar.databinding.RollCallTokenFragmentBinding;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import net.glxn.qrgen.android.QRCode;

import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Optional;

public class RollCallTokenFragment extends Fragment {

  public static final String TAG = RollCallTokenFragment.class.getSimpleName();
  public static final String EXTRA_ID = "rollcall_id";

  private LaoDetailViewModel mLaoDetailViewModel;
  private RollCallTokenFragmentBinding mRollCallTokenFragmentBinding;
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
    mRollCallTokenFragmentBinding =
        RollCallTokenFragmentBinding.inflate(inflater, container, false);

    mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(getActivity());

    String rollCallId = this.getArguments().getString(EXTRA_ID);
    Optional<RollCall> optRollCall =
        mLaoDetailViewModel.getCurrentLao().getValue().getRollCall(rollCallId);
    if (!optRollCall.isPresent()) {
      Log.d(TAG, "failed to retrieve roll call with id " + rollCallId);
      mLaoDetailViewModel.openLaoWallet();
    } else {
      rollCall = optRollCall.get();
    }

    String firstLaoId =
        mLaoDetailViewModel
            .getCurrentLaoValue()
            .getChannel()
            .substring(6); // use the laoId set at creation + need to remove /root/ prefix
    String sk = "";
    String pk = "";
    Log.d(TAG, "rollcall: " + rollCallId);
    try {
      Pair<byte[], byte[]> token =
          Wallet.getInstance().findKeyPair(firstLaoId, rollCall.getPersistentId());
      sk = Base64.getUrlEncoder().encodeToString(token.first);
      pk = Base64.getUrlEncoder().encodeToString(token.second);
    } catch (GeneralSecurityException e) {
      Log.d(TAG, "failed to retrieve token from wallet", e);
      mLaoDetailViewModel.openLaoWallet();
    }

    mRollCallTokenFragmentBinding.rollcallName.setText("Roll Call: " + rollCall.getName());
    mRollCallTokenFragmentBinding.privateKey.setText("Private key:\n" + sk);
    mRollCallTokenFragmentBinding.publicKey.setText("Public key:\n" + pk);

    Bitmap myBitmap = QRCode.from(pk).bitmap();
    mRollCallTokenFragmentBinding.pkQrCode.setImageBitmap(myBitmap);

    mRollCallTokenFragmentBinding.setLifecycleOwner(getActivity());

    return mRollCallTokenFragmentBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    mRollCallTokenFragmentBinding.backButton.setOnClickListener(
        clicked -> mLaoDetailViewModel.openLaoWallet());
  }
}
