package com.github.dedis.student20_pop.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.ui.QRCodeScanningFragment.QRCodeScanningType;
import com.github.dedis.student20_pop.utility.qrcode.OnCameraAllowedListener;

/**
 * Fragment handling permission granting for the camera
 */
public final class CameraPermissionFragment extends Fragment implements View.OnClickListener {

    public static final String TAG = CameraPermissionFragment.class.getSimpleName();
    private static final int HANDLE_CAMERA_PERM = 2;
    private final QRCodeScanningType qrCodeScanningType;

    private OnCameraAllowedListener onCameraAllowedListener;

    public CameraPermissionFragment(QRCodeScanningType qrCodeScanningType) {
        super();
        this.qrCodeScanningType = qrCodeScanningType;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            onCameraAllowedListener = (OnCameraAllowedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement listeners");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera_perm, container, false);
        //set button click listener
        view.findViewById(R.id.allow_camera_button).setOnClickListener(this);

        // Check for the camera permission, if is is granted, switch to QRCodeScanningFragment
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            onCameraAllowedListener.onCameraAllowedListener(qrCodeScanningType);
        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == HANDLE_CAMERA_PERM &&
                grantResults.length != 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // we have permission, so we switch to QRCodeScanningFragment
            onCameraAllowedListener.onCameraAllowedListener(qrCodeScanningType);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // If the permission was granted while the app was paused, switch to QRCodeScanningFragment

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            onCameraAllowedListener.onCameraAllowedListener(qrCodeScanningType);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.allow_camera_button) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, HANDLE_CAMERA_PERM);
        }
    }
}
