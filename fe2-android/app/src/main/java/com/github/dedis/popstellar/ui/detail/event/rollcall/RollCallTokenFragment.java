package com.github.dedis.popstellar.ui.detail.event.rollcall;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.RollCallTokenFragmentBinding;
import com.github.dedis.popstellar.model.objects.RollCall;
import com.github.dedis.popstellar.model.objects.Wallet;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.utility.error.keys.KeyException;

import net.glxn.qrgen.android.QRCode;

import java.util.Optional;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RollCallTokenFragment extends Fragment {

  public static final String TAG = RollCallTokenFragment.class.getSimpleName();
  public static final String EXTRA_ID = "rollcall_id";

  @Inject Wallet wallet;
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

    mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    String rollCallId = requireArguments().getString(EXTRA_ID);
    Optional<RollCall> optRollCall =
        mLaoDetailViewModel.getCurrentLao().getValue().getRollCall(rollCallId);
    if (!optRollCall.isPresent()) {
      Log.d(TAG, "failed to retrieve roll call with id " + rollCallId);
      mLaoDetailViewModel.openLaoWallet(getParentFragmentManager());
    } else {
      rollCall = optRollCall.get();
    }

    String firstLaoId = mLaoDetailViewModel.getCurrentLaoValue().getId();
    String pk = "";
    Log.d(TAG, "rollcall: " + rollCallId);
    try {
      PoPToken token = wallet.generatePoPToken(firstLaoId, rollCall.getPersistentId());
      pk = token.getPublicKey().getEncoded();
    } catch (KeyException e) {
      Log.d(TAG, "failed to retrieve token from wallet", e);
      mLaoDetailViewModel.openLaoWallet(getParentFragmentManager());
    }

    mRollCallTokenFragmentBinding.rollcallName.setText("Roll Call: " + rollCall.getName());
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
        clicked -> mLaoDetailViewModel.openLaoWallet(getParentFragmentManager()));
  }
}
