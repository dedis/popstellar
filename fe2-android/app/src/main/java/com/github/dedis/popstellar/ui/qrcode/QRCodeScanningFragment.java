package com.github.dedis.popstellar.ui.qrcode;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.QrcodeFragmentBinding;
import com.github.dedis.popstellar.ui.detail.LaoDetailActivity;
import com.github.dedis.popstellar.ui.detail.LaoDetailViewModel;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/** Fragment handling the QR code scanning */
@AndroidEntryPoint
public final class QRCodeScanningFragment extends Fragment {

  public static final String TAG = QRCodeScanningFragment.class.getSimpleName();
  private static final int HANDLE_GMS = 9001;

  @Inject BarcodeDetector barcodeDetector;
  @Inject CameraProvider cameraProvider;

  private CameraSource camera;

  private QrcodeFragmentBinding mQrCodeFragBinding;
  private QRCodeScanningViewModel mQRCodeScanningViewModel;
  private CameraPreview mPreview;
  private Integer nbAttendees = 0;
  private AlertDialog closeRollCallAlert;

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mQrCodeFragBinding = QrcodeFragmentBinding.inflate(inflater, container, false);
    camera =
        cameraProvider.provide(
            getResources().getInteger(R.integer.camera_preview_width),
            getResources().getInteger(R.integer.camera_preview_height));

    FragmentActivity activity = getActivity();

    if (activity instanceof HomeActivity) {
      mQRCodeScanningViewModel = HomeActivity.obtainViewModel(activity);

    } else if (activity instanceof LaoDetailActivity) {
      mQRCodeScanningViewModel = LaoDetailActivity.obtainViewModel(activity);

      // Subscribe to "scan warning " event when the same QR code is scanned twice for example
      // It is used in the LaoDetailActivity whatever the scanning action is
      observeWarningEvent();

    } else {
      throw new IllegalArgumentException("cannot obtain view model");
    }

    if (mQRCodeScanningViewModel.getScanningAction() == ScanningAction.ADD_ROLL_CALL_ATTENDEE) {
      mQrCodeFragBinding.setScanningAction(ScanningAction.ADD_ROLL_CALL_ATTENDEE);
      mQrCodeFragBinding.addAttendeeTotalText.setVisibility(View.VISIBLE);
      mQrCodeFragBinding.addAttendeeNumberText.setVisibility(View.VISIBLE);
      mQrCodeFragBinding.addAttendeeConfirm.setVisibility(View.VISIBLE);

      mQrCodeFragBinding.manualAddConfirm.setOnClickListener(
          view -> {
            String data = mQrCodeFragBinding.manualAddEditText.getText().toString();
            boolean success = mQRCodeScanningViewModel.addManually(data);
            if (success) {
              mQrCodeFragBinding.manualAddEditText.getText().clear();
            }
          });
      // Subscribe to " Nb of attendees"  event
      observeNbAttendeesEvent();

      // Subscribe to " Attendees scan confirm " event
      observeAttendeeScanConfirmEvent();

      // set up the listener for the button that closes the roll call
      setupCloseRollCallButton();

      // Subscribe to "ask close roll call" event
      observeAskCloseRollCallEvent();
    } else if (mQRCodeScanningViewModel.getScanningAction() == ScanningAction.ADD_WITNESS) {
      mQrCodeFragBinding.setScanningAction(ScanningAction.ADD_WITNESS);
      // Subscribe to " Witness scan confirm " event
      observeWitnessScanConfirmEvent();
    } else if (mQRCodeScanningViewModel.getScanningAction() == ScanningAction.ADD_LAO_PARTICIPANT) {
      mQrCodeFragBinding.manualAddConfirm.setOnClickListener(
          view -> {
            String data = mQrCodeFragBinding.manualAddEditText.getText().toString();
            mQRCodeScanningViewModel.addManually(data);
          });
      mQrCodeFragBinding.setScanningAction(ScanningAction.ADD_LAO_PARTICIPANT);
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
    if (mPreview != null) {
      mPreview.stop();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mPreview != null) {
      mPreview.release();
    }
  }

  private void startCamera() throws SecurityException {
    if (mQRCodeScanningViewModel.isManual()){
      // For integration tests it currently not possible to grant permissions so for manual
      // adding mode, we do not start the camera
      return;
    }
    // check that the device has play services available.
    int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
    if (code != ConnectionResult.SUCCESS) {
      GoogleApiAvailability.getInstance()
          .getErrorDialog(requireActivity(), code, HANDLE_GMS)
          .show();
    }
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
    mQrCodeFragBinding.addAttendeeConfirm.setOnClickListener(clicked -> setupClickCloseListener());
  }

  private void setupClickCloseListener() {
    if (closeRollCallAlert != null && closeRollCallAlert.isShowing()) {
      closeRollCallAlert.dismiss();
    }
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle("Close Roll Call");
    builder.setMessage("You have scanned " + nbAttendees + " attendees.");
    builder.setOnDismissListener(dialog -> startCamera());
    builder.setPositiveButton(
        R.string.confirm,
        (dialog, which) -> ((LaoDetailViewModel) mQRCodeScanningViewModel).closeRollCall());
    builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
    mPreview.stop();
    closeRollCallAlert = builder.create();
    closeRollCallAlert.show();
  }

  private void setupSuccessPopup(String msg) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setTitle("Success");
    builder.setMessage(msg);
    builder.setOnDismissListener(dialog -> startCamera());
    AlertDialog alert = builder.create();
    mPreview.stop();
    alert.show();
    new Handler(Looper.myLooper())
        .postDelayed(
            () -> {
              if (alert.isShowing()) {
                alert.dismiss();
              }
            },
            2000);
  }

  private void setupWarningPopup(String msg) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setTitle("Warning");
    builder.setMessage(msg);
    builder.setOnDismissListener(dialog -> startCamera());
    builder.setPositiveButton(
        "Ok",
        (dialog, which) -> {
          if (dialog != null) {
            dialog.dismiss();
          }
        });
    mPreview.stop();
    builder.show();
  }

  void observeWarningEvent() {
    ((LaoDetailViewModel) mQRCodeScanningViewModel)
        .getScanWarningEvent()
        .observe(
            getViewLifecycleOwner(),
            stringEvent -> {
              String event = stringEvent.getContentIfNotHandled();
              if (event != null) {
                setupWarningPopup(event);
              }
            });
  }

  void observeNbAttendeesEvent() {
    ((LaoDetailViewModel) mQRCodeScanningViewModel)
        .getNbAttendeesEvent()
        .observe(
            getViewLifecycleOwner(),
            integerEvent -> {
              Integer event = integerEvent.getContentIfNotHandled();
              if (event != null) {
                nbAttendees = event;
                mQrCodeFragBinding.addAttendeeNumberText.setText(nbAttendees.toString());
              }
            });
  }

  void observeWitnessScanConfirmEvent() {
    ((LaoDetailViewModel) mQRCodeScanningViewModel)
        .getWitnessScanConfirmEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                setupSuccessPopup("A new witness was added to the the Lao");
              }
            });
  }

  void observeAttendeeScanConfirmEvent() {
    ((LaoDetailViewModel) mQRCodeScanningViewModel)
        .getAttendeeScanConfirmEvent()
        .observe(
            getViewLifecycleOwner(),
            stringEvent -> {
              String event = stringEvent.getContentIfNotHandled();
              if (event != null) {
                setupSuccessPopup(event);
              }
            });
  }

  void observeAskCloseRollCallEvent() {
    // observe events that require current open roll call to be closed
    ((LaoDetailViewModel) mQRCodeScanningViewModel)
        .getAskCloseRollCallEvent()
        .observe(
            getViewLifecycleOwner(),
            integerEvent -> {
              Integer nextFragment = integerEvent.getContentIfNotHandled();
              if (nextFragment != null) {
                setupClickCloseListener();
              }
            });
  }
}
