package com.github.dedis.popstellar.di;

import android.content.Context;
import android.hardware.Camera;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;

@Module
@InstallIn(ActivityComponent.class)
public class CameraModule {

  private CameraModule() {}

  @Provides
  @Singleton
  public static BarcodeDetector provideQRCodeDetector(Context context) {
    return new BarcodeDetector.Builder(context).setBarcodeFormats(Barcode.QR_CODE).build();
  }

  @Provides
  @Singleton
  public static CameraSource.Builder provideCameraSourceBuilder(
      Context context, BarcodeDetector qrDetector) {
    return new CameraSource.Builder(context, qrDetector)
        .setFacing(CameraSource.CAMERA_FACING_BACK)
        .setRequestedFps(15.0f)
        .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
  }
}
