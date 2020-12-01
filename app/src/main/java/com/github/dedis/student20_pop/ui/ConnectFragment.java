package com.github.dedis.student20_pop.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.utility.qrcode.CameraPreview;
import com.github.dedis.student20_pop.utility.qrcode.QRCodeListener;
import com.github.dedis.student20_pop.utility.qrcode.QRFocusingProcessor;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

/**
 * Fragment used to display the Connect UI
 **/
public final class ConnectFragment extends Fragment implements QRCodeListener {

    public static final String TAG = ConnectFragment.class.getSimpleName();

    private static final int HANDLE_GMS = 9001;

    private CameraSource camera;
    private CameraPreview preview;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connect, container, false);

        preview = view.findViewById(R.id.qr_camera_preview);

        // Check for the camera permission, if is is not granted, switch to CameraPermissionFragment
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            camera = createCamera();
        else
            switchToCameraPermissionFragment();

        return view;
    }

    private CameraSource createCamera() {
        BarcodeDetector qrDetector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        qrDetector.setProcessor(new QRFocusingProcessor(qrDetector, this));

        return new CameraSource.Builder(requireContext(), qrDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(getResources().getInteger(R.integer.requested_preview_width),
                        getResources().getInteger(R.integer.requested_preview_height))
                .setRequestedFps(15.0f)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                .build();
    }


    private void switchToCameraPermissionFragment() {
        requireFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container_main, new CameraPermissionFragment(), CameraPermissionFragment.TAG)
                .commit();
    }

    private void startCamera() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getContext());
        if (code != ConnectionResult.SUCCESS)
            GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), code, HANDLE_GMS).show();

        if (camera != null) {
            try {
                preview.start(camera);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                camera.release();
                camera = null;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // If the permission was removed while the app was paused, switch to CameraPermissionFragment

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            startCamera();
        else
            switchToCameraPermissionFragment();
    }

    @Override
    public void onPause() {
        super.onPause();
        if(preview != null)
            preview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(preview != null)
            preview.release();
    }

    @Override
    public void onQRCodeDetected(String url) {
        Log.i(TAG, "Received qrcode url : " + url);
        requireFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container_main, ConnectingFragment.newInstance(url), ConnectingFragment.TAG)
                .addToBackStack(TAG).commit();
    }
}