package com.github.dedis.popstellar.ui.qrcode;

import androidx.lifecycle.LiveData;

public interface QRCodeScanningViewModel {

  void handleData(String data, ScanningAction scanningAction);

  LiveData<Integer> getNbScanned();
}
