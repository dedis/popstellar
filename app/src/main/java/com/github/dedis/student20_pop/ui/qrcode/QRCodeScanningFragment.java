package com.github.dedis.student20_pop.ui.qrcode;

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
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.databinding.FragmentHomeBinding;
import com.github.dedis.student20_pop.databinding.FragmentQrcodeBinding;
import com.github.dedis.student20_pop.home.HomeActivity;
import com.github.dedis.student20_pop.utility.qrcode.*;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

import static com.github.dedis.student20_pop.ui.qrcode.QRCodeScanningFragment.QRCodeScanningType.CONNECT_LAO;

/** Fragment handling the QR code scanning */
public final class QRCodeScanningFragment extends Fragment implements QRCodeListener {

  public static final String TAG = QRCodeScanningFragment.class.getSimpleName();
  private static final int HANDLE_GMS = 9001;

  private FragmentQrcodeBinding mQrCodeFragBinding;

  private QRCodeScanningViewModel mQRCodeScanningViewModel;

  private CameraSource camera;
  private CameraPreview preview;
  private QRCodeListener qrCodeListener;
  private QRCodeScanningType qrCodeScanningType;
  private String eventId;


  /** Default Fragment constructor */
  public QRCodeScanningFragment() {
    new QRCodeScanningFragment(CONNECT_LAO, null);
  }

  /**
   * Fragment constructor
   *
   * @param qrCodeScanningType tell what should be done with QR code information
   */
  public QRCodeScanningFragment(QRCodeScanningType qrCodeScanningType, String eventId) {
    super();
    this.qrCodeScanningType = qrCodeScanningType;
    this.eventId = eventId;
  }

  public static QRCodeScanningFragment newInstance() {
    return new QRCodeScanningFragment();
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    if (context instanceof QRCodeListener) qrCodeListener = (QRCodeListener) context;
    else throw new ClassCastException(context.toString() + " must implement QRCodeListener");
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {


    mQrCodeFragBinding = FragmentQrcodeBinding.inflate(inflater, container, false);

    FragmentActivity activity = getActivity();
    if (activity instanceof HomeActivity) {
      mQRCodeScanningViewModel = HomeActivity.obtainViewModel(activity);
    } else {
      throw new IllegalArgumentException("cannot obtain view model");
    }

    preview = mQrCodeFragBinding.qrCameraPreview;

    mQrCodeFragBinding.scanDescription.setText(mQRCodeScanningViewModel.getScanDescription());

    mQrCodeFragBinding.setLifecycleOwner(activity);

    createCamera();

    return mQrCodeFragBinding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    // If the permission was removed while the app was paused, switch to CameraPermissionFragment
    if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        == PackageManager.PERMISSION_GRANTED) startCamera();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (preview != null) preview.stop();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (preview != null) preview.release();
  }

  @Override
  public void onQRCodeDetected(String data, QRCodeScanningType qrCodeScanningType, String eventId) {
    Log.d(TAG, "QR Code detected");
    qrCodeListener.onQRCodeDetected(data, qrCodeScanningType, eventId);
  }

  private void createCamera() {
    BarcodeDetector qrDetector =
            new BarcodeDetector.Builder(getContext()).setBarcodeFormats(Barcode.QR_CODE).build();

    qrDetector.setProcessor(new QRFocusingProcessor(qrDetector, this, qrCodeScanningType, eventId));

    camera = new CameraSource.Builder(requireContext(), qrDetector)
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .setRequestedPreviewSize(
                    getResources().getInteger(R.integer.requested_preview_width),
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

  /**
   * Enum representing QR code functionality If QRCodeScanningFragment is launched to add a witness
   * or connect to a lao or to add an attendee to a roll call event
   */
  public enum QRCodeScanningType {
    ADD_ROLL_CALL_ATTENDEE,
    ADD_WITNESS,
    CONNECT_LAO
  }
}
