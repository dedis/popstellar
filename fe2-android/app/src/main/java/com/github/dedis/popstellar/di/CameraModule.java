package com.github.dedis.popstellar.di;

import android.content.Context;
import android.hardware.Camera;

import com.github.dedis.popstellar.ui.qrcode.CameraProvider;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.android.scopes.ActivityScoped;

@Module
@InstallIn(ActivityComponent.class)
public class CameraModule {

  private CameraModule() {}

  @Provides
  @ActivityScoped
  public static BarcodeDetector provideQRCodeDetector(@ApplicationContext Context context) {
    return new BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.QR_CODE).build();
  }

  @Provides
  @ActivityScoped
  public static CameraProvider provideCamera(
      @ApplicationContext Context context, BarcodeDetector qrDetector) {
    return new CameraProvider(
        () ->
            new CameraSource.Builder(context, qrDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(15.0f)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE));
  }
}
