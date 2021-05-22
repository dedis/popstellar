package com.github.dedis.student20_pop.qrcode;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.student20_pop.databinding.FragmentQrcodeBinding;
import com.github.dedis.student20_pop.detail.LaoDetailActivity;
import com.github.dedis.student20_pop.detail.LaoDetailViewModel;
import com.github.dedis.student20_pop.home.HomeActivity;
import com.github.dedis.student20_pop.utility.qrcode.BarcodeTracker;
import com.github.dedis.student20_pop.utility.qrcode.CameraPreview;
import com.github.dedis.student20_pop.utility.qrcode.QRFocusingProcessor;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

/** Fragment handling the QR code scanning */
public final class QRCodeScanningFragment extends Fragment {
  public static final String TAG = QRCodeScanningFragment.class.getSimpleName();
  private static final int HANDLE_GMS = 9001;
  private FragmentQrcodeBinding mQrCodeFragBinding;
  private QRCodeScanningViewModel mQRCodeScanningViewModel;
  private CameraSource camera;
  private CameraPreview mPreview;
  private BarcodeDetector barcodeDetector;
  /** Fragment constructor */
  public QRCodeScanningFragment(CameraSource camera, BarcodeDetector detector) {
    super();
    this.camera = camera;
    this.barcodeDetector = detector;
  }
  public static QRCodeScanningFragment newInstance(CameraSource camera, BarcodeDetector detector) {
    return new QRCodeScanningFragment(camera, detector);
  }
  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
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
    } else if (activity instanceof LaoDetailActivity) {
      mQRCodeScanningViewModel = LaoDetailActivity.obtainViewModel(activity);
      mQrCodeFragBinding.addAttendeeTotalText.setVisibility(View.VISIBLE);
      mQrCodeFragBinding.addAttendeeNumberText.setVisibility(View.VISIBLE);
      mQrCodeFragBinding.addAttendeeConfirm.setVisibility(View.VISIBLE);
      ((LaoDetailViewModel)mQRCodeScanningViewModel)
              .getNbAttendeesEvent()
              .observe(
                      this,
                      integerEvent -> {
                        Integer event = integerEvent.getContentIfNotHandled();
                        if (event != null) {
                          mQrCodeFragBinding.addAttendeeNumberText.setText(event.toString());
                        }
                      });
      ((LaoDetailViewModel)mQRCodeScanningViewModel)
              .getAttendeeScanConfirmEvent()
              .observe(
                      this,
                      stringEvent -> {
                        String event = stringEvent.getContentIfNotHandled();
                        if (event != null) {
                          setupSuccessPopup(event);
                        } });
      ((LaoDetailViewModel)mQRCodeScanningViewModel)
              .getScanWarningEvent()
              .observe(
                      this,
                      stringEvent -> {
                        String event = stringEvent.getContentIfNotHandled();
                        if (event != null) {
                          setupWarningPopup(event);
                        } });
      setupCloseRollCallButton();

      // Subscribe to "close roll call" event
      ((LaoDetailViewModel)mQRCodeScanningViewModel)
              .getCloseRollCallEvent()
              .observe(
                      this,
                      booleanEvent -> {
                        Boolean action = booleanEvent.getContentIfNotHandled();
                        if (action != null) {
                          closeRollCall();
                        }
                      });
    }else {
      throw new IllegalArgumentException("cannot obtain view model");
    }
    mPreview = mQrCodeFragBinding.qrCameraPreview;
    mQrCodeFragBinding.scanDescription.setText(mQRCodeScanningViewModel.getScanDescription());
    mQrCodeFragBinding.setLifecycleOwner(activity);
    // TODO: consider removing coupling with QRFocusingProcessor
    barcodeDetector.setProcessor(
            new QRFocusingProcessor(
                    barcodeDetector, BarcodeTracker.getInstance(mQRCodeScanningViewModel)));
    return mQrCodeFragBinding.getRoot();
  }
  @Override
  public void onResume() {
    super.onResume();
    startCamera();
  }
  @Override
  public void onPause() {
    super.onPause();
    if (mPreview != null) mPreview.stop();
  }
  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mPreview != null) mPreview.release();
  }
  private void startCamera() throws SecurityException {
    // check that the device has play services available.
    int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getContext());
    if (code != ConnectionResult.SUCCESS)
      GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), code, HANDLE_GMS).show();
    if (camera != null) {
      try {
        mPreview.start(camera);
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        camera.release();
      }
    }
  }
  private void setupCloseRollCallButton() {
    mQrCodeFragBinding.addAttendeeConfirm.setOnClickListener(
            v -> ((LaoDetailViewModel)mQRCodeScanningViewModel).closeRollCall());
  }

  private void closeRollCall() {
    ((LaoDetailViewModel)mQRCodeScanningViewModel).openLaoDetail();
  }

  private void setupSuccessPopup(String msg){
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setTitle("Success");
    builder.setMessage(msg);
    mPreview.stop();
    AlertDialog dialog = builder.show();
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        if (dialog.isShowing()){
          dialog.dismiss();
          startCamera();
        }
      }
    }, 2000);
  }

  private void setupWarningPopup(String msg){
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setTitle("Warning");
    builder.setMessage(msg);
    mPreview.stop();
    builder.setPositiveButton("Ok", (dialog, which) -> {
      dialog.dismiss();
      startCamera();
    });
    builder.show();
  }
}