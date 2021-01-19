package com.github.dedis.student20_pop.utility.qrcode;

import com.github.dedis.student20_pop.ui.qrcode.QRCodeScanningFragment;

/**
 * Use this listener to tell activity to switch to CameraPermissionFragment when current fragment
 * needs camera permission
 */
public interface OnCameraNotAllowedListener {
  void onCameraNotAllowedListener(
      QRCodeScanningFragment.QRCodeScanningType qrCodeScanningType, String eventId);
}
