package com.github.dedis.popstellar.ui.lao.popcha;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.github.dedis.popstellar.ui.qrcode.QRCodeScanningViewModel;
import dagger.hilt.android.lifecycle.HiltViewModel;
import javax.inject.Inject;

@HiltViewModel
public class PoPCHAViewModel extends AndroidViewModel implements QRCodeScanningViewModel {

  @Inject
  public PoPCHAViewModel(@NonNull Application application) {
    super(application);
  }

  private String laoId;

  public void setLaoId(String laoId) {
    this.laoId = laoId;
  }

  public String getLaoId() {
    return laoId;
  }

  @Override
  public void handleData(String data) {}

  @Override
  public LiveData<Integer> getNbScanned() {
    return null;
  }
}
