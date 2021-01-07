package com.github.dedis.student20_pop.utility.qrcode;

import com.github.dedis.student20_pop.ui.QRCodeScanningFragment;

/**
 * Use this listener to switch to fragment that needed
 * camera permission before
 */
public interface OnCameraAllowedListener {
    void onCameraAllowedListener(QRCodeScanningFragment.QRCodeScanningType qrCodeScanningType, String eventId);
}
