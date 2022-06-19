package com.github.dedis.popstellar.ui.qrcode;

import static androidx.core.content.ContextCompat.checkSelfPermission;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.ActivityResultRegistry;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.QrcodeCameraPermFragmentBinding;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment handling permission granting for the camera */
@AndroidEntryPoint
public final class CameraPermissionFragment extends Fragment {

  public static final String TAG = CameraPermissionFragment.class.getSimpleName();
  public static final String REQUEST_KEY = "PERMISSION_REQUEST";

  private QrcodeCameraPermFragmentBinding mCameraPermFragBinding;
  private final ActivityResultLauncher<String> requestPermissionLauncher;

  public CameraPermissionFragment(@NonNull ActivityResultRegistry resultRegistry) {
    requestPermissionLauncher =
        registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            resultRegistry,
            isGranted -> {
              if (Boolean.TRUE.equals(isGranted)) {
                getParentFragmentManager().setFragmentResult(REQUEST_KEY, Bundle.EMPTY);
              }
            });
  }

  public static CameraPermissionFragment newInstance(
      @NonNull ActivityResultRegistry resultRegistry) {
    return new CameraPermissionFragment(resultRegistry);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    super.onCreateView(inflater, container, savedInstanceState);

    mCameraPermFragBinding = QrcodeCameraPermFragmentBinding.inflate(inflater, container, false);
    mCameraPermFragBinding.setLifecycleOwner(getViewLifecycleOwner());
    mCameraPermFragBinding.permissionManualRc.setOnClickListener(
        v -> LaoDetailActivity.obtainViewModel(getActivity()).openQrCodeScanningRollCall(true));
    return mCameraPermFragBinding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    setupCameraPermissionButton();
  }

  @Override
  public void onResume() {
    super.onResume();
    // If the permission was granted while the app was paused, switch to QRCodeScanningFragment
    if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED) {
      getParentFragmentManager().setFragmentResult(REQUEST_KEY, Bundle.EMPTY);
    }
  }

  private void setupCameraPermissionButton() {
    mCameraPermFragBinding.allowCameraButton.setOnClickListener(
        v -> requestPermissionLauncher.launch(Manifest.permission.CAMERA));
  }
}
