package com.github.dedis.popstellar.qrcode;

import com.github.dedis.popstellar.utility.qrcode.QRCodeListener;

public interface QRCodeScanningViewModel extends QRCodeListener {

  int getScanDescription();

  ScanningAction getScanningAction();
}
