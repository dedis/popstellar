package com.github.dedis.popstellar.ui.qrcode

import androidx.lifecycle.LiveData

interface QRCodeScanningViewModel {
  fun handleData(data: String?)

  val nbScanned: LiveData<Int?>
}
