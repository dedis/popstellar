package com.github.dedis.popstellar.ui.detail.event.rollcall;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.RollCallFragmentBinding;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import net.glxn.qrgen.android.QRCode;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RollCallDetailFragment extends Fragment {

  public static final String TAG = RollCallDetailFragment.class.getSimpleName();
  private static final String EXTRA_PK = "pk";

  private RollCallFragmentBinding mRollCallFragBinding;
  private LaoDetailViewModel mLaoDetailViewModel;

  public static RollCallDetailFragment newInstance(PublicKey pk) {
    RollCallDetailFragment rollCallDetailFragment = new RollCallDetailFragment();
    Bundle bundle = new Bundle(1);
    bundle.putString(EXTRA_PK, pk.getEncoded());
    rollCallDetailFragment.setArguments(bundle);
    return rollCallDetailFragment;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mRollCallFragBinding = RollCallFragmentBinding.inflate(inflater, container, false);

    String pk = requireArguments().getString(EXTRA_PK);
    Bitmap myBitmap = QRCode.from(pk).bitmap();
    mRollCallFragBinding.pkQrCode.setImageBitmap(myBitmap);

    mLaoDetailViewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    mRollCallFragBinding.setLifecycleOwner(getActivity());

    return mRollCallFragBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    mRollCallFragBinding.backButton.setOnClickListener(
        clicked -> mLaoDetailViewModel.openLaoDetail());
  }
}
