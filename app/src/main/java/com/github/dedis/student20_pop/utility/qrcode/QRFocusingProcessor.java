package com.github.dedis.student20_pop.utility.qrcode;

import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.FocusingProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

/**
 * A Barcode processor.
 *
 * This class handles the detection of barcodes and chooses the most centered to be decoded.
 */
public class QRFocusingProcessor extends FocusingProcessor<Barcode> {

    public QRFocusingProcessor(BarcodeDetector detector, QRCodeListener listener) {
        super(detector, new BarcodeTracker(listener));
    }

    @Override
    public int selectFocus(Detector.Detections<Barcode> detections) {
        //Find most centered qrcode
        SparseArray<Barcode> barcodes = detections.getDetectedItems();
        double centerX = detections.getFrameMetadata().getWidth()/2d;
        double centerY = detections.getFrameMetadata().getHeight()/2d;
        double minSquaredDistance = Double.MAX_VALUE;
        int id = -1;

        for(int i = 0; i < barcodes.size(); i++) {
            int key = barcodes.keyAt(i);
            Barcode curBarcode = barcodes.get(key);

            double dx = centerX - curBarcode.getBoundingBox().centerX();
            double dy = centerY - curBarcode.getBoundingBox().centerY();
            double squaredDist = dx * dx + dy * dy;

            if(squaredDist < minSquaredDistance) {
                minSquaredDistance = squaredDist;
                id = key;
            }
        }

        return id;
    }

    /**
     * Tracker for barcodes
     *
     * Handles new barcode detection and notify the listener
     */
    private static class BarcodeTracker extends Tracker<Barcode> {

        private final QRCodeListener listener;

        public BarcodeTracker(QRCodeListener listener) {
            this.listener = listener;
        }

        @Override
        public void onNewItem(int id, Barcode barcode) {
            if(barcode.valueFormat == Barcode.URL)
                listener.onQRCodeDetected(barcode.url.url);
        }
    }
}
