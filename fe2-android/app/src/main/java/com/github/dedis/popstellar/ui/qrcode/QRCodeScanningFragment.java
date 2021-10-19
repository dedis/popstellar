package com.github.dedis.popstellar.ui.qrcode;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

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

/** Fragment handling the QR code scanning */
public final class QRCodeScanningFragment extends Fragment {

  public static final String TAG = QRCodeScanningFragment.class.getSimpleName();
  private static final int HANDLE_GMS = 9001;
  private QrcodeFragmentBinding mQrCodeFragBinding;
  private QRCodeScanningViewModel mQRCodeScanningViewModel;
  private CameraSource camera;
  private CameraPreview mPreview;
  private BarcodeDetector barcodeDetector;
  private Integer nbAttendees = 0;
  private AlertDialog closeRollCallAlert;

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
    mQrCodeFragBinding = QrcodeFragmentBinding.inflate(inflater, container, false);
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
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (mQRCodeScanningViewModel.getScanningAction() == ScanningAction.ADD_WITNESS) {
      Button back = getActivity().findViewById(R.id.tab_back);
      back.setOnClickListener(c -> ((LaoDetailViewModel) mQRCodeScanningViewModel).openLaoDetail());
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
    // check that the device has play services available.
    int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getContext());
    if (code != ConnectionResult.SUCCESS) {
      GoogleApiAvailability.getInstance().getErrorDialog(getActivity(), code, HANDLE_GMS).show();
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
    mQrCodeFragBinding.addAttendeeConfirm.setOnClickListener(
        clicked -> setupClickCloseListener(R.id.fragment_lao_detail));
  }

  private void setupClickCloseListener(int nextFragment) {
    if (closeRollCallAlert != null && closeRollCallAlert.isShowing()) {
      closeRollCallAlert.dismiss();
    }
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle("Close Roll Call");
    builder.setMessage("You have scanned " + nbAttendees + " attendees.");
    builder.setOnDismissListener(dialog -> startCamera());
    builder.setPositiveButton(
        R.string.confirm,
        (dialog, which) ->
            ((LaoDetailViewModel) mQRCodeScanningViewModel).closeRollCall(nextFragment));
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
    new Handler()
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
            this,
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
            this,
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
            this,
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
            this,
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
            this,
            integerEvent -> {
              Integer nextFragment = integerEvent.getContentIfNotHandled();
              if (nextFragment != null) {
                setupClickCloseListener(nextFragment);
              }
            });
  }
}
