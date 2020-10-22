package com.github.dedis.student20_pop.utility.qrcode;

/**
 * A listener that will receive detected url of a QR code
 */
public interface QRCodeListener {

    void onQRCodeDetected(String url);
}
