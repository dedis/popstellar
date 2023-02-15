package com.github.dedis.popstellar.ui.detail;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.InviteFragmentBinding;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.model.qrcode.ConnectToLao;
import com.github.dedis.popstellar.repository.remote.GlobalNetworkManager;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.google.gson.Gson;

import net.glxn.qrgen.android.QRCode;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class InviteFragment extends Fragment {

  private static final String TAG = InviteFragment.class.getSimpleName();

  @Inject Gson gson;
  @Inject GlobalNetworkManager networkManager;

  private static final int QR_SIDE = 800;

  private LaoDetailViewModel viewModel;

  public static InviteFragment newInstance() {
    return new InviteFragment();
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    InviteFragmentBinding binding = InviteFragmentBinding.inflate(inflater, container, false);
    viewModel = LaoDetailActivity.obtainViewModel(requireActivity());

    binding.laoPropertiesIdentifierText.setText(viewModel.getPublicKey().getEncoded());
    binding.laoPropertiesServerText.setText(networkManager.getCurrentUrl());

    try {
      LaoView laoView = viewModel.getLao();

      ConnectToLao data = new ConnectToLao(networkManager.getCurrentUrl(), laoView.getId());
      Bitmap myBitmap = QRCode.from(gson.toJson(data)).withSize(QR_SIDE, QR_SIDE).bitmap();
      binding.channelQrCode.setImageBitmap(myBitmap);
      binding.laoPropertiesNameText.setText(laoView.getName());

      viewModel
          .getRole()
          .observe(
              getViewLifecycleOwner(),
              role -> binding.laoPropertiesRoleText.setText(role.getStringId()));

    } catch (UnknownLaoException e) {
      ErrorUtils.logAndShow(requireContext(), TAG, e, R.string.unknown_lao_exception);
      return null;
    }
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    viewModel.setPageTitle(R.string.invite);
    viewModel.setIsTab(true);
  }
}
