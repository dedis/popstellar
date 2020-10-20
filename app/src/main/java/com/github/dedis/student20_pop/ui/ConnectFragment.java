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
import com.github.dedis.student20_pop.qrcode.CameraPreview;
import com.github.dedis.student20_pop.qrcode.QRCodeListener;
import com.github.dedis.student20_pop.qrcode.QRFocusingProcessor;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

/**
 * Fragment used to display the Connect UI
**/
public final class ConnectFragment extends Fragment implements QRCodeListener, View.OnClickListener {

    public static final String TAG = ConnectFragment.class.getSimpleName();

    private static final int HANDLE_GMS = 9001;
    private static final int HANDLE_CAMERA_PERM = 2;

    private CameraSource camera;
    private CameraPreview preview;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_connect, container, false);

        preview = view.findViewById(R.id.qr_camera_preview);
        //set button click listener
        view.findViewById(R.id.allow_camera_button).setOnClickListener(this);

        // Check for the camera permission, if is is not granted, ask for it
        int rc = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            camera = createCamera(view);
        } else {
            view.findViewById(R.id.qr_camera_preview).setVisibility(View.GONE);
        }

        return view;
    }

    private CameraSource createCamera(View view) {
        view.findViewById(R.id.camera_permission).setVisibility(View.GONE);
        preview.setVisibility(View.VISIBLE);

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
        startCamera();
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
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == HANDLE_CAMERA_PERM &&
                grantResults.length != 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // we have permission, so create the camerasource
            camera = createCamera(requireView());
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.allow_camera_button) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, HANDLE_CAMERA_PERM);
        }
    }

    @Override
    public void onQRCodeDetected(String url) {
        Log.i(TAG, "Received qrcode url : " + url);
        requireFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, ConnectingFragment.newInstance(url), TAG)
                .addToBackStack(TAG).commit();
    }
}
