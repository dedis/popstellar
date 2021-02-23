package com.github.dedis.student20_pop.utility.qrcode;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

public class BarcodeTracker extends Tracker<Barcode> {

  private static BarcodeTracker INSTANCE = null;

  private QRCodeListener listener;

  private BarcodeTracker() {}

  public static BarcodeTracker getInstance(QRCodeListener listener) {
    if (INSTANCE == null) {
      synchronized (BarcodeTracker.class) {
        if (INSTANCE == null) {
          INSTANCE = new BarcodeTracker();
        }
      }
    }
    INSTANCE.setListener(listener);
    return INSTANCE;
  }

  public void setListener(QRCodeListener listener) {
    this.listener = listener;
  }

  @Override
  public void onNewItem(int id, Barcode barcode) {
    listener.onQRCodeDetected(barcode);
  }
}
