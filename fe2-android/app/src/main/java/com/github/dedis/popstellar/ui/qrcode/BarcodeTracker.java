package com.github.dedis.popstellar.ui.qrcode;

import androidx.annotation.NonNull;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

public class BarcodeTracker extends Tracker<Barcode> {

  private static BarcodeTracker INSTANCE = null;

  private QRCodeListener listener;

  private BarcodeTracker() {}

  public static synchronized BarcodeTracker getInstance(QRCodeListener listener) {
    if (INSTANCE == null) {
      INSTANCE = new BarcodeTracker();
    }
    INSTANCE.setListener(listener);
    return INSTANCE;
  }

  public void setListener(QRCodeListener listener) {
    this.listener = listener;
  }

  @Override
  public void onNewItem(int id, @NonNull Barcode barcode) {
    listener.onQRCodeDetected(barcode);
  }
}
