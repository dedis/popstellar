package com.github.dedis.popstellar.ui.qrcode;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.QrcodeCameraPermFragmentBinding;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.home.HomeActivity;

/** Fragment handling permission granting for the camera */
public final class CameraPermissionFragment extends Fragment {

  public static final String TAG = CameraPermissionFragment.class.getSimpleName();

  private static final int HANDLE_CAMERA_PERM = 2;

  private QrcodeCameraPermFragmentBinding mCameraPermFragBinding;
  private CameraPermissionViewModel mCameraPermissionViewModel;

  public static CameraPermissionFragment newInstance() {
    return new CameraPermissionFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    mCameraPermFragBinding = QrcodeCameraPermFragmentBinding.inflate(inflater, container, false);

    FragmentActivity activity = getActivity();
    if (activity instanceof HomeActivity) {
      mCameraPermissionViewModel = HomeActivity.obtainViewModel(activity);
    } else if (activity instanceof LaoDetailActivity) {
      mCameraPermissionViewModel = LaoDetailActivity.obtainViewModel(activity);
    } else {
      throw new IllegalArgumentException("cannot obtain view model for " + TAG);
    }

    mCameraPermFragBinding.setLifecycleOwner(activity);

    return mCameraPermFragBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    Button back = getActivity().findViewById(R.id.tab_back);
    back.setOnClickListener(c -> ((LaoDetailViewModel) mCameraPermissionViewModel).openLaoDetail());
    setupCameraPermissionButton();
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == HANDLE_CAMERA_PERM
        && grantResults.length != 0
        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      mCameraPermissionViewModel.onPermissionGranted();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    // If the permission was granted while the app was paused, switch to QRCodeScanningFragment
    if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED) {
      mCameraPermissionViewModel.onPermissionGranted();
    }
  }

  private void setupCameraPermissionButton() {
    mCameraPermFragBinding.allowCameraButton.setOnClickListener(
        v -> requestPermissions(new String[] {Manifest.permission.CAMERA}, HANDLE_CAMERA_PERM));
  }
}
