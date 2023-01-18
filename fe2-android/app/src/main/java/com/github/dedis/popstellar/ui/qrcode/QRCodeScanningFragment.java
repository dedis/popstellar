package com.github.dedis.popstellar.ui.qrcode;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.github.dedis.popstellar.R;
import com.github.dedis.popstellar.databinding.QrcodeFragmentBinding;
import com.github.dedis.popstellar.ui.detail.*;
import com.github.dedis.popstellar.ui.home.HomeActivity;
import com.github.dedis.popstellar.ui.home.HomeViewModel;
import com.github.dedis.popstellar.utility.error.ErrorUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static androidx.core.content.ContextCompat.checkSelfPermission;
import static com.github.dedis.popstellar.ui.detail.LaoDetailActivity.setCurrentFragment;

/** Fragment handling the QR code scanning */
@AndroidEntryPoint
public final class QRCodeScanningFragment extends Fragment {

  public static final String TAG = QRCodeScanningFragment.class.getSimpleName();
  private static final int HANDLE_GMS = 9001;

  @Inject BarcodeDetector barcodeDetector;
  @Inject CameraProvider cameraProvider;

  private CameraSource camera;

  private QrcodeFragmentBinding binding;
  private QRCodeScanningViewModel viewModel;
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
    binding = QrcodeFragmentBinding.inflate(inflater, container, false);
    camera =
        cameraProvider.provide(
            getResources().getInteger(R.integer.camera_preview_width),
            getResources().getInteger(R.integer.camera_preview_height));

    FragmentActivity activity = getActivity();

    if (activity instanceof HomeActivity) {
      viewModel = HomeActivity.obtainViewModel(activity);
      HomeViewModel homeViewModel = (HomeViewModel) viewModel;
      homeViewModel.setPageTitle(R.string.join_lao_title);
    } else if (activity instanceof LaoDetailActivity) {
      viewModel = LaoDetailActivity.obtainViewModel(activity);

      // Subscribe to "scan warning " event when the same QR code is scanned twice for example
      // It is used in the LaoDetailActivity whatever the scanning action is
      observeWarningEvent();

    } else {
      throw new IllegalArgumentException("cannot obtain view model");
    }
    binding.setScanningAction(viewModel.getScanningAction());
    binding.manualAddConfirm.setOnClickListener(
        view -> {
          String data = binding.manualAddEditText.getText().toString();
          boolean success = viewModel.addManually(data);
          if (success) {
            binding.manualAddEditText.getText().clear();
          }
        });

    if (viewModel.getScanningAction() == ScanningAction.ADD_ROLL_CALL_ATTENDEE) {
      binding.addAttendeeTotalText.setVisibility(View.VISIBLE);
      binding.addAttendeeNumberText.setVisibility(View.VISIBLE);
      binding.addAttendeeConfirm.setVisibility(View.VISIBLE);

      LaoDetailViewModel laoDetailViewModel = (LaoDetailViewModel) viewModel;
      laoDetailViewModel.setPageTitle(R.string.add_attendee_title);

      // Subscribe to " Nb of attendees"  event
      observeNbAttendeesEvent();

      // Subscribe to " Attendees scan confirm " event
      observeAttendeeScanConfirmEvent();

      // set up the listener for the button that closes the roll call
      setupCloseRollCallButton();
    }

    if (viewModel.getScanningAction() == ScanningAction.ADD_WITNESS) {
      LaoDetailViewModel laoDetailViewModel = (LaoDetailViewModel) viewModel;
      laoDetailViewModel.setPageTitle(R.string.add_witness_description);

      // Subscribe to " Witness scan confirm " event
      observeWitnessScanConfirmEvent();
    }

    mPreview = binding.qrCameraPreview;
    binding.scanDescription.setText(viewModel.getScanDescription());
    binding.setLifecycleOwner(activity);
    // TODO: consider removing coupling with QRFocusingProcessor
    barcodeDetector.setProcessor(
        new QRFocusingProcessor(barcodeDetector, BarcodeTracker.getInstance(viewModel)));
    setupAllowCameraButton();
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    applyPermissionToView();
  }

  private void setupAllowCameraButton() {
    // Create request permission launcher which will ask for permission
    ActivityResultLauncher<String> requestPermissionLauncher =
        registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            requireActivity().getActivityResultRegistry(),
            isGranted -> applyPermissionToView() // This is the callback of the permission granter
            );

    // The button launch the build launcher when is it clicked
    binding.allowCameraButton.setOnClickListener(
        b -> requestPermissionLauncher.launch(Manifest.permission.CAMERA));
  }

  private void applyPermissionToView() {
    if (checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PERMISSION_GRANTED) {
      binding.cameraPermission.setVisibility(View.GONE);
      startCamera();
    } else {
      // the camera permission is not granted, open the dedicated fragment
      binding.cameraPermission.setVisibility(View.VISIBLE);
    }
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

  private LaoDetailViewModel getLaoViewModel() {
    return (LaoDetailViewModel) viewModel;
  }

  private void startCamera() throws SecurityException {
    // check that the device has play services available.
    int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
    if (code != ConnectionResult.SUCCESS) {
      Objects.requireNonNull(
              GoogleApiAvailability.getInstance()
                  .getErrorDialog(requireActivity(), code, HANDLE_GMS))
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
    binding.addAttendeeConfirm.setOnClickListener(clicked -> setupClickCloseListener());
  }

  private void setupClickCloseListener() {
    if (closeRollCallAlert != null && closeRollCallAlert.isShowing()) {
      closeRollCallAlert.dismiss();
    }
    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle("Close Roll Call");
    builder.setMessage("You have scanned " + nbAttendees + " attendees.");
    builder.setOnDismissListener(dialog -> applyPermissionToView());
    builder.setPositiveButton(
        R.string.confirm,
        (dialog, which) ->
            getLaoViewModel()
                .addDisposable(
                    getLaoViewModel()
                        .closeRollCall()
                        .subscribe(
                            () ->
                                setCurrentFragment(
                                    getParentFragmentManager(),
                                    R.id.fragment_lao_detail,
                                    LaoDetailFragment::newInstance),
                            error ->
                                ErrorUtils.logAndShow(
                                    requireContext(), TAG, error, R.string.error_close_rollcall))));
    builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());
    mPreview.stop();
    closeRollCallAlert = builder.create();
    closeRollCallAlert.show();
  }

  private void setupWarningPopup(String msg) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setTitle("Warning");
    builder.setMessage(msg);
    builder.setOnDismissListener(dialog -> applyPermissionToView());
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
    getLaoViewModel()
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
    getLaoViewModel()
        .getNbAttendees()
        .observe(
            getViewLifecycleOwner(),
            attendees -> {
              nbAttendees = attendees;
              binding.addAttendeeNumberText.setText(nbAttendees.toString());
            });
  }

  void observeWitnessScanConfirmEvent() {
    getLaoViewModel()
        .getWitnessScanConfirmEvent()
        .observe(
            getViewLifecycleOwner(),
            booleanEvent -> {
              Boolean event = booleanEvent.getContentIfNotHandled();
              if (event != null) {
                Toast.makeText(
                        requireContext(), R.string.add_witness_successful, Toast.LENGTH_SHORT)
                    .show();
              }
            });
  }

  void observeAttendeeScanConfirmEvent() {
    getLaoViewModel()
        .getAttendeeScanConfirmEvent()
        .observe(
            getViewLifecycleOwner(),
            stringEvent -> {
              String event = stringEvent.getContentIfNotHandled();
              if (event != null) {
                Toast.makeText(
                        requireContext(), R.string.add_attendee_successful, Toast.LENGTH_SHORT)
                    .show();
              }
            });
  }
}
