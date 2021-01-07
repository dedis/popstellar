package com.github.dedis.student20_pop.utility.qrcode;

import com.github.dedis.student20_pop.ui.QRCodeScanningFragment.QRCodeScanningType;

/**
 * A listener that will receive detected url of a QR code
 */
public interface QRCodeListener {

    void onQRCodeDetected(String data, QRCodeScanningType qrCodeScanningType, String eventId);
}
