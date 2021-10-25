package com.github.dedis.popstellar.ui.qrcode;

import com.google.android.gms.vision.barcode.Barcode;

/** A listener that will receive detected url of a QR code */
public interface QRCodeListener {

  void onQRCodeDetected(Barcode barcode);
}
