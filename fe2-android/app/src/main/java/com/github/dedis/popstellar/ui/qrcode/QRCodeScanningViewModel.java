package com.github.dedis.popstellar.ui.qrcode;

public interface QRCodeScanningViewModel extends QRCodeListener {

  int getScanDescription();

  ScanningAction getScanningAction();

  boolean addManually(String data);
}
