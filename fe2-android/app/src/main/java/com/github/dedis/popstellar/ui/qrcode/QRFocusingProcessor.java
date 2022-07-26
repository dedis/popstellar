package com.github.dedis.popstellar.ui.qrcode;

import android.util.SparseArray;

import com.google.android.gms.vision.*;
import com.google.android.gms.vision.barcode.Barcode;

/**
 * A Barcode processor.
 *
 * <p>This class handles the detection of barcodes and chooses the most centered to be decoded.
 */
public class QRFocusingProcessor extends FocusingProcessor<Barcode> {

  public QRFocusingProcessor(Detector<Barcode> detector, Tracker<Barcode> tracker) {
    super(detector, tracker);
  }

  @Override
  public int selectFocus(Detector.Detections<Barcode> detections) {
    // Find most centered qrcode
    SparseArray<Barcode> barcodes = detections.getDetectedItems();
    double centerX = detections.getFrameMetadata().getWidth() / 2d;
    double centerY = detections.getFrameMetadata().getHeight() / 2d;
    double minSquaredDistance = Double.MAX_VALUE;
    int id = -1;

    for (int i = 0; i < barcodes.size(); i++) {
      int key = barcodes.keyAt(i);
      Barcode curBarcode = barcodes.get(key);

      double dx = centerX - curBarcode.getBoundingBox().centerX();
      double dy = centerY - curBarcode.getBoundingBox().centerY();
      double squaredDist = dx * dx + dy * dy;

      if (squaredDist < minSquaredDistance) {
        minSquaredDistance = squaredDist;
        id = key;
      }
    }

    return id;
  }
}
