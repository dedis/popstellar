package com.github.dedis.student20_pop.ui.qrcode;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
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

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.databinding.FragmentCameraPermBinding;
import com.github.dedis.student20_pop.home.HomeActivity;
import com.github.dedis.student20_pop.ui.qrcode.QRCodeScanningFragment.QRCodeScanningType;
import com.github.dedis.student20_pop.utility.qrcode.OnCameraAllowedListener;

/** Fragment handling permission granting for the camera */
public final class CameraPermissionFragment extends Fragment {

  public static final String TAG = CameraPermissionFragment.class.getSimpleName();
  private static final int HANDLE_CAMERA_PERM = 2;

  private FragmentCameraPermBinding mCameraPermFragBinding;

  private CameraPermissionViewModel mCameraPermissionViewModel;


  public static CameraPermissionFragment newInstance() {
    return new CameraPermissionFragment();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    mCameraPermFragBinding = FragmentCameraPermBinding.inflate(inflater, container, false);

    FragmentActivity activity = getActivity();
    if (activity instanceof HomeActivity) {
      mCameraPermissionViewModel = HomeActivity.obtainViewModel(activity);
    } else {
      throw new IllegalArgumentException("cannot obtain view model");
    }

    mCameraPermFragBinding.setLifecycleOwner(activity);

    return mCameraPermFragBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    ((HomeActivity) getActivity()).setupHomeButton();
    ((HomeActivity) getActivity()).setupConnectButton();
    ((HomeActivity) getActivity()).setupLaunchButton();

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
        == PackageManager.PERMISSION_GRANTED)
      mCameraPermissionViewModel.onPermissionGranted();
  }

  private void setupCameraPermissionButton() {
    mCameraPermFragBinding.allowCameraButton.setOnClickListener(v ->
            requestPermissions(new String[] {Manifest.permission.CAMERA}, HANDLE_CAMERA_PERM)
    );
  }
}
