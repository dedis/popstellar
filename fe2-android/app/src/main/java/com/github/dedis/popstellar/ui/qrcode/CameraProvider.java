package com.github.dedis.popstellar.ui.qrcode;

import com.google.android.gms.vision.CameraSource;

import java.util.function.Supplier;

public class CameraProvider {

  private final Supplier<CameraSource.Builder> builder;

  public CameraProvider(Supplier<CameraSource.Builder> builder) {
    this.builder = builder;
  }

  public CameraSource provide(int width, int length) {
    return this.builder.get().setRequestedPreviewSize(width, length).build();
  }
}
