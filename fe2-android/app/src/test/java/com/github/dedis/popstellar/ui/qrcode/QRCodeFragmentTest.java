package com.github.dedis.popstellar.ui.qrcode;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.gms.vision.barcode.Barcode;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import dagger.hilt.android.testing.HiltAndroidTest;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class QRCodeFragmentTest {

    @Test
    public void checkQRListenerIsNotNullAfterGetInstanceCall(){
        QRCodeListener qrCodeListener = new QRCodeListener() {
            @Override
            public void onQRCodeDetected(Barcode barcode) {
            }
        };
        BarcodeTracker idleBarcodeTracker = BarcodeTracker.getInstance(qrCodeListener);
        assertNotNull(idleBarcodeTracker);
        BarcodeTracker idleBarcodeTracker2 = BarcodeTracker.getInstance(qrCodeListener);
        assertTrue(idleBarcodeTracker2 == idleBarcodeTracker);

    }


}
