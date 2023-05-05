package com.github.dedis.popstellar.ui.qrcode;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.QrFragmentBinding;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.qrcode.PopTokenData;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.home.HomeViewModel;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import net.glxn.qrgen.android.QRCode;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class QrFragment extends Fragment {
  public static final String TAG = QrFragment.class.getSimpleName();

  // Dependencies
  @Inject Gson gson;
  @Inject KeyManager keyManager;

  private QrFragmentBinding binding;

  private HomeViewModel viewModel;

  public QrFragment() {
    // Required empty public constructor
  }

  public static QrFragment newInstance() {
    return new QrFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    binding = QrFragmentBinding.inflate(inflater, container, false);
    binding.setLifecycleOwner(getActivity());

    viewModel = HomeActivity.obtainViewModel(requireActivity());

    // Add the public key in form of text and QR
    setQRFromPublicKey();

    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.qr_title);
    viewModel.setIsHome(false);
  }

  private void setQRFromPublicKey() {
    PublicKey pk = keyManager.getMainPublicKey();
    binding.pkText.setText(pk.getEncoded());

    PopTokenData data = new PopTokenData(pk);
    Bitmap myBitmap =
        QRCode.from(gson.toJson(data))
            .withColor(ActivityUtils.getQRCodeColor(requireContext()), Color.TRANSPARENT)
            .bitmap();
    binding.pkQrCode.setImageBitmap(myBitmap);
  }
}
