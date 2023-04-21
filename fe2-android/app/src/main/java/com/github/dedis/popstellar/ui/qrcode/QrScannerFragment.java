package com.github.dedis.popstellar.ui.qrcode;

import android.Manifest;
import android.os.Bundle;
import android.view.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.mlkit.vision.MlKitAnalyzer;
import androidx.camera.view.LifecycleCameraController;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.dedis.popstellar.databinding.QrScannerFragmentBinding;
import com.github.dedis.popstellar.ui.PopViewModel;
import com.google.mlkit.vision.barcode.*;
import com.google.mlkit.vision.barcode.common.Barcode;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static androidx.camera.view.CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED;
import static androidx.core.content.ContextCompat.checkSelfPermission;

public class QrScannerFragment extends Fragment {
  private static final Logger logger = LogManager.getLogger(QrScannerFragment.class);

  public static final String SCANNING_KEY = "scanning_action_key";
  public static final String BACK_ARGS = "back_arguments";

  private QrScannerFragmentBinding binding;
  private BarcodeScanner barcodeScanner;

  private QRCodeScanningViewModel scanningViewModel;
  private PopViewModel popViewModel;

  public QrScannerFragment() {
    // Required empty public constructor
  }

  public static QrScannerFragment newInstance(ScanningAction scanningAction, String... backArgs) {
    QrScannerFragment fragment = new QrScannerFragment();
    Bundle bundle = new Bundle(1);
    bundle.putSerializable(SCANNING_KEY, scanningAction);
    bundle.putSerializable(BACK_ARGS, backArgs);
    fragment.setArguments(bundle);
    return fragment;
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    binding = QrScannerFragmentBinding.inflate(inflater, container, false);
    ScanningAction scanningAction = getScanningAction();
    popViewModel = scanningAction.obtainPopViewModel(requireActivity());
    scanningViewModel =
        scanningAction.obtainScannerViewModel(requireActivity(), popViewModel.getLaoId());

    if (scanningAction != ScanningAction.ADD_LAO_PARTICIPANT) {
      displayCounter();
    }

    binding.scannedTitle.setText(scanningAction.scanTitle);
    binding.addManualTitle.setText(scanningAction.manualAddTitle);
    binding.manualAddEditText.setHint(scanningAction.hint);
    binding.scannerInstructionText.setText(scanningAction.instruction);

    setupNbScanned();
    setupManualAdd();
    setupAllowCameraButton();
    return binding.getRoot();
  }

  @Override
  public void onResume() {
    super.onResume();
    ScanningAction scanningAction = getScanningAction();
    popViewModel.setPageTitle(scanningAction.pageTitle);
    applyPermissionToView();

    // Handle back press navigation
    requireActivity()
        .getOnBackPressedDispatcher()
        .addCallback(
            this, scanningAction.onBackPressedCallback(getParentFragmentManager(), getBackArgs()));
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (barcodeScanner != null) {
      barcodeScanner.close();
    }
  }

  private ScanningAction getScanningAction() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      return requireArguments().getSerializable(SCANNING_KEY, ScanningAction.class);
    } else {
      // This is deprecated as of Android 13 but it'll be probably 2030 before it's our min SDK
      // noinspection deprecation
      return (ScanningAction) requireArguments().getSerializable(SCANNING_KEY);
    }
  }

  private String[] getBackArgs() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      return requireArguments().getSerializable(BACK_ARGS, String[].class);
    } else {
      // This is deprecated as of Android 13 but it'll be probably 2030 before it's our min SDK
      // noinspection deprecation
      return (String[]) requireArguments().getSerializable(BACK_ARGS);
    }
  }

  private void setupNbScanned() {
    scanningViewModel
        .getNbScanned()
        .observe(getViewLifecycleOwner(), nb -> binding.scannedNumber.setText(String.valueOf(nb)));
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
      binding.scannerInstructionText.setVisibility(View.VISIBLE);
      binding.qrCodeSight.setVisibility(View.VISIBLE);
      startCamera();
    } else {
      // the camera permission is not granted, make dedicated views visible
      binding.cameraPermission.setVisibility(View.VISIBLE);
      binding.scannerInstructionText.setVisibility(View.GONE);
      binding.qrCodeSight.setVisibility(View.GONE);
    }
  }

  private void startCamera() {
    BarcodeScannerOptions options =
        new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build();
    barcodeScanner = BarcodeScanning.getClient(options);
    LifecycleCameraController cameraController = new LifecycleCameraController(requireContext());
    cameraController.bindToLifecycle(this);
    Executor executor = ContextCompat.getMainExecutor(requireContext());
    cameraController.setImageAnalysisAnalyzer(
        executor,
        new MlKitAnalyzer(
            Collections.singletonList(barcodeScanner),
            COORDINATE_SYSTEM_VIEW_REFERENCED,
            executor,
            result -> {
              List<Barcode> barcodes = result.getValue(barcodeScanner);
              if (barcodes != null && !barcodes.isEmpty()) {
                logger.debug("barcode raw value :" + barcodes.get(0).getRawValue());
                onResult(barcodes.get(0));
              }
            }));
    binding.scannerCamera.setController(cameraController);
  }

  private void setupManualAdd() {
    binding.scannerEnterManually.setOnClickListener(
        v -> {
          binding.scannerBottomTexts.setVisibility(View.GONE);
          binding.enterManuallyCard.setVisibility(View.VISIBLE);
        });

    binding.addManualClose.setOnClickListener(
        v -> {
          binding.scannerBottomTexts.setVisibility(View.VISIBLE);
          binding.enterManuallyCard.setVisibility(View.GONE);
        });

    binding.manualAddButton.setOnClickListener(
        v -> {
          String input = binding.manualAddEditText.getText().toString();
          onResult(input);
        });
  }

  private void displayCounter() {
    binding.scannedTitle.setVisibility(View.VISIBLE);
    binding.scannedNumber.setVisibility(View.VISIBLE);
  }

  private void onResult(Barcode barcode) {
    onResult(barcode.getRawValue());
  }

  private void onResult(String data) {
    scanningViewModel.handleData(data);
  }
}
