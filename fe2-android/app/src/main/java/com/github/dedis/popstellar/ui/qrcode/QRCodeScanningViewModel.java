package com.github.dedis.popstellar.ui.qrcode;

import androidx.lifecycle.LiveData;

public interface QRCodeScanningViewModel {

  void handleData(String data);

  LiveData<Integer> getNbScanned();
}
