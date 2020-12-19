package com.github.dedis.student20_pop.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.utility.qrcode.CameraPreview;
import com.github.dedis.student20_pop.utility.qrcode.OnCameraNotAllowedListener;
import com.github.dedis.student20_pop.utility.qrcode.QRCodeListener;
import com.github.dedis.student20_pop.utility.qrcode.QRFocusingProcessor;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

import static com.github.dedis.student20_pop.ui.QRCodeScanningFragment.QRCodeScanningType.CONNECT_LAO;

/**
 * Fragment used to display the Connect UI
 **/
public final class QRCodeScanningFragment extends Fragment implements QRCodeListener {

    public static final String TAG = QRCodeScanningFragment.class.getSimpleName();
    private static final int HANDLE_GMS = 9001;

    private CameraSource camera;
    private CameraPreview preview;
    private OnCameraNotAllowedListener onCameraNotAllowedListener;
    private QRCodeListener qrCodeListener;
    private QRCodeScanningType qrCodeScanningType;

    /**
     * Default Fragment constructor
     */
    public QRCodeScanningFragment() {
        new QRCodeScanningFragment(CONNECT_LAO);
    }

    /**
     * Fragment constructor
     *
     * @param qrCodeScanningType tell what should be done with QR code information
     */
    public QRCodeScanningFragment(QRCodeScanningType qrCodeScanningType) {
        super();
        this.qrCodeScanningType = qrCodeScanningType;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            onCameraNotAllowedListener = (OnCameraNotAllowedListener) context;
            qrCodeListener = (QRCodeListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement listeners");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_qrcode, container, false);

        preview = view.findViewById(R.id.qr_camera_preview);
        TextView scanDescription = view.findViewById(R.id.scan_description);

        switch (qrCodeScanningType) {
            case CONNECT_LAO:
                scanDescription.setText(R.string.qrcode_scanning_connect_lao);
                break;
            case ADD_ROLL_CALL:
                scanDescription.setText("TODO");
                break;
            case ADD_WITNESS:
                scanDescription.setText(R.string.qrcode_scanning_add_witness);
                break;
        }

        // Check for the camera permission, if is is not granted, switch to CameraPermissionFragment
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            camera = createCamera();
        else
            onCameraNotAllowedListener.onCameraNotAllowedListener(qrCodeScanningType);

        return view;
    }

    private CameraSource createCamera() {
        BarcodeDetector qrDetector = new BarcodeDetector.Builder(getContext())
                .setBarcodeFormats(Barcode.QR_CODE)
                .build();

        qrDetector.setProcessor(new QRFocusingProcessor(qrDetector, this, qrCodeScanningType));

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
        // If the permission was removed while the app was paused, switch to CameraPermissionFragment

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            startCamera();
        else
            onCameraNotAllowedListener.onCameraNotAllowedListener(qrCodeScanningType);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (preview != null)
            preview.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (preview != null)
            preview.release();
    }

    @Override
    public void onQRCodeDetected(String data, QRCodeScanningType qrCodeScanningType) {
        qrCodeListener.onQRCodeDetected(data, qrCodeScanningType);
    }

    /**
     * Enum representing QR code functionality
     * If QRCodeScanningFragment is launched to add a witness
     * or connect to a lao or to add an attendee to a roll call event
     */
    public enum QRCodeScanningType {
        ADD_ROLL_CALL,
        ADD_WITNESS,
        CONNECT_LAO

    }
}